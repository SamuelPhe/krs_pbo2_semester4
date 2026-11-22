<?php
declare(strict_types=1);
require __DIR__ . '/config.php';

$account=$_SESSION['krs_user']??null;
if(!is_array($account)){header('Location: index.php?notice=login-required',true,302);exit;}
if(($account['role']??'')!=='Kaprodi'){http_response_code(403);exit('Akses ditolak. Finalisasi hanya untuk Kaprodi.');}
$pdo=krs_db();
$kaprodiId=(int)($account['id']??0);
$programQuery=$pdo->prepare('SELECT id_prodi,nama_prodi FROM prodi WHERE id_kaprodi=? ORDER BY nama_prodi');
$programQuery->execute([$kaprodiId]);$assignedPrograms=$programQuery->fetchAll();
if(!$assignedPrograms){http_response_code(403);exit('Akun Kaprodi belum ditugaskan pada program studi. Hubungi Admin.');}
$programIds=array_map(static fn(array $p):int=>(int)$p['id_prodi'],$assignedPrograms);
$selectedProgram=filter_input(INPUT_GET,'program',FILTER_VALIDATE_INT)?:0;
if($selectedProgram>0&&!in_array($selectedProgram,$programIds,true)){$selectedProgram=0;}
if($selectedProgram===0&&count($assignedPrograms)===1)$selectedProgram=(int)$assignedPrograms[0]['id_prodi'];
$detailId=filter_input(INPUT_GET,'id',FILTER_VALIDATE_INT)?:0;
$today=(string)$pdo->query('SELECT CURRENT_DATE')->fetchColumn();
$error='';$success=(string)($_SESSION['krs_finalisasi_flash']??'');unset($_SESSION['krs_finalisasi_flash']);

function krs_finalisasi_get(PDO $pdo,int $applicationId,int $kaprodiId):?array{
 $q=$pdo->prepare("SELECT pk.id_pengajuan,pk.id_krs,pk.id_mahasiswa,pk.status_acc,pk.tanggal_pengajuan,
   m.nama_mahasiswa,m.id_prodi,p.nama_prodi,k.semester,k.tahun_ajaran,k.status_krs,k.tanggal_mulai,k.tanggal_selesai,
   d.nama_dosen AS nama_dosen_pa
   FROM pengajuan_krs pk JOIN mahasiswa m ON m.id_mahasiswa=pk.id_mahasiswa
   JOIN krs k ON k.id_krs=pk.id_krs LEFT JOIN prodi p ON p.id_prodi=m.id_prodi
   LEFT JOIN dosen d ON d.id_dosen=m.id_dosen_pa
   JOIN prodi assigned ON assigned.id_prodi=m.id_prodi
   WHERE pk.id_pengajuan=? AND assigned.id_kaprodi=? AND pk.status_acc='AccDosenPA' LIMIT 1");
 $q->execute([$applicationId,$kaprodiId]);$row=$q->fetch();return $row?:null;
}
function krs_finalisasi_open(array $row,string $today):bool{
 return $row['status_acc']==='AccDosenPA'&&$row['status_krs']==='terbuka'&&$today>=(string)$row['tanggal_mulai']&&$today<=(string)$row['tanggal_selesai'];
}

if($_SERVER['REQUEST_METHOD']==='POST'){
 $csrf=(string)($_SESSION['csrf_token']??'');$sent=(string)($_POST['csrf_token']??'');
 if($csrf===''||!hash_equals($csrf,$sent)){http_response_code(403);$error='Sesi formulir tidak valid.';}
 else{
  $action=(string)($_POST['action']??'');$appId=filter_var($_POST['id_pengajuan']??'',FILTER_VALIDATE_INT);
  $appId=($appId===false||$appId===null||$appId<1)?0:(int)$appId;$detailId=$appId;
  if($appId<1)$error='ID pengajuan tidak valid.';
  else{
   try{
    $pdo->beginTransaction();
    $lock=$pdo->prepare("SELECT pk.id_pengajuan,pk.status_acc,pk.id_krs,m.id_prodi,k.status_krs,k.tanggal_mulai,k.tanggal_selesai
      FROM pengajuan_krs pk JOIN mahasiswa m ON m.id_mahasiswa=pk.id_mahasiswa
      JOIN krs k ON k.id_krs=pk.id_krs JOIN prodi p ON p.id_prodi=m.id_prodi
      WHERE pk.id_pengajuan=? AND p.id_kaprodi=? FOR UPDATE");
    $lock->execute([$appId,$kaprodiId]);$locked=$lock->fetch();
    $freshToday=(string)$pdo->query('SELECT CURRENT_DATE')->fetchColumn();
    $active=$locked&&$locked['status_acc']==='AccDosenPA'&&$locked['status_krs']==='terbuka'
      &&$freshToday>=(string)$locked['tanggal_mulai']&&$freshToday<=(string)$locked['tanggal_selesai'];
    if(!$locked){$pdo->rollBack();http_response_code(404);$error='Pengajuan tidak ditemukan pada program studi yang Anda pimpin.';}
    elseif(!$active){$pdo->rollBack();$error='Pengajuan tidak lagi berada dalam periode finalisasi aktif.';}
    elseif(in_array($action,['approve_selected','reject_selected'],true)){
      $raw=$_POST['detail_ids']??[];if(!is_array($raw))$raw=[];
      $ids=[];foreach($raw as $value){$valid=filter_var($value,FILTER_VALIDATE_INT);if($valid!==false&&$valid!==null&&$valid>0)$ids[]=(int)$valid;}
      $ids=array_values(array_unique($ids));$note=trim((string)($_POST['catatan_revisi']??''));
      if(!$ids||count($ids)>40){$pdo->rollBack();$error='Pilih satu sampai 40 mata kuliah yang masih menunggu finalisasi.';}
      elseif($action==='reject_selected'&&($note===''||strlen($note)>1000)){$pdo->rollBack();$error='Catatan revisi wajib diisi maksimal 1.000 byte.';}
      else{
        $approve=$pdo->prepare("UPDATE krs_detail kd JOIN pengajuan_krs pk ON pk.id_pengajuan=kd.id_pengajuan
          JOIN mahasiswa m ON m.id_mahasiswa=pk.id_mahasiswa JOIN prodi p ON p.id_prodi=m.id_prodi
          SET kd.status_acc='AccDosenPA',kd.catatan_revisi=''
          WHERE kd.id_detail=? AND kd.id_pengajuan=? AND p.id_kaprodi=? AND pk.status_acc='AccDosenPA' AND kd.status_acc='Disetujui'");
        $reject=$pdo->prepare("UPDATE krs_detail kd JOIN pengajuan_krs pk ON pk.id_pengajuan=kd.id_pengajuan
          JOIN mahasiswa m ON m.id_mahasiswa=pk.id_mahasiswa JOIN prodi p ON p.id_prodi=m.id_prodi
          SET kd.status_acc='Ditolak',kd.catatan_revisi=?
          WHERE kd.id_detail=? AND kd.id_pengajuan=? AND p.id_kaprodi=? AND pk.status_acc='AccDosenPA' AND kd.status_acc='Disetujui'");
        $updated=0;
        foreach($ids as $courseId){
          if($action==='approve_selected'){$approve->execute([$courseId,$appId,$kaprodiId]);$updated+=$approve->rowCount();}
          else{$reject->execute([$note,$courseId,$appId,$kaprodiId]);$updated+=$reject->rowCount();}
        }
        if($updated<1){$pdo->rollBack();$error='Tidak ada mata kuliah yang dapat diperbarui. Periksa status mata kuliah.';}
        else{$pdo->commit();$_SESSION['krs_finalisasi_flash']=$updated.' mata kuliah berhasil '.($action==='approve_selected'?'disetujui pada tahap finalisasi.':'dikembalikan untuk revisi.');header('Location: finalisasi.php?id='.$appId,true,303);exit;}
      }
    }elseif($action==='finish'){
      $q=$pdo->prepare("SELECT kd.status_acc,kd.catatan_revisi,m.nama_matkul FROM krs_detail kd
        JOIN jadwal j ON j.id_jadwal=kd.id_jadwal JOIN matkul m ON m.id_matkul=j.id_matkul
        WHERE kd.id_pengajuan=? FOR UPDATE");$q->execute([$appId]);$details=$q->fetchAll();
      $pending=array_filter($details,static fn(array $d):bool=>!in_array($d['status_acc'],['AccDosenPA','Ditolak'],true));
      if(!$details||$pending){$pdo->rollBack();$error=!$details?'Pengajuan belum memiliki mata kuliah.':'Semua mata kuliah harus diberi keputusan sebelum finalisasi selesai.';}
      else{
        $rejected=array_filter($details,static fn(array $d):bool=>$d['status_acc']==='Ditolak');
        $next=$rejected?'Ditolak':'Disetujui';
        $summary=[];foreach($rejected as $d)$summary[]=(string)$d['nama_matkul'].': '.(string)$d['catatan_revisi'];
        $update=$pdo->prepare("UPDATE pengajuan_krs SET status_acc=?,id_dosen_acc=?,catatan_revisi=? WHERE id_pengajuan=? AND status_acc='AccDosenPA'");
        $update->execute([$next,$kaprodiId,$rejected?implode("\n",$summary):null,$appId]);
        if($update->rowCount()!==1){$pdo->rollBack();$error='Status pengajuan berubah. Muat ulang daftar finalisasi.';}
        else{$pdo->commit();$_SESSION['krs_finalisasi_flash']=$next==='Disetujui'?'KRS telah disetujui pada tahap akhir.':'KRS dikembalikan kepada Mahasiswa karena ada mata kuliah yang ditolak pada finalisasi.';header('Location: finalisasi.php',true,303);exit;}
      }
    }else{$pdo->rollBack();$error='Aksi finalisasi tidak dikenal.';}
   }catch(Throwable $exception){if($pdo->inTransaction())$pdo->rollBack();error_log('KRS finalization error: '.$exception->getMessage());$error='Finalisasi gagal disimpan. Periksa kembali status pengajuan.';}
  }
 }
}

$application=null;$details=[];$pendingCount=0;$rejectedCount=0;$canAct=false;
if($detailId>0){
 $application=krs_finalisasi_get($pdo,$detailId,$kaprodiId);
 if(!$application){http_response_code(404);$error=$error!==''?$error:'Pengajuan tidak ditemukan pada program studi yang Anda pimpin.';}
 else{
  $canAct=krs_finalisasi_open($application,$today);
  $q=$pdo->prepare("SELECT kd.id_detail,kd.status_acc,kd.catatan_revisi,m.nama_matkul,m.sks,
    j.hari,j.jam_mulai,j.jam_selesai,kl.nama_kelas,r.nama_ruang
    FROM krs_detail kd JOIN jadwal j ON j.id_jadwal=kd.id_jadwal
    JOIN matkul m ON m.id_matkul=j.id_matkul LEFT JOIN kelas kl ON kl.id_kelas=j.kelas
    LEFT JOIN ruang r ON r.id_ruang=j.ruang WHERE kd.id_pengajuan=?
    ORDER BY FIELD(LOWER(j.hari),'senin','selasa','rabu','kamis','jumat','sabtu','minggu'),j.jam_mulai");
  $q->execute([$detailId]);$details=$q->fetchAll();
  foreach($details as $d){if(!in_array($d['status_acc'],['AccDosenPA','Ditolak'],true))$pendingCount++;if($d['status_acc']==='Ditolak')$rejectedCount++;}
 }
}

$where="pk.status_acc='AccDosenPA' AND p.id_kaprodi=? AND k.status_krs='terbuka' AND CURRENT_DATE BETWEEN k.tanggal_mulai AND k.tanggal_selesai";
$params=[$kaprodiId];
if($selectedProgram>0){$where.=' AND m.id_prodi=?';$params[]=$selectedProgram;}
$q=$pdo->prepare("SELECT pk.id_pengajuan,pk.tanggal_pengajuan,m.nama_mahasiswa,p.nama_prodi,k.semester,k.tahun_ajaran,
 (SELECT COUNT(*) FROM krs_detail kd WHERE kd.id_pengajuan=pk.id_pengajuan) AS jumlah_matkul,
 (SELECT COALESCE(SUM(mc.sks),0) FROM krs_detail kd JOIN jadwal j ON j.id_jadwal=kd.id_jadwal JOIN matkul mc ON mc.id_matkul=j.id_matkul WHERE kd.id_pengajuan=pk.id_pengajuan) AS total_sks
 FROM pengajuan_krs pk JOIN mahasiswa m ON m.id_mahasiswa=pk.id_mahasiswa
 JOIN prodi p ON p.id_prodi=m.id_prodi JOIN krs k ON k.id_krs=pk.id_krs
 WHERE {$where} ORDER BY pk.tanggal_pengajuan,pk.id_pengajuan");
$q->execute($params);$queue=$q->fetchAll();
function final_status_class(string $status):string{return match($status){'Disetujui'=>'approved','Ditolak'=>'rejected','AccDosenPA'=>'review',default=>'draft'};}
?>
<!doctype html><html lang="id"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Finalisasi KRS — KRS</title>
<style>
*{box-sizing:border-box}body{margin:0;padding:18px;background:#d9ddd8;font-family:"Segoe UI",Arial,sans-serif;color:#303735}
.canvas{position:relative;width:min(1024px,100%);margin:auto;min-height:660px;background:#d9d5bb;box-shadow:0 12px 38px #24332f42}.bg{position:absolute;inset:0;width:100%;height:100%;object-fit:fill}.content{position:relative;padding:42px 7% 32px}
.heading{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:18px}.heading h1{font-size:28px;color:#2d4840;margin:0}.heading p{font-size:13px;color:#68756f;margin:4px 0}
.back,.button{display:inline-block;padding:9px 14px;background:#f8f8f3;border:1px solid #69817a;color:#31514a;text-decoration:none;font-weight:700;font-size:13px}
.panel{background:#ffffffdf;border:1px solid #a4b8b0;padding:14px 16px;margin-bottom:16px}.panel h2{margin:0 0 10px;color:#20675f;font-size:17px}
table{width:100%;border-collapse:collapse;font-size:13px;background:white}th{background:#0b7068;color:white;text-align:left;padding:10px;white-space:nowrap}td{padding:9px;border-bottom:1px solid #dce5e1;vertical-align:middle}tbody tr:nth-child(even){background:#f1f5f3}.table-wrap{overflow:auto}
.status{display:inline-block;padding:4px 9px;border-radius:14px;font-size:11px;font-weight:700}.approved{background:#d8f0e4;color:#216b48}.rejected{background:#fde5e2;color:#a03229}.review{background:#fff0c8;color:#86580b}.draft{background:#e9eef0;color:#435a61}
.toolbar{display:flex;align-items:center;gap:9px;flex-wrap:wrap;margin-bottom:12px}select,textarea{border:1px solid #aebfba;background:white;color:#283a36;padding:9px;font:13px "Segoe UI",Arial,sans-serif}textarea{width:100%;min-height:72px;resize:vertical}
button{border:1px solid #0b7068;background:#0b7068;color:white;padding:9px 13px;font:700 13px "Segoe UI",Arial,sans-serif;cursor:pointer}.reject{background:#fff0ed;color:#963a31;border-color:#c68c84}
.alert{padding:10px 12px;background:#eef8f1;border:1px solid #b8d6c3;color:#265c40;margin-bottom:13px;font-size:13px}.alert.error{background:#fff0ed;border-color:#dfb8ae;color:#8c3428}
.small{font-size:12px;color:#6b7974}.meta{display:grid;grid-template-columns:repeat(4,1fr);gap:9px;margin-bottom:15px}.meta div{background:#ffffffdf;padding:9px;border:1px solid #b1c3bc}.meta small{display:block;color:#71807a;font-size:11px}.meta strong{display:block;margin-top:4px;color:#2f4c44;font-size:14px}
@media(max-width:720px){body{padding:0}.content{padding:22px 14px}.heading{align-items:flex-start}.meta{grid-template-columns:1fr 1fr}.table-wrap table{min-width:750px}}
</style></head><body>
<main class="canvas"><img class="bg" src="https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Rectangle%20(right).png" alt="">
<div class="content"><header class="heading"><div><h1><?=($detailId>0?'Detail Finalisasi KRS':'Daftar KRS yang Perlu Difinalisasi')?></h1><p><?=krs_e((string)$account['name'])?> · Kaprodi</p></div><a class="back" href="<?=($detailId>0?'finalisasi.php':'index.php')?>">Back</a></header>
<?php if($success!==''): ?><div class="alert" role="status"><?=krs_e($success)?></div><?php endif; ?><?php if($error!==''): ?><div class="alert error" role="alert"><?=krs_e($error)?></div><?php endif; ?>
<?php if($detailId>0&&$application): ?>
<div class="meta"><div><small>Pengajuan</small><strong>#<?=krs_e((string)$application['id_pengajuan'])?></strong></div><div><small>Mahasiswa</small><strong><?=krs_e((string)$application['nama_mahasiswa'])?></strong></div><div><small>Program Studi</small><strong><?=krs_e((string)$application['nama_prodi'])?></strong></div><div><small>Sesi</small><strong>Semester <?=krs_e((string)$application['semester'])?> · <?=krs_e((string)$application['tahun_ajaran'])?></strong></div></div>
<section class="panel"><h2>Detail Mata Kuliah</h2><p class="small">Pengajuan ini telah melewati pemeriksaan Dosen PA. Kaprodi dapat menyetujui atau mengembalikan mata kuliah untuk revisi.</p>
<form method="post" action="finalisasi.php?id=<?=$detailId?>"><input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token']??''))?>"><input type="hidden" name="id_pengajuan" value="<?=$detailId?>">
<div class="table-wrap"><table><thead><tr><th>Pilih</th><th>ID Detail</th><th>Mata Kuliah</th><th>SKS</th><th>Kelas</th><th>Hari</th><th>Jam</th><th>Ruang</th><th>Status</th><th>Catatan</th></tr></thead><tbody>
<?php if(!$details): ?><tr><td colspan="10">Belum ada mata kuliah.</td></tr><?php else: foreach($details as $d): ?><tr>
<td><input type="checkbox" name="detail_ids[]" value="<?=krs_e((string)$d['id_detail'])?>" <?=($canAct&&$d['status_acc']==='Disetujui'?'':'disabled')?>></td>
<td><?=krs_e((string)$d['id_detail'])?></td><td><?=krs_e((string)$d['nama_matkul'])?></td><td><?=krs_e((string)$d['sks'])?></td>
<td><?=krs_e((string)($d['nama_kelas']??'-'))?></td><td><?=krs_e(ucfirst((string)$d['hari']))?></td><td><?=krs_e((string)$d['jam_mulai'])?>–<?=krs_e((string)$d['jam_selesai'])?></td>
<td><?=krs_e((string)($d['nama_ruang']??'-'))?></td><td><span class="status <?=final_status_class((string)$d['status_acc'])?>"><?=krs_e((string)$d['status_acc'])?></span></td><td><?=krs_e((string)($d['catatan_revisi']??'-'))?></td>
</tr><?php endforeach; endif; ?></tbody></table></div>
<?php if($canAct): ?><div class="toolbar" style="margin-top:13px"><button type="submit" name="action" value="approve_selected">Setujui Dipilih</button><button class="reject" type="submit" name="action" value="reject_selected">Revisi Dipilih</button></div><label for="catatan_revisi" class="small">Catatan revisi (wajib untuk tombol Revisi)</label><textarea id="catatan_revisi" name="catatan_revisi" maxlength="1000"></textarea><?php endif; ?></form></section>
<section class="panel"><h2>Penyelesaian Finalisasi</h2><p class="small"><?=$pendingCount?> mata kuliah belum memiliki keputusan akhir; <?=$rejectedCount?> ditolak pada tahap ini.</p>
<?php if($canAct&&$pendingCount===0&&count($details)>0): ?><form method="post" action="finalisasi.php?id=<?=$detailId?>" onsubmit="return confirm('Selesaikan finalisasi KRS ini?')"><input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token']??''))?>"><input type="hidden" name="id_pengajuan" value="<?=$detailId?>"><button type="submit" name="action" value="finish">Selesaikan Finalisasi</button></form><?php elseif($canAct): ?><p class="small">Berikan keputusan pada setiap mata kuliah sebelum menyelesaikan finalisasi.</p><?php else: ?><p class="small">Pengajuan ini hanya dapat dibaca karena masa finalisasi tidak aktif.</p><?php endif; ?></section>
<?php else: ?>
<section class="panel"><h2>Antrean Finalisasi</h2>
<?php if(count($assignedPrograms)>1): ?><form class="toolbar" method="get" action="finalisasi.php"><label for="program">Program Studi:</label><select id="program" name="program"><option value="0">Semua program studi</option><?php foreach($assignedPrograms as $p): ?><option value="<?=krs_e((string)$p['id_prodi'])?>" <?=($selectedProgram===(int)$p['id_prodi']?'selected':'')?>><?=krs_e((string)$p['nama_prodi'])?></option><?php endforeach; ?></select><button type="submit">Filter</button></form><?php endif; ?>
<div class="table-wrap"><table><thead><tr><th>ID Pengajuan</th><th>Nama Mahasiswa</th><th>Program Studi</th><th>Semester</th><th>Tahun Ajaran</th><th>Tanggal</th><th>Mata Kuliah</th><th>Total SKS</th><th>Aksi</th></tr></thead><tbody>
<?php if(!$queue): ?><tr><td colspan="9">Belum ada pengajuan yang menunggu finalisasi.</td></tr><?php else: foreach($queue as $item): ?><tr>
<td>#<?=krs_e((string)$item['id_pengajuan'])?></td><td><?=krs_e((string)$item['nama_mahasiswa'])?></td><td><?=krs_e((string)$item['nama_prodi'])?></td><td><?=krs_e((string)$item['semester'])?></td><td><?=krs_e((string)$item['tahun_ajaran'])?></td><td><?=krs_e((string)$item['tanggal_pengajuan'])?></td><td><?=krs_e((string)$item['jumlah_matkul'])?></td><td><?=krs_e((string)$item['total_sks'])?></td><td><a class="button" href="finalisasi.php?id=<?=krs_e((string)$item['id_pengajuan'])?>">Lihat Detail</a></td>
</tr><?php endforeach; endif; ?></tbody></table></div></section>
<?php endif; ?>
</div></main></body></html>
