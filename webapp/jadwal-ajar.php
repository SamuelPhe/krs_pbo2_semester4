<?php
declare(strict_types=1);
require __DIR__ . '/config.php';
$account=$_SESSION['krs_user']??null;
if(!is_array($account)){header('Location: index.php?notice=login-required',true,302);exit;}
$role=(string)($account['role']??'');
$expected=['Dosen'=>'dosen','Dosen PA'=>'dosen pa','Kaprodi'=>'kaprodi'];
if(!isset($expected[$role])){http_response_code(403);exit('Akses ditolak.');}
$pdo=krs_db();$id=(int)($account['id']??0);
$q=$pdo->prepare('SELECT id_dosen,nama_dosen,posisi FROM dosen WHERE id_dosen=? LIMIT 1');$q->execute([$id]);$lecturer=$q->fetch();
if(!$lecturer||strtolower((string)$lecturer['posisi'])!==$expected[$role]){http_response_code(403);exit('Akun dosen tidak valid.');}
$scheduleId=filter_input(INPUT_GET,'schedule',FILTER_VALIDATE_INT)?:0;
$error='';
$schedules=$pdo->prepare("SELECT j.id_jadwal,m.nama_matkul,m.sks,j.hari,j.jam_mulai,j.jam_selesai,
  kl.nama_kelas,r.nama_ruang,
  (SELECT COUNT(*) FROM krs_detail kd JOIN pengajuan_krs pk ON pk.id_pengajuan=kd.id_pengajuan
    WHERE kd.id_jadwal=j.id_jadwal AND pk.status_acc IN ('Ditinjau','Disetujui','AccDosenPA')) AS jumlah_mhs
  FROM jadwal j JOIN matkul m ON m.id_matkul=j.id_matkul
  LEFT JOIN kelas kl ON kl.id_kelas=j.kelas LEFT JOIN ruang r ON r.id_ruang=j.ruang
  WHERE j.id_dosen=? ORDER BY FIELD(LOWER(j.hari),'senin','selasa','rabu','kamis','jumat','sabtu','minggu'),j.jam_mulai");
$schedules->execute([$id]);$rows=$schedules->fetchAll();
$students=[];
if($scheduleId>0){
 $exists=$pdo->prepare('SELECT j.id_jadwal,m.nama_matkul FROM jadwal j JOIN matkul m ON m.id_matkul=j.id_matkul WHERE j.id_jadwal=? AND j.id_dosen=? LIMIT 1');
 $exists->execute([$scheduleId,$id]);$subject=$exists->fetch();
 if(!$subject){http_response_code(404);$error='Jadwal tidak ditemukan pada daftar mengajar Anda.';}
 else{
  $q=$pdo->prepare("SELECT DISTINCT m.id_mahasiswa,m.nama_mahasiswa,p.nama_prodi,m.angkatan,m.semester,
      pk.id_pengajuan,pk.status_acc AS status_pengajuan,k.semester AS semester_krs,k.tahun_ajaran
    FROM krs_detail kd JOIN pengajuan_krs pk ON pk.id_pengajuan=kd.id_pengajuan
    JOIN mahasiswa m ON m.id_mahasiswa=pk.id_mahasiswa
    LEFT JOIN prodi p ON p.id_prodi=m.id_prodi JOIN krs k ON k.id_krs=pk.id_krs
    WHERE kd.id_jadwal=? AND pk.status_acc IN ('Ditinjau','Disetujui','AccDosenPA')
    ORDER BY m.nama_mahasiswa");
  $q->execute([$scheduleId]);$students=$q->fetchAll();
 }
}
?>
<!doctype html><html lang="id"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Jadwal Ajar — KRS</title>
<style>
*{box-sizing:border-box}body{margin:0;padding:18px;background:#d9ddd8;font-family:"Segoe UI",Arial,sans-serif;color:#303735}
.canvas{position:relative;width:min(1024px,100%);min-height:650px;margin:auto;background:#d9d5bb;box-shadow:0 12px 38px #24332f42}.bg{position:absolute;inset:0;width:100%;height:100%;object-fit:fill}.content{position:relative;padding:42px 7% 30px}
.heading{display:flex;justify-content:space-between;align-items:center;gap:10px;margin-bottom:18px}.heading h1{margin:0;color:#344641;font-size:28px}.heading p{margin:4px 0;color:#68756f;font-size:13px}
.back,.button{display:inline-block;padding:9px 13px;background:#f8f8f3;border:1px solid #69817a;color:#31514a;text-decoration:none;font-weight:700;font-size:12px}.panel{background:#ffffffdf;border:1px solid #a4b8b0;padding:14px;margin-bottom:15px}.panel h2{margin:0 0 10px;color:#20675f;font-size:17px}
.table-wrap{overflow:auto}table{width:100%;border-collapse:collapse;background:white;font-size:13px}th{background:#0b7068;color:white;padding:10px;text-align:left;white-space:nowrap}td{padding:9px;border-bottom:1px solid #dce5e1}tbody tr:nth-child(even){background:#f1f5f3}
.status{display:inline-block;padding:4px 8px;border-radius:14px;font-size:11px;background:#e8f0ed;color:#3e6156;font-weight:700}.alert{background:#fff0ed;border:1px solid #dfb8ae;padding:10px;color:#8c3428}
@media(max-width:720px){body{padding:0}.content{padding:22px 13px}.heading{align-items:flex-start}.table-wrap table{min-width:750px}}
</style></head><body><main class="canvas"><img class="bg" src="https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Rectangle%20(right).png" alt=""><div class="content">
<header class="heading"><div><h1><?=($scheduleId>0?'Daftar Mahasiswa pada Jadwal':'Jadwal Ajar Saya')?></h1><p><?=krs_e((string)$lecturer['nama_dosen'])?></p></div><a class="back" href="<?=($scheduleId>0?'jadwal-ajar.php':'index.php')?>">Back</a></header>
<?php if($error!==''): ?><div class="alert"><?=krs_e($error)?></div><?php endif; ?>
<?php if($scheduleId>0&&isset($subject)): ?>
<section class="panel"><h2><?=krs_e((string)$subject['nama_matkul'])?></h2><p>Daftar mahasiswa yang mengambil jadwal ini dengan pengajuan berstatus Ditinjau, AccDosenPA, atau Disetujui.</p>
<div class="table-wrap"><table><thead><tr><th>ID Mahasiswa</th><th>Nama Mahasiswa</th><th>Program Studi</th><th>Angkatan</th><th>Semester Mahasiswa</th><th>ID Pengajuan</th><th>Status Pengajuan</th><th>Sesi</th></tr></thead><tbody>
<?php if(!$students): ?><tr><td colspan="8">Belum ada mahasiswa pada jadwal ini.</td></tr><?php else: foreach($students as $s): ?><tr><td><?=krs_e((string)$s['id_mahasiswa'])?></td><td><?=krs_e((string)$s['nama_mahasiswa'])?></td><td><?=krs_e((string)($s['nama_prodi']??'-'))?></td><td><?=krs_e((string)$s['angkatan'])?></td><td><?=krs_e((string)$s['semester'])?></td><td>#<?=krs_e((string)$s['id_pengajuan'])?></td><td><span class="status"><?=krs_e((string)$s['status_pengajuan'])?></span></td><td>Sem <?=krs_e((string)$s['semester_krs'])?> · <?=krs_e((string)$s['tahun_ajaran'])?></td></tr><?php endforeach; endif; ?>
</tbody></table></div></section>
<?php else: ?>
<section class="panel"><h2>Daftar Jadwal Mengajar</h2><div class="table-wrap"><table><thead><tr><th>ID Jadwal</th><th>Mata Kuliah</th><th>SKS</th><th>Hari</th><th>Jam</th><th>Kelas</th><th>Ruang</th><th>Mahasiswa</th><th>Aksi</th></tr></thead><tbody>
<?php if(!$rows): ?><tr><td colspan="9">Belum ada jadwal mengajar yang ditugaskan.</td></tr><?php else: foreach($rows as $s): ?><tr>
<td><?=krs_e((string)$s['id_jadwal'])?></td><td><?=krs_e((string)$s['nama_matkul'])?></td><td><?=krs_e((string)$s['sks'])?></td><td><?=krs_e(ucfirst((string)$s['hari']))?></td><td><?=krs_e((string)$s['jam_mulai'])?>–<?=krs_e((string)$s['jam_selesai'])?></td><td><?=krs_e((string)($s['nama_kelas']??'-'))?></td><td><?=krs_e((string)($s['nama_ruang']??'-'))?></td><td><?=krs_e((string)$s['jumlah_mhs'])?> / 40</td><td><a class="button" href="jadwal-ajar.php?schedule=<?=krs_e((string)$s['id_jadwal'])?>">Lihat Mahasiswa</a></td>
</tr><?php endforeach; endif; ?></tbody></table></div></section>
<?php endif; ?>
</div></main></body></html>
