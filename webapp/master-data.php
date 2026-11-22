<?php
declare(strict_types=1);

require __DIR__ . '/config.php';

$account = $_SESSION['krs_user'] ?? null;
if (!is_array($account)) {
    header('Location: index.php?notice=login-required', true, 302);
    exit;
}
if (($account['role'] ?? '') !== 'Admin') {
    http_response_code(403);
    exit('Akses ditolak. Master Data hanya untuk Admin.');
}
$pdo = krs_db();

$entities = [
    'prodi' => ['label' => 'Data Prodi', 'id' => 'id_prodi', 'name' => 'nama_prodi'],
    'matkul' => ['label' => 'Data Matkul', 'id' => 'id_matkul', 'name' => 'nama_matkul'],
    'jadwal' => ['label' => 'Data Jadwal', 'id' => 'id_jadwal', 'name' => 'id_jadwal'],
    'kelas' => ['label' => 'Data Kelas', 'id' => 'id_kelas', 'name' => 'nama_kelas'],
    'ruang' => ['label' => 'Data Ruang', 'id' => 'id_ruang', 'name' => 'nama_ruang'],
];
$type = (string)($_GET['type'] ?? $_POST['type'] ?? 'prodi');
if (!isset($entities[$type])) $type = 'prodi';
$meta = $entities[$type];
$editId = filter_input(INPUT_GET, 'edit', FILTER_VALIDATE_INT) ?: 0;
$showForm = isset($_GET['new']) || $editId > 0;
$error = '';
$success = (string)($_SESSION['master_data_flash'] ?? '');
unset($_SESSION['master_data_flash']);

$programs = $pdo->query('SELECT id_prodi, nama_prodi FROM prodi ORDER BY nama_prodi')->fetchAll();
$kaprodi = $pdo->query("SELECT id_dosen, nama_dosen FROM dosen WHERE posisi = 'kaprodi' ORDER BY nama_dosen")->fetchAll();
$courses = $pdo->query('SELECT id_matkul, nama_matkul, id_prodi FROM matkul ORDER BY nama_matkul')->fetchAll();
$lecturers = $pdo->query('SELECT id_dosen, nama_dosen FROM dosen ORDER BY nama_dosen')->fetchAll();
$classes = $pdo->query('SELECT id_kelas, nama_kelas FROM kelas ORDER BY nama_kelas')->fetchAll();
$rooms = $pdo->query('SELECT id_ruang, nama_ruang FROM ruang ORDER BY nama_ruang')->fetchAll();

$values = ['id'=>'','name'=>'','program'=>'','kaprodi'=>'','sks'=>'3','semester'=>'1','course'=>'','lecturer'=>'','day'=>'senin','class'=>'','room'=>'','start'=>'07:00','end'=>'08:40'];
if ($editId > 0) {
    $query = $pdo->prepare("SELECT * FROM {$type} WHERE {$meta['id']} = ? LIMIT 1");
    $query->execute([$editId]);
    $row = $query->fetch();
    if (!$row) {
        $editId=0;$showForm=false;$error='Data yang dipilih tidak ditemukan.';
    } else {
        $values['id']=(string)$row[$meta['id']];
        if ($type==='prodi') {
            $values['name']=(string)$row['nama_prodi'];$values['kaprodi']=(string)($row['id_kaprodi'] ?? '');
        } elseif ($type==='matkul') {
            $values['name']=(string)$row['nama_matkul'];$values['program']=(string)$row['id_prodi'];$values['sks']=(string)$row['sks'];$values['semester']=(string)$row['semester'];
        } elseif ($type==='jadwal') {
            $values['course']=(string)$row['id_matkul'];$values['lecturer']=(string)$row['id_dosen'];$values['day']=(string)$row['hari'];
            $values['class']=(string)$row['kelas'];$values['room']=(string)$row['ruang'];$values['start']=substr((string)$row['jam_mulai'],0,5);$values['end']=substr((string)$row['jam_selesai'],0,5);
        } else {
            $values['name']=(string)$row[$meta['name']];
        }
    }
}

if ($_SERVER['REQUEST_METHOD']==='POST') {
    $token=(string)($_SESSION['csrf_token']??'');$sent=(string)($_POST['csrf_token']??'');
    if($token===''||!hash_equals($token,$sent)){http_response_code(403);$error='Sesi formulir tidak valid. Muat ulang halaman.';}
    else{
        $action=(string)($_POST['action']??'');
        $id=filter_var($_POST['id']??'',FILTER_VALIDATE_INT);$id=($id===false||$id===null||$id<1)?0:(int)$id;
        try{
            if($action==='delete'){
                if($id<1){$error='ID data tidak valid.';}
                else{
                    $pdo->beginTransaction();$guard='';
                    $guards=match($type){
                        'prodi'=>[
                            ['SELECT COUNT(*) FROM mahasiswa WHERE id_prodi=?','Program studi masih digunakan oleh data mahasiswa.'],
                            ['SELECT COUNT(*) FROM matkul WHERE id_prodi=?','Program studi masih memiliki mata kuliah.'],
                            ['SELECT COUNT(*) FROM krs WHERE id_prodi=?','Program studi masih memiliki sesi KRS.'],
                        ],
                        'matkul'=>[['SELECT COUNT(*) FROM jadwal WHERE id_matkul=?','Mata kuliah masih memiliki jadwal kuliah.']],
                        'dosen'=>[],
                        'kelas'=>[['SELECT COUNT(*) FROM jadwal WHERE kelas=?','Kelas masih dipakai pada jadwal.']],
                        'ruang'=>[['SELECT COUNT(*) FROM jadwal WHERE ruang=?','Ruang masih dipakai pada jadwal.']],
                        default=>[],
                    };
                    if($type==='jadwal'){
                        $check=$pdo->prepare('SELECT COUNT(*) FROM krs_detail WHERE id_jadwal=?');$check->execute([$id]);
                        if((int)$check->fetchColumn()>0)$guard='Jadwal sudah dipakai dalam pengajuan KRS dan tidak dapat dihapus.';
                    } elseif($type==='dosen') {
                        $check=$pdo->prepare('SELECT COUNT(*) FROM mahasiswa WHERE id_dosen_pa=?');$check->execute([$id]);
                        if((int)$check->fetchColumn()>0)$guard='Dosen masih menjadi pembimbing akademik mahasiswa.';
                        $check=$pdo->prepare('SELECT COUNT(*) FROM prodi WHERE id_kaprodi=?');$check->execute([$id]);
                        if($guard===''&&(int)$check->fetchColumn()>0)$guard='Dosen masih ditetapkan sebagai Kaprodi.';
                        $check=$pdo->prepare('SELECT COUNT(*) FROM jadwal WHERE id_dosen=?');$check->execute([$id]);
                        if($guard===''&&(int)$check->fetchColumn()>0)$guard='Dosen masih memiliki jadwal mengajar.';
                    } else {
                        foreach($guards as [$sql,$message]){
                            $check=$pdo->prepare($sql);$check->execute([$id]);
                            if((int)$check->fetchColumn()>0){$guard=$message;break;}
                        }
                    }
                    if($guard!==''){$pdo->rollBack();$error=$guard;}
                    else{
                        $delete=$pdo->prepare("DELETE FROM {$type} WHERE {$meta['id']}=?");$delete->execute([$id]);$deleted=$delete->rowCount();$pdo->commit();
                        if($deleted>0){$_SESSION['master_data_flash']='Data berhasil dihapus.';header('Location: master-data.php?type='.$type,true,303);exit;}
                        $error='Data tidak ditemukan.';
                    }
                }
            } elseif($action==='save'){
                $values=[
                    'id'=>trim((string)($_POST['id']??'')),'name'=>trim((string)($_POST['name']??'')),
                    'program'=>trim((string)($_POST['program']??'')),'kaprodi'=>trim((string)($_POST['kaprodi']??'')),
                    'sks'=>trim((string)($_POST['sks']??'')),'semester'=>trim((string)($_POST['semester']??'')),
                    'course'=>trim((string)($_POST['course']??'')),'lecturer'=>trim((string)($_POST['lecturer']??'')),
                    'day'=>trim((string)($_POST['day']??'')),'class'=>trim((string)($_POST['class']??'')),
                    'room'=>trim((string)($_POST['room']??'')),'start'=>trim((string)($_POST['start']??'')),
                    'end'=>trim((string)($_POST['end']??'')),
                ];
                $idValue=$values['id']===''?null:filter_var($values['id'],FILTER_VALIDATE_INT);
                if($idValue===false||($idValue!==null&&$idValue<1))$error='ID harus berupa bilangan bulat positif.';
                elseif($editId>0&&(int)$idValue!==$editId)$error='ID utama tidak dapat diubah.';
                if($error===''&&$editId===0&&$idValue!==null){
                    $check=$pdo->prepare("SELECT COUNT(*) FROM {$type} WHERE {$meta['id']}=?");$check->execute([(int)$idValue]);
                    if((int)$check->fetchColumn()>0)$error='ID tersebut sudah digunakan.';
                }

                if($error===''&&in_array($type,['prodi','matkul','kelas','ruang'],true)){
                    if($values['name']===''||strlen($values['name'])>100)$error='Nama wajib diisi dan maksimal 100 karakter.';
                }
                if($error===''&&$type==='prodi'){
                    $kaprodiId=$values['kaprodi']===''?null:filter_var($values['kaprodi'],FILTER_VALIDATE_INT);
                    if($kaprodiId===false||($kaprodiId!==null&&$kaprodiId<1))$error='Pilih Kaprodi yang valid.';
                    elseif($kaprodiId!==null){$check=$pdo->prepare("SELECT COUNT(*) FROM dosen WHERE id_dosen=? AND posisi='kaprodi'");$check->execute([$kaprodiId]);if((int)$check->fetchColumn()!==1)$error='Akun yang dipilih bukan Kaprodi.';}
                }
                if($error===''&&$type==='matkul'){
                    $programId=filter_var($values['program'],FILTER_VALIDATE_INT);$sks=filter_var($values['sks'],FILTER_VALIDATE_INT);$semester=filter_var($values['semester'],FILTER_VALIDATE_INT);
                    if($programId===false||$programId<1)$error='Pilih program studi.';
                    elseif($sks===false||$sks<1||$sks>24)$error='SKS harus 1 sampai 24.';
                    elseif($semester===false||$semester<1||$semester>14)$error='Semester harus 1 sampai 14.';
                    else{$check=$pdo->prepare('SELECT COUNT(*) FROM prodi WHERE id_prodi=?');$check->execute([$programId]);if((int)$check->fetchColumn()!==1)$error='Program studi tidak ditemukan.';}
                }
                $days=['senin','selasa','rabu','kamis','jumat','sabtu','minggu'];
                if($error===''&&$type==='jadwal'){
                    $course=filter_var($values['course'],FILTER_VALIDATE_INT);$lecturer=filter_var($values['lecturer'],FILTER_VALIDATE_INT);
                    $class=filter_var($values['class'],FILTER_VALIDATE_INT);$room=filter_var($values['room'],FILTER_VALIDATE_INT);
                    $startTime=$values['start'];$endTime=$values['end'];
                    if($course===false||$lecturer===false||$class===false||$room===false)$error='Pilih mata kuliah, dosen, kelas, dan ruang yang valid.';
                    elseif(!in_array($values['day'],$days,true))$error='Hari kuliah tidak valid.';
                    elseif(!preg_match('/^(?:[01]\d|2[0-3]):[0-5]\d$/',$startTime)||!preg_match('/^(?:[01]\d|2[0-3]):[0-5]\d$/',$endTime)||$startTime>=$endTime)$error='Jam mulai dan selesai tidak valid.';
                    else{
                        $refs=[['SELECT COUNT(*) FROM matkul WHERE id_matkul=?',$course],['SELECT COUNT(*) FROM dosen WHERE id_dosen=?',$lecturer],['SELECT COUNT(*) FROM kelas WHERE id_kelas=?',$class],['SELECT COUNT(*) FROM ruang WHERE id_ruang=?',$room]];
                        foreach($refs as [$sql,$value]){$check=$pdo->prepare($sql);$check->execute([$value]);if((int)$check->fetchColumn()!==1){$error='Salah satu referensi jadwal tidak ditemukan.';break;}}
                        if($error===''){
                            $overlapSql='SELECT j.id_jadwal, j.id_dosen, j.kelas, j.ruang, m.nama_matkul
                                FROM jadwal j LEFT JOIN matkul m ON m.id_matkul=j.id_matkul
                                WHERE LOWER(j.hari)=? AND j.jam_mulai < ? AND j.jam_selesai > ?
                                  AND (j.id_dosen=? OR j.kelas=? OR j.ruang=?)';
                            $params=[$values['day'],$endTime,$startTime,$lecturer,(string)$class,(string)$room];
                            if($editId>0){$overlapSql.=' AND j.id_jadwal<>?';$params[]=$editId;}
                            $check=$pdo->prepare($overlapSql);$check->execute($params);$conflicts=$check->fetchAll();
                            if($conflicts){$who=[];foreach($conflicts as $conflict){if((int)$conflict['id_dosen']===(int)$lecturer)$who[]='dosen';if((string)$conflict['kelas']===(string)$class)$who[]='kelas';if((string)$conflict['ruang']===(string)$room)$who[]='ruang';}$error='Jadwal bentrok pada hari/jam tersebut ('.implode(', ',array_unique($who)).').';}
                        }
                    }
                }

                if($error===''){
                    if($type==='prodi'){
                        $data=[$values['name'],$kaprodiId??null];
                        if($editId>0){$sql='UPDATE prodi SET nama_prodi=?, id_kaprodi=? WHERE id_prodi=?';$data[]=$editId;}
                        else{$sql='INSERT INTO prodi (nama_prodi,id_kaprodi'.($idValue!==null?',id_prodi':'').') VALUES (?,?'.($idValue!==null?',?':'').')';if($idValue!==null)$data[]=(int)$idValue;}
                    } elseif($type==='matkul'){
                        $data=[$values['name'],(int)$sks,(int)$semester,(int)$programId];
                        if($editId>0){$sql='UPDATE matkul SET nama_matkul=?,sks=?,semester=?,id_prodi=? WHERE id_matkul=?';$data[]=$editId;}
                        else{$sql='INSERT INTO matkul (nama_matkul,sks,semester,id_prodi'.($idValue!==null?',id_matkul':'').') VALUES (?,?,?,?'.($idValue!==null?',?':'').')';if($idValue!==null)$data[]=(int)$idValue;}
                    } elseif($type==='jadwal'){
                        $data=[(int)$course,(int)$lecturer,$values['day'],(string)$class,$startTime.':00',$endTime.':00',(string)$room];
                        if($editId>0){$sql='UPDATE jadwal SET id_matkul=?,id_dosen=?,hari=?,kelas=?,jam_mulai=?,jam_selesai=?,ruang=? WHERE id_jadwal=?';$data[]=$editId;}
                        else{$sql='INSERT INTO jadwal (id_matkul,id_dosen,hari,kelas,jam_mulai,jam_selesai,ruang'.($idValue!==null?',id_jadwal':'').') VALUES (?,?,?,?,?,?,?'.($idValue!==null?',?':'').')';if($idValue!==null)$data[]=(int)$idValue;}
                    } else {
                        $data=[$values['name']];
                        if($editId>0){$sql="UPDATE {$type} SET {$meta['name']}=? WHERE {$meta['id']}=?";$data[]=$editId;}
                        else{$sql="INSERT INTO {$type} ({$meta['name']}".($idValue!==null?','.$meta['id']:'').') VALUES (?'.($idValue!==null?',?':'').')';if($idValue!==null)$data[]=(int)$idValue;}
                    }
                    $save=$pdo->prepare($sql);$save->execute($data);
                    $_SESSION['master_data_flash']=$editId>0?'Data berhasil diperbarui.':'Data berhasil ditambahkan.';
                    header('Location: master-data.php?type='.$type,true,303);exit;
                }
                $editId=(int)($idValue?:0);$showForm=true;
            } else {$error='Aksi Master Data tidak dikenal.';}
        }catch(Throwable $exception){
            if($pdo->inTransaction())$pdo->rollBack();
            error_log('KRS master data error: '.$exception->getMessage());
            $error='Data gagal disimpan. Periksa nama, ID, jadwal bentrok, dan relasi.';
            if($action==='save')$showForm=true;
        }
    }
}

if($type==='prodi'){
    $rows=$pdo->query('SELECT p.id_prodi,p.nama_prodi,d.nama_dosen FROM prodi p LEFT JOIN dosen d ON d.id_dosen=p.id_kaprodi ORDER BY p.id_prodi')->fetchAll();
    $headers=['ID Prodi','Program Studi','Kaprodi'];
}elseif($type==='matkul'){
    $rows=$pdo->query('SELECT m.id_matkul,m.nama_matkul,m.sks,m.semester,p.nama_prodi FROM matkul m LEFT JOIN prodi p ON p.id_prodi=m.id_prodi ORDER BY m.id_matkul')->fetchAll();
    $headers=['ID MK','Mata Kuliah','SKS','Semester','Program Studi'];
}elseif($type==='jadwal'){
    $rows=$pdo->query('SELECT j.id_jadwal,m.nama_matkul,d.nama_dosen,j.hari,k.nama_kelas,j.jam_mulai,j.jam_selesai,r.nama_ruang
        FROM jadwal j LEFT JOIN matkul m ON m.id_matkul=j.id_matkul
        LEFT JOIN dosen d ON d.id_dosen=j.id_dosen
        LEFT JOIN kelas k ON k.id_kelas=j.kelas
        LEFT JOIN ruang r ON r.id_ruang=j.ruang
        ORDER BY FIELD(LOWER(j.hari),\'senin\',\'selasa\',\'rabu\',\'kamis\',\'jumat\',\'sabtu\',\'minggu\'),j.jam_mulai')->fetchAll();
    $headers=['ID Jadwal','Mata Kuliah','Dosen','Hari','Kelas','Jam Mulai','Jam Selesai','Ruang'];
}else{
    $rows=$pdo->query("SELECT {$meta['id']},{$meta['name']} FROM {$type} ORDER BY {$meta['id']}")->fetchAll();
    $headers=[$meta['id'],$meta['name']];
}
$days=['senin','selasa','rabu','kamis','jumat','sabtu','minggu'];
?>
<!doctype html><html lang="id"><head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Master Data — KRS</title>
<style>
*{box-sizing:border-box}body{margin:0;padding:18px;background:#d9ddd8;font-family:"Segoe UI",Arial,sans-serif;color:#303735}
.canvas{position:relative;width:min(1024px,100%);min-height:680px;margin:auto;background:#d9d5bb;box-shadow:0 12px 38px #24332f42;overflow:hidden}.bg{position:absolute;inset:0;width:100%;height:100%;object-fit:fill}.content{position:relative;padding:42px 5% 32px}
h1{margin:0 0 16px;color:#344641;font-size:30px}.nav{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:16px}.nav a,.back{display:inline-block;padding:8px 14px;background:#f7f8f4;border:1px solid #6a817a;color:#30544b;text-decoration:none;font-weight:700}.nav a.active,.primary{background:#0b7068;color:#fff;border-color:#0b7068}
.panel{background:#ffffffdf;border:1px solid #a4b8b0;padding:14px;margin-bottom:16px}.toolbar{display:flex;gap:10px;flex-wrap:wrap;align-items:center;margin-bottom:12px}
button,.button{display:inline-block;border:1px solid #6a817a;background:#f7f8f4;color:#30544b;padding:9px 14px;font:700 13px "Segoe UI",Arial,sans-serif;text-decoration:none;cursor:pointer}.primary{background:#0b7068;color:white;border-color:#0b7068}.danger{color:#98372e;border-color:#c58b84;background:#fff1ee}
.table-wrap{overflow:auto;background:#fff}table{width:100%;border-collapse:collapse;font-size:13px}th{background:#0b7068;color:#fff;text-align:left;padding:10px;white-space:nowrap}td{padding:9px;border-bottom:1px solid #dce5e1}tbody tr:nth-child(even){background:#f1f5f3}
.alert{padding:10px 12px;background:#eef8f1;border:1px solid #b8d6c3;color:#265c40;margin-bottom:13px;font-size:13px}.alert.error{background:#fff0ed;border-color:#dfb8ae;color:#8c3428}
.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px 16px}.field{display:flex;flex-direction:column;gap:5px}.field.full{grid-column:1/-1}
label{font-size:13px;font-weight:700;color:#38534c}input,select{min-height:39px;padding:8px 10px;border:1px solid #aebfba;background:white;color:#283a36;font:14px "Segoe UI",Arial,sans-serif}
.actions{display:flex;gap:9px;justify-content:flex-end;margin-top:16px}.hint{font-size:11px;color:#71817d}
@media(max-width:720px){body{padding:0}.canvas{min-height:100vh}.content{padding:22px 14px}.form-grid{grid-template-columns:1fr}.field.full{grid-column:auto}h1{font-size:23px}}
</style></head><body>
<main class="canvas"><img class="bg" src="https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Master%20Data.png" alt="">
<div class="content"><h1>Master Data</h1>
<nav class="nav"><?php foreach($entities as $key=>$item): ?><a class="<?=($type===$key?'active':'')?>" href="master-data.php?type=<?=krs_e($key)?>"><?=krs_e($item['label'])?></a><?php endforeach; ?><a class="back" href="index.php">Back</a></nav>
<?php if($success!==''): ?><div class="alert" role="status"><?=krs_e($success)?></div><?php endif; ?><?php if($error!==''): ?><div class="alert error" role="alert"><?=krs_e($error)?></div><?php endif; ?>
<section class="panel"><div class="toolbar"><strong><?=krs_e($meta['label'])?></strong><a class="button primary" href="master-data.php?type=<?=krs_e($type)?>&new=1">Add</a></div>
<div class="table-wrap"><table><thead><tr><?php foreach($headers as $h): ?><th><?=krs_e($h)?></th><?php endforeach; ?><th>Aksi</th></tr></thead><tbody>
<?php if(!$rows): ?><tr><td colspan="<?=count($headers)+1?>">Belum ada data.</td></tr><?php else: foreach($rows as $row): ?><tr>
<?php foreach($row as $v): ?><td><?=krs_e((string)($v??'-'))?></td><?php endforeach; ?>
<td><div class="toolbar"><a class="button" href="master-data.php?type=<?=krs_e($type)?>&edit=<?=krs_e((string)$row[$meta['id']])?>">Edit</a>
<form method="post" action="master-data.php?type=<?=krs_e($type)?>" onsubmit="return confirm('Hapus data ini?')"><input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token']??''))?>"><input type="hidden" name="type" value="<?=krs_e($type)?>"><input type="hidden" name="id" value="<?=krs_e((string)$row[$meta['id']])?>"><button class="danger" type="submit" name="action" value="delete">Delete</button></form></div></td>
</tr><?php endforeach; endif; ?></tbody></table></div></section>
<?php if($showForm): ?><section class="panel"><h2 style="margin:0 0 14px;color:#20675f"><?=($editId>0?'Edit ':'Tambah ')?><?=krs_e($meta['label'])?></h2>
<form method="post" action="master-data.php?type=<?=krs_e($type)?>"><input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token']??''))?>"><input type="hidden" name="type" value="<?=krs_e($type)?>"><input type="hidden" name="action" value="save">
<div class="form-grid">
<div class="field"><label for="id">ID (opsional)</label><input id="id" name="id" type="number" min="1" value="<?=krs_e((string)$values['id'])?>" <?=($editId>0?'readonly':'')?> placeholder="Kosongkan untuk ID otomatis"></div>
<?php if(in_array($type,['prodi','matkul','kelas','ruang'],true)): ?>
<div class="field"><label for="name"><?=($type==='matkul'?'Nama Mata Kuliah':($type==='prodi'?'Nama Program Studi':($type==='kelas'?'Nama Kelas':'Nama Ruang')))?></label><input id="name" name="name" maxlength="100" required value="<?=krs_e((string)$values['name'])?>"></div>
<?php endif; ?>
<?php if($type==='prodi'): ?>
<div class="field"><label for="kaprodi">Kaprodi</label><select id="kaprodi" name="kaprodi"><option value="">-- Belum ditetapkan --</option><?php foreach($kaprodi as $d): ?><option value="<?=krs_e((string)$d['id_dosen'])?>" <?=((string)$values['kaprodi']===(string)$d['id_dosen']?'selected':'')?>><?=krs_e((string)$d['nama_dosen'])?></option><?php endforeach; ?></select></div>
<?php elseif($type==='matkul'): ?>
<div class="field"><label for="program">Program Studi</label><select id="program" name="program" required><option value="">-- Pilih Prodi --</option><?php foreach($programs as $p): ?><option value="<?=krs_e((string)$p['id_prodi'])?>" <?=((string)$values['program']===(string)$p['id_prodi']?'selected':'')?>><?=krs_e((string)$p['nama_prodi'])?></option><?php endforeach; ?></select></div>
<div class="field"><label for="sks">SKS</label><input id="sks" name="sks" type="number" min="1" max="24" value="<?=krs_e((string)$values['sks'])?>" required></div>
<div class="field"><label for="semester">Semester</label><select id="semester" name="semester"><?php for($i=1;$i<=14;$i++): ?><option value="<?=$i?>" <?=((string)$values['semester']===(string)$i?'selected':'')?>><?=$i?></option><?php endfor; ?></select></div>
<?php elseif($type==='jadwal'): ?>
<div class="field"><label for="course">Mata Kuliah</label><select name="course" id="course" required><option value="">-- Pilih Mata Kuliah --</option><?php foreach($courses as $c): ?><option value="<?=krs_e((string)$c['id_matkul'])?>" <?=((string)$values['course']===(string)$c['id_matkul']?'selected':'')?>><?=krs_e((string)$c['nama_matkul'])?></option><?php endforeach; ?></select></div>
<div class="field"><label for="lecturer">Dosen Pengajar</label><select name="lecturer" id="lecturer" required><option value="">-- Pilih Dosen --</option><?php foreach($lecturers as $d): ?><option value="<?=krs_e((string)$d['id_dosen'])?>" <?=((string)$values['lecturer']===(string)$d['id_dosen']?'selected':'')?>><?=krs_e((string)$d['nama_dosen'])?></option><?php endforeach; ?></select></div>
<div class="field"><label for="day">Hari</label><select name="day" id="day"><?php foreach($days as $day): ?><option value="<?=$day?>" <?=($values['day']===$day?'selected':'')?>><?=ucfirst($day)?></option><?php endforeach; ?></select></div>
<div class="field"><label for="class">Kelas</label><select name="class" id="class" required><option value="">-- Pilih Kelas --</option><?php foreach($classes as $cl): ?><option value="<?=krs_e((string)$cl['id_kelas'])?>" <?=((string)$values['class']===(string)$cl['id_kelas']?'selected':'')?>><?=krs_e((string)$cl['nama_kelas'])?></option><?php endforeach; ?></select></div>
<div class="field"><label for="room">Ruang</label><select name="room" id="room" required><option value="">-- Pilih Ruang --</option><?php foreach($rooms as $r): ?><option value="<?=krs_e((string)$r['id_ruang'])?>" <?=((string)$values['room']===(string)$r['id_ruang']?'selected':'')?>><?=krs_e((string)$r['nama_ruang'])?></option><?php endforeach; ?></select></div>
<div class="field"><label for="start">Jam Mulai</label><input type="time" name="start" id="start" value="<?=krs_e((string)$values['start'])?>" required></div>
<div class="field"><label for="end">Jam Selesai</label><input type="time" name="end" id="end" value="<?=krs_e((string)$values['end'])?>" required></div>
<p class="hint field full">Jadwal yang bertabrakan pada dosen, kelas, atau ruang yang sama akan ditolak otomatis.</p>
<?php endif; ?>
</div><div class="actions"><a class="button" href="master-data.php?type=<?=krs_e($type)?>">Batal</a><button type="submit" class="primary">Save</button></div>
</form></section><?php endif; ?>
</div></main></body></html>
