<?php
declare(strict_types=1);
require __DIR__ . '/config.php';

$account=$_SESSION['krs_user']??null;
if(!is_array($account)){header('Location: index.php?notice=login-required',true,302);exit;}
if(($account['role']??'')!=='Admin'){http_response_code(403);exit('Akses ditolak. Riwayat KRS hanya untuk Admin.');}
$pdo=krs_db();
$years=$pdo->query('SELECT DISTINCT tahun_ajaran FROM krs ORDER BY tahun_ajaran DESC')->fetchAll(PDO::FETCH_COLUMN);
$year=trim((string)($_GET['year']??($years[0]??'')));
$semester=trim((string)($_GET['semester']??''));
$studentId=trim((string)($_GET['student']??''));
$detailId=filter_input(INPUT_GET,'detail',FILTER_VALIDATE_INT)?:0;
$print=(string)($_GET['print']??'')==='1';
$csv=(string)($_GET['export']??'')==='csv';
$validYear=$year===''||preg_match('/^\d{4}$/',$year);
$validSemester=$semester===''||ctype_digit($semester)&& (int)$semester>=1&&(int)$semester<=14;
$validStudent=$studentId===''||ctype_digit($studentId)&& (int)$studentId>0;
$error=(!$validYear||!$validSemester||!$validStudent)?'Filter riwayat tidak valid.':'';
if(!$validYear)$year='';
if(!$validSemester)$semester='';
if(!$validStudent)$studentId='';

$where=[];$params=[];
if($year!==''){$where[]='k.tahun_ajaran=?';$params[]=(int)$year;}
if($semester!==''){$where[]='k.semester=?';$params[]=(int)$semester;}
if($studentId!==''){$where[]='m.id_mahasiswa=?';$params[]=(int)$studentId;}
$whereSql=$where?'WHERE '.implode(' AND ',$where):'';

$baseSql=" FROM pengajuan_krs pk JOIN krs k ON k.id_krs=pk.id_krs
 JOIN mahasiswa m ON m.id_mahasiswa=pk.id_mahasiswa
 LEFT JOIN prodi p ON p.id_prodi=m.id_prodi
 LEFT JOIN dosen d ON d.id_dosen=m.id_dosen_pa
 LEFT JOIN dosen approver ON approver.id_dosen=pk.id_dosen_acc {$whereSql}";

if($csv){
 $q=$pdo->prepare("SELECT pk.id_pengajuan,k.id_krs,m.id_mahasiswa,m.nama_mahasiswa,p.nama_prodi,
   k.semester,k.tahun_ajaran,pk.tanggal_pengajuan,pk.status_acc,d.nama_dosen AS dosen_pa,
   approver.nama_dosen AS pemeriksa,mc.nama_matkul,mc.sks,kd.status_acc AS status_matkul,kd.catatan_revisi
   {$baseSql} LEFT JOIN krs_detail kd ON kd.id_pengajuan=pk.id_pengajuan
   LEFT JOIN jadwal j ON j.id_jadwal=kd.id_jadwal
   LEFT JOIN matkul mc ON mc.id_matkul=j.id_matkul
   ORDER BY k.tahun_ajaran DESC,k.semester,m.nama_mahasiswa,pk.id_pengajuan");
 $q->execute($params);
 header('Content-Type: text/csv; charset=utf-8');
 header('Content-Disposition: attachment; filename="riwayat-krs-'.($year!==''?$year:'semua').'.csv"');
 $out=fopen('php://output','wb');fwrite($out,"\xEF\xBB\xBF");
 fputcsv($out,['ID Pengajuan','ID Sesi','ID Mahasiswa','Nama Mahasiswa','Program Studi','Semester','Tahun Ajaran','Tanggal Pengajuan','Status Pengajuan','Dosen PA','Pemeriksa Terakhir','Mata Kuliah','SKS','Status Mata Kuliah','Catatan Revisi']);
 while($row=$q->fetch(PDO::FETCH_NUM))fputcsv($out,$row);
 fclose($out);exit;
}

$totalQuery=$pdo->prepare("SELECT COUNT(*) {$baseSql}");$totalQuery->execute($params);$total=(int)$totalQuery->fetchColumn();
$countsQuery=$pdo->prepare("SELECT pk.status_acc,COUNT(*) AS jumlah {$baseSql} GROUP BY pk.status_acc ORDER BY pk.status_acc");$countsQuery->execute($params);$statusCounts=$countsQuery->fetchAll();
$detail=null;$detailRows=[];
if($detailId>0){
 $detailWhere=$where ? $whereSql.' AND pk.id_pengajuan=?' : 'WHERE pk.id_pengajuan=?';
 $q=$pdo->prepare("SELECT pk.id_pengajuan,pk.id_krs,pk.tanggal_pengajuan,pk.status_acc,pk.catatan_revisi,
   m.id_mahasiswa,m.nama_mahasiswa,m.angkatan,m.semester AS semester_mhs,p.nama_prodi,
   k.semester,k.tahun_ajaran,k.status_krs,k.tanggal_mulai,k.tanggal_selesai,
   d.nama_dosen AS nama_dosen_pa,approver.nama_dosen AS nama_pemeriksa
   FROM pengajuan_krs pk JOIN krs k ON k.id_krs=pk.id_krs
   JOIN mahasiswa m ON m.id_mahasiswa=pk.id_mahasiswa
   LEFT JOIN prodi p ON p.id_prodi=m.id_prodi
   LEFT JOIN dosen d ON d.id_dosen=m.id_dosen_pa
   LEFT JOIN dosen approver ON approver.id_dosen=pk.id_dosen_acc
   {$detailWhere} LIMIT 1");
 $detailParams=$params;$detailParams[]=$detailId;$q->execute($detailParams);$detail=$q->fetch();
 if(!$detail){http_response_code(404);$error='Pengajuan tidak ditemukan pada filter yang dipilih.';}
 else{
  $rows=$pdo->prepare("SELECT kd.id_detail,kd.status_acc,kd.catatan_revisi,m.nama_matkul,m.sks,j.hari,j.jam_mulai,j.jam_selesai,
    kl.nama_kelas,r.nama_ruang,d.nama_dosen
    FROM krs_detail kd JOIN jadwal j ON j.id_jadwal=kd.id_jadwal
    JOIN matkul m ON m.id_matkul=j.id_matkul
    LEFT JOIN kelas kl ON kl.id_kelas=j.kelas LEFT JOIN ruang r ON r.id_ruang=j.ruang
    LEFT JOIN dosen d ON d.id_dosen=j.id_dosen WHERE kd.id_pengajuan=?
    ORDER BY FIELD(LOWER(j.hari),'senin','selasa','rabu','kamis','jumat','sabtu','minggu'),j.jam_mulai");
  $rows->execute([$detailId]);$detailRows=$rows->fetchAll();
 }
}
$listQuery=$pdo->prepare("SELECT pk.id_pengajuan,pk.id_krs,pk.tanggal_pengajuan,pk.status_acc,m.id_mahasiswa,m.nama_mahasiswa,
 p.nama_prodi,k.semester,k.tahun_ajaran,d.nama_dosen AS dosen_pa,
 (SELECT COUNT(*) FROM krs_detail kd WHERE kd.id_pengajuan=pk.id_pengajuan) AS jumlah_matkul,
 (SELECT COALESCE(SUM(mc.sks),0) FROM krs_detail kd JOIN jadwal j ON j.id_jadwal=kd.id_jadwal JOIN matkul mc ON mc.id_matkul=j.id_matkul WHERE kd.id_pengajuan=pk.id_pengajuan) AS total_sks
 {$baseSql} ORDER BY k.tahun_ajaran DESC,k.semester DESC,pk.tanggal_pengajuan DESC,pk.id_pengajuan DESC");
$listQuery->execute($params);$rows=$listQuery->fetchAll();

function history_status_class(string $status):string{return match($status){'Disetujui'=>'approved','Ditolak'=>'rejected','AccDosenPA'=>'review','Ditinjau'=>'review','Draft'=>'draft',default=>'neutral'};}
$queryParams=['year'=>$year,'semester'=>$semester,'student'=>$studentId];$queryParams=array_filter($queryParams,static fn($v)=>$v!=='');
$csvUrl='riwayat.php?'.http_build_query($queryParams+['export'=>'csv']);
$printUrl='riwayat.php?'.http_build_query($queryParams+['print'=>'1']);
$backUrl='riwayat.php?'.http_build_query($queryParams);
?>
<!doctype html><html lang="id"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Riwayat KRS — KRS</title>
<style>
*{box-sizing:border-box}body{margin:0;padding:18px;background:#d9ddd8;font-family:"Segoe UI",Arial,sans-serif;color:#303735}
.canvas{position:relative;width:min(1100px,100%);min-height:660px;margin:auto;background:#d9d5bb;box-shadow:0 12px 38px #24332f42}.bg{position:absolute;inset:0;width:100%;height:100%;object-fit:fill}.content{position:relative;padding:38px 5% 34px}
.heading{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:15px}.heading h1{margin:0;color:#344641;font-size:28px}.heading p{margin:3px 0;color:#68756f;font-size:12px}
.panel{background:#ffffffdf;border:1px solid #a4b8b0;padding:14px 16px;margin-bottom:15px}.panel h2{margin:0 0 10px;color:#20675f;font-size:17px}
.back,.button{display:inline-block;padding:9px 13px;background:#f8f8f3;border:1px solid #69817a;color:#31514a;text-decoration:none;font-weight:700;font-size:12px}.primary{background:#0b7068;color:#fff;border-color:#0b7068}
.filters{display:grid;grid-template-columns:1fr 1fr 1fr auto;gap:10px;align-items:end}.field{display:flex;flex-direction:column;gap:5px}label{font-size:12px;font-weight:700;color:#38534c}input,select{min-height:38px;padding:8px;border:1px solid #aebfba;background:#fff;color:#283a36;font:13px "Segoe UI",Arial,sans-serif}
.toolbar{display:flex;gap:8px;flex-wrap:wrap;margin:12px 0}
.table-wrap{overflow:auto}table{width:100%;border-collapse:collapse;font-size:12px;background:#fff}th{background:#0b7068;color:#fff;text-align:left;padding:10px;white-space:nowrap}td{padding:8px;border-bottom:1px solid #dce5e1;vertical-align:middle}tbody tr:nth-child(even){background:#f1f5f3}
.status{display:inline-block;padding:4px 8px;border-radius:14px;font-size:11px;font-weight:700;white-space:nowrap}.approved{background:#d8f0e4;color:#216b48}.rejected{background:#fde5e2;color:#a03229}.review{background:#fff0c8;color:#86580b}.draft{background:#e9eef0;color:#435a61}.neutral{background:#e5eae8;color:#53645e}
.summary{display:flex;gap:10px;flex-wrap:wrap}.summary span{padding:7px 10px;background:#f0f4f2;border:1px solid #d3dfda;font-size:12px}
.alert{background:#fff0ed;border:1px solid #dfb8ae;padding:10px;color:#8c3428;margin-bottom:12px}
.meta{display:grid;grid-template-columns:repeat(4,1fr);gap:9px;margin-bottom:12px}.meta div{background:#fff;border:1px solid #b1c3bc;padding:9px}.meta small{display:block;color:#71807a;font-size:11px}.meta strong{display:block;margin-top:3px;color:#2d4d44;font-size:13px}
@media(max-width:760px){body{padding:0}.content{padding:20px 12px}.filters{grid-template-columns:1fr 1fr}.meta{grid-template-columns:1fr 1fr}.table-wrap table{min-width:850px}}
@media print{body{padding:0;background:#fff}.canvas{box-shadow:none;width:100%;min-height:0}.bg{display:none}.content{padding:0}.no-print,.back{display:none!important}.panel{break-inside:avoid;border:1px solid #aaa}.table-wrap{overflow:visible}table{font-size:9pt}th{background:#ddd!important;color:#000!important;-webkit-print-color-adjust:exact;print-color-adjust:exact}td{padding:5px}}
</style></head><body>
<main class="canvas"><img class="bg" src="https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Manajemen%20data.png" alt="">
<div class="content"><header class="heading"><div><h1><?=($detail?'Detail Riwayat KRS':'Riwayat KRS Mahasiswa')?></h1><p>Arsip pengajuan dan hasil pemeriksaan KRS</p></div><a class="back no-print" href="<?=($detail?$backUrl:'index.php')?>">Back</a></header>
<?php if($error!==''): ?><div class="alert"><?=krs_e($error)?></div><?php endif; ?>
<?php if($detail): ?>
<div class="meta"><div><small>ID Pengajuan</small><strong>#<?=krs_e((string)$detail['id_pengajuan'])?></strong></div><div><small>Mahasiswa</small><strong><?=krs_e((string)$detail['nama_mahasiswa'])?> (<?=krs_e((string)$detail['id_mahasiswa'])?>)</strong></div><div><small>Program Studi</small><strong><?=krs_e((string)($detail['nama_prodi']??'-'))?></strong></div><div><small>Sesi Akademik</small><strong>Semester <?=krs_e((string)$detail['semester'])?> · <?=krs_e((string)$detail['tahun_ajaran'])?></strong></div>
<div><small>Tanggal Pengajuan</small><strong><?=krs_e((string)$detail['tanggal_pengajuan'])?></strong></div><div><small>Dosen PA</small><strong><?=krs_e((string)($detail['nama_dosen_pa']??'-'))?></strong></div><div><small>Pemeriksa Terakhir</small><strong><?=krs_e((string)($detail['nama_pemeriksa']??'-'))?></strong></div><div><small>Status</small><strong><span class="status <?=history_status_class((string)$detail['status_acc'])?>"><?=krs_e((string)$detail['status_acc'])?></span></strong></div></div>
<?php if((string)$detail['catatan_revisi']!==''): ?><div class="panel"><strong>Catatan:</strong><p style="white-space:pre-line"><?=krs_e((string)$detail['catatan_revisi'])?></p></div><?php endif; ?>
<section class="panel"><h2>Rincian Mata Kuliah</h2><div class="table-wrap"><table><thead><tr><th>Mata Kuliah</th><th>SKS</th><th>Dosen</th><th>Kelas</th><th>Hari</th><th>Jam</th><th>Ruang</th><th>Status</th><th>Catatan</th></tr></thead><tbody>
<?php if(!$detailRows): ?><tr><td colspan="9">Tidak ada mata kuliah.</td></tr><?php else: foreach($detailRows as $d): ?><tr><td><?=krs_e((string)$d['nama_matkul'])?></td><td><?=krs_e((string)$d['sks'])?></td><td><?=krs_e((string)($d['nama_dosen']??'-'))?></td><td><?=krs_e((string)($d['nama_kelas']??'-'))?></td><td><?=krs_e(ucfirst((string)$d['hari']))?></td><td><?=krs_e((string)$d['jam_mulai'])?>–<?=krs_e((string)$d['jam_selesai'])?></td><td><?=krs_e((string)($d['nama_ruang']??'-'))?></td><td><?=krs_e((string)$d['status_acc'])?></td><td><?=krs_e((string)($d['catatan_revisi']??'-'))?></td></tr><?php endforeach; endif; ?>
</tbody></table></div></section>
<div class="toolbar no-print"><a class="button" href="<?=$backUrl?>">Kembali ke Riwayat</a><a class="button primary" href="<?=$printUrl?>">Cetak / Simpan PDF</a></div>
<?php else: ?>
<section class="panel no-print"><h2>Filter Arsip Periode</h2><form method="get" action="riwayat.php" class="filters">
<div class="field"><label for="year">Tahun Ajaran</label><select id="year" name="year"><option value="">Semua Tahun</option><?php foreach($years as $y): ?><option value="<?=krs_e((string)$y)?>" <?=($year===(string)$y?'selected':'')?>><?=krs_e((string)$y)?></option><?php endforeach; ?></select></div>
<div class="field"><label for="semester">Semester</label><select id="semester" name="semester"><option value="">Semua Semester</option><?php for($i=1;$i<=14;$i++): ?><option value="<?=$i?>" <?=($semester===(string)$i?'selected':'')?>><?=$i?></option><?php endfor; ?></select></div>
<div class="field"><label for="student">ID Mahasiswa (opsional)</label><input id="student" name="student" inputmode="numeric" value="<?=krs_e($studentId)?>" placeholder="Contoh: 1000"></div>
<button class="primary" type="submit">Tampilkan Data</button></form>
<div class="toolbar"><a class="button" href="<?=$csvUrl?>">Ekspor CSV</a><a class="button primary" href="<?=$printUrl?>">Cetak Arsip Periode</a></div></section>
<section class="panel"><div class="summary"><span>Total pengajuan: <strong><?=$total?></strong></span><?php foreach($statusCounts as $c): ?><span><?=krs_e((string)$c['status_acc'])?>: <strong><?=krs_e((string)$c['jumlah'])?></strong></span><?php endforeach; ?></div>
<div class="table-wrap" style="margin-top:12px"><table><thead><tr><th>ID Pengajuan</th><th>ID Sesi</th><th>ID Mahasiswa</th><th>Nama Mahasiswa</th><th>Program Studi</th><th>Semester</th><th>Tahun</th><th>Tanggal</th><th>Dosen PA</th><th>Mata Kuliah</th><th>SKS</th><th>Status</th><th>Detail</th></tr></thead><tbody>
<?php if(!$rows): ?><tr><td colspan="13">Tidak ada riwayat sesuai filter.</td></tr><?php else: foreach($rows as $row): ?><tr>
<td>#<?=krs_e((string)$row['id_pengajuan'])?></td><td><?=krs_e((string)$row['id_krs'])?></td><td><?=krs_e((string)$row['id_mahasiswa'])?></td><td><?=krs_e((string)$row['nama_mahasiswa'])?></td><td><?=krs_e((string)($row['nama_prodi']??'-'))?></td><td><?=krs_e((string)$row['semester'])?></td><td><?=krs_e((string)$row['tahun_ajaran'])?></td><td><?=krs_e((string)$row['tanggal_pengajuan'])?></td><td><?=krs_e((string)($row['dosen_pa']??'-'))?></td><td><?=krs_e((string)$row['jumlah_matkul'])?></td><td><?=krs_e((string)$row['total_sks'])?></td><td><span class="status <?=history_status_class((string)$row['status_acc'])?>"><?=krs_e((string)$row['status_acc'])?></span></td><td><a class="button" href="riwayat.php?<?=http_build_query($queryParams+['detail'=>$row['id_pengajuan']])?>">Lihat</a></td>
</tr><?php endforeach; endif; ?></tbody></table></div></section>
<?php endif; ?>
</div></main>
<?php if($print): ?><script>window.addEventListener('load',function(){window.print();});</script><?php endif; ?>
</body></html>
