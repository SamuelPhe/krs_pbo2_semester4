<?php
declare(strict_types=1);
require __DIR__ . '/config.php';
$account=$_SESSION['krs_user']??null;
if(!is_array($account)){header('Location: index.php?notice=login-required',true,302);exit;}
$role=(string)($account['role']??'');$expected=['Dosen PA'=>'dosen pa','Kaprodi'=>'kaprodi'];
if(!isset($expected[$role])){http_response_code(403);exit('Akses ditolak.');}
$pdo=krs_db();$dosenId=(int)($account['id']??0);
$check=$pdo->prepare('SELECT id_dosen,nama_dosen,posisi FROM dosen WHERE id_dosen=?');$check->execute([$dosenId]);$advisor=$check->fetch();
if(!$advisor||strtolower((string)$advisor['posisi'])!==$expected[$role]){http_response_code(403);exit('Akun pembimbing tidak valid.');}
$studentId=filter_input(INPUT_GET,'student',FILTER_VALIDATE_INT)?:0;
$detailId=filter_input(INPUT_GET,'detail',FILTER_VALIDATE_INT)?:0;
$error='';$student=null;$history=[];$detail=null;$detailRows=[];
if($studentId>0){
 $q=$pdo->prepare('SELECT m.id_mahasiswa,m.nama_mahasiswa,m.angkatan,m.semester,p.nama_prodi FROM mahasiswa m LEFT JOIN prodi p ON p.id_prodi=m.id_prodi WHERE m.id_mahasiswa=? AND m.id_dosen_pa=? LIMIT 1');
 $q->execute([$studentId,$dosenId]);$student=$q->fetch();
 if(!$student){http_response_code(404);$error='Mahasiswa tidak ditemukan dalam daftar bimbingan Anda.';}
 else{
  $q=$pdo->prepare('SELECT pk.id_pengajuan,pk.id_krs,pk.tanggal_pengajuan,pk.status_acc,pk.catatan_revisi,k.semester,k.tahun_ajaran,
    (SELECT COUNT(*) FROM krs_detail kd WHERE kd.id_pengajuan=pk.id_pengajuan) AS jumlah_matkul,
    (SELECT COALESCE(SUM(mc.sks),0) FROM krs_detail kd JOIN jadwal j ON j.id_jadwal=kd.id_jadwal JOIN matkul mc ON mc.id_matkul=j.id_matkul WHERE kd.id_pengajuan=pk.id_pengajuan) AS total_sks
    FROM pengajuan_krs pk JOIN krs k ON k.id_krs=pk.id_krs WHERE pk.id_mahasiswa=? ORDER BY pk.id_pengajuan DESC');
  $q->execute([$studentId]);$history=$q->fetchAll();
  if($detailId>0){
   $q=$pdo->prepare('SELECT pk.id_pengajuan,pk.status_acc,pk.catatan_revisi,pk.tanggal_pengajuan,k.semester,k.tahun_ajaran
      FROM pengajuan_krs pk JOIN krs k ON k.id_krs=pk.id_krs
      WHERE pk.id_pengajuan=? AND pk.id_mahasiswa=? LIMIT 1');
   $q->execute([$detailId,$studentId]);$detail=$q->fetch();
   if(!$detail){http_response_code(404);$error='Pengajuan tidak ditemukan untuk mahasiswa ini.';}
   else{
    $q=$pdo->prepare("SELECT kd.status_acc,kd.catatan_revisi,m.nama_matkul,m.sks,j.hari,j.jam_mulai,j.jam_selesai,kl.nama_kelas,r.nama_ruang,d.nama_dosen
      FROM krs_detail kd JOIN jadwal j ON j.id_jadwal=kd.id_jadwal JOIN matkul m ON m.id_matkul=j.id_matkul
      LEFT JOIN kelas kl ON kl.id_kelas=j.kelas LEFT JOIN ruang r ON r.id_ruang=j.ruang LEFT JOIN dosen d ON d.id_dosen=j.id_dosen
      WHERE kd.id_pengajuan=? ORDER BY FIELD(LOWER(j.hari),'senin','selasa','rabu','kamis','jumat','sabtu','minggu'),j.jam_mulai");
    $q->execute([$detailId]);$detailRows=$q->fetchAll();
   }
  }
 }
}
if($studentId===0){
 $q=$pdo->prepare('SELECT m.id_mahasiswa,m.nama_mahasiswa,m.angkatan,m.semester,p.nama_prodi FROM mahasiswa m LEFT JOIN prodi p ON p.id_prodi=m.id_prodi WHERE m.id_dosen_pa=? ORDER BY m.nama_mahasiswa');
 $q->execute([$dosenId]);$students=$q->fetchAll();
}
function mpa_status_class(string $s):string{return match($s){'Disetujui'=>'approved','Ditolak'=>'rejected','AccDosenPA','Ditinjau'=>'review','Draft'=>'draft',default=>'neutral'};}
?>
<!doctype html><html lang="id"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Mahasiswa PA — KRS</title>
<style>
*{box-sizing:border-box}body{margin:0;padding:18px;background:#d9ddd8;font-family:"Segoe UI",Arial,sans-serif;color:#303735}
.canvas{position:relative;width:min(1024px,100%);min-height:650px;margin:auto;background:#d9d5bb;box-shadow:0 12px 38px #24332f42}.bg{position:absolute;inset:0;width:100%;height:100%;object-fit:fill}.content{position:relative;padding:42px 7% 30px}
.heading{display:flex;justify-content:space-between;align-items:center;gap:10px;margin-bottom:18px}.heading h1{margin:0;color:#344641;font-size:28px}.heading p{margin:4px 0;color:#68756f;font-size:13px}
.back,.button{display:inline-block;padding:9px 13px;background:#f8f8f3;border:1px solid #69817a;color:#31514a;text-decoration:none;font-weight:700;font-size:12px}
.panel{background:#ffffffdf;border:1px solid #a4b8b0;padding:14px;margin-bottom:15px}.panel h2{margin:0 0 10px;color:#20675f;font-size:17px}
.table-wrap{overflow:auto}table{width:100%;border-collapse:collapse;background:white;font-size:13px}th{background:#0b7068;color:white;padding:10px;text-align:left;white-space:nowrap}td{padding:9px;border-bottom:1px solid #dce5e1}tbody tr:nth-child(even){background:#f1f5f3}
.status{display:inline-block;padding:4px 8px;border-radius:14px;font-size:11px;font-weight:700}.approved{background:#d8f0e4;color:#216b48}.rejected{background:#fde5e2;color:#a03229}.review{background:#fff0c8;color:#86580b}.draft{background:#e9eef0;color:#435a61}.neutral{background:#e5eae8;color:#53645e}
.meta{display:grid;grid-template-columns:repeat(4,1fr);gap:9px;margin-bottom:14px}.meta div{background:#fff;border:1px solid #b1c3bc;padding:9px}.meta small{display:block;color:#71807a;font-size:11px}.meta strong{display:block;color:#2d4d44;font-size:13px;margin-top:3px}.alert{background:#fff0ed;border:1px solid #dfb8ae;padding:10px;color:#8c3428}
@media(max-width:720px){body{padding:0}.content{padding:22px 13px}.heading{align-items:flex-start}.meta{grid-template-columns:1fr 1fr}.table-wrap table{min-width:760px}}
</style></head><body><main class="canvas"><img class="bg" src="https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Rectangle%20(right).png" alt=""><div class="content">
<header class="heading"><div><h1><?=($detailId>0?'Detail Riwayat KRS Mahasiswa':($studentId>0?'Riwayat KRS Mahasiswa Bimbingan':'Daftar Mahasiswa Bimbingan'))?></h1><p><?=krs_e((string)$advisor['nama_dosen'])?> · <?=krs_e($role)?></p></div><a class="back" href="<?=($detailId>0?'mahasiswa-pa.php?student='.$studentId:($studentId>0?'mahasiswa-pa.php':'index.php'))?>">Back</a></header>
<?php if($error!==''): ?><div class="alert"><?=krs_e($error)?></div><?php endif; ?>
<?php if($student): ?>
<div class="meta"><div><small>Nama Mahasiswa</small><strong><?=krs_e((string)$student['nama_mahasiswa'])?></strong></div><div><small>ID Mahasiswa</small><strong><?=krs_e((string)$student['id_mahasiswa'])?></strong></div><div><small>Program Studi</small><strong><?=krs_e((string)($student['nama_prodi']??'-'))?></strong></div><div><small>Angkatan / Semester</small><strong><?=krs_e((string)$student['angkatan'])?> / <?=krs_e((string)$student['semester'])?></strong></div></div>
<?php if($detail): ?><section class="panel"><h2>Pengajuan #<?=krs_e((string)$detail['id_pengajuan'])?> · Semester <?=krs_e((string)$detail['semester'])?> · <?=krs_e((string)$detail['tahun_ajaran'])?></h2><p>Status: <span class="status <?=mpa_status_class((string)$detail['status_acc'])?>"><?=krs_e((string)$detail['status_acc'])?></span> · Tanggal: <?=krs_e((string)$detail['tanggal_pengajuan'])?></p><?php if((string)$detail['catatan_revisi']!==''): ?><p style="white-space:pre-line"><strong>Catatan:</strong> <?=krs_e((string)$detail['catatan_revisi'])?></p><?php endif; ?>
<div class="table-wrap"><table><thead><tr><th>Mata Kuliah</th><th>SKS</th><th>Dosen Pengajar</th><th>Kelas</th><th>Hari</th><th>Jam</th><th>Ruang</th><th>Status</th><th>Catatan</th></tr></thead><tbody>
<?php if(!$detailRows): ?><tr><td colspan="9">Belum ada mata kuliah dalam pengajuan ini.</td></tr><?php else: foreach($detailRows as $d): ?><tr><td><?=krs_e((string)$d['nama_matkul'])?></td><td><?=krs_e((string)$d['sks'])?></td><td><?=krs_e((string)($d['nama_dosen']??'-'))?></td><td><?=krs_e((string)($d['nama_kelas']??'-'))?></td><td><?=krs_e(ucfirst((string)$d['hari']))?></td><td><?=krs_e((string)$d['jam_mulai'])?>–<?=krs_e((string)$d['jam_selesai'])?></td><td><?=krs_e((string)($d['nama_ruang']??'-'))?></td><td><span class="status <?=mpa_status_class((string)$d['status_acc'])?>"><?=krs_e((string)$d['status_acc'])?></span></td><td><?=krs_e((string)($d['catatan_revisi']??'-'))?></td></tr><?php endforeach; endif; ?>
</tbody></table></div></section>
<?php else: ?><section class="panel"><h2>Riwayat Pengajuan</h2><div class="table-wrap"><table><thead><tr><th>ID Pengajuan</th><th>Sesi</th><th>Tanggal</th><th>Mata Kuliah</th><th>SKS</th><th>Status</th><th>Detail</th></tr></thead><tbody>
<?php if(!$history): ?><tr><td colspan="7">Mahasiswa belum memiliki riwayat pengajuan.</td></tr><?php else: foreach($history as $h): ?><tr><td>#<?=krs_e((string)$h['id_pengajuan'])?></td><td>Sem <?=krs_e((string)$h['semester'])?> · <?=krs_e((string)$h['tahun_ajaran'])?></td><td><?=krs_e((string)$h['tanggal_pengajuan'])?></td><td><?=krs_e((string)$h['jumlah_matkul'])?></td><td><?=krs_e((string)$h['total_sks'])?></td><td><span class="status <?=mpa_status_class((string)$h['status_acc'])?>"><?=krs_e((string)$h['status_acc'])?></span></td><td><a class="button" href="mahasiswa-pa.php?student=<?=$studentId?>&detail=<?=krs_e((string)$h['id_pengajuan'])?>">Lihat Detail</a></td></tr><?php endforeach; endif; ?>
</tbody></table></div></section><?php endif; ?>
<?php else: ?>
<section class="panel"><h2>Mahasiswa yang Anda Bimbing</h2><div class="table-wrap"><table><thead><tr><th>ID Mahasiswa</th><th>Nama</th><th>Program Studi</th><th>Angkatan</th><th>Semester Aktif</th><th>Aksi</th></tr></thead><tbody>
<?php if(!$students): ?><tr><td colspan="6">Belum ada mahasiswa yang ditugaskan kepada Anda.</td></tr><?php else: foreach($students as $s): ?><tr><td><?=krs_e((string)$s['id_mahasiswa'])?></td><td><?=krs_e((string)$s['nama_mahasiswa'])?></td><td><?=krs_e((string)($s['nama_prodi']??'-'))?></td><td><?=krs_e((string)$s['angkatan'])?></td><td><?=krs_e((string)$s['semester'])?></td><td><a class="button" href="mahasiswa-pa.php?student=<?=krs_e((string)$s['id_mahasiswa'])?>">Lihat Riwayat KRS</a></td></tr><?php endforeach; endif; ?>
</tbody></table></div></section>
<?php endif; ?>
</div></main></body></html>
