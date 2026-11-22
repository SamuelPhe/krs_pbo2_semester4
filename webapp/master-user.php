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
    exit('Akses ditolak. Master User hanya untuk Admin.');
}

$pdo = krs_db();
$types = [
    'mahasiswa' => ['label' => 'Data Mahasiswa', 'table' => 'mahasiswa', 'id' => 'id_mahasiswa', 'name' => 'nama_mahasiswa'],
    'dosen' => ['label' => 'Data Dosen', 'table' => 'dosen', 'id' => 'id_dosen', 'name' => 'nama_dosen'],
    'admin' => ['label' => 'Data Admin', 'table' => 'admin', 'id' => 'id_admin', 'name' => 'nama_admin'],
];
$type = (string)($_GET['type'] ?? $_POST['type'] ?? 'mahasiswa');
if (!isset($types[$type])) {
    $type = 'mahasiswa';
}
$meta = $types[$type];
$editId = filter_input(INPUT_GET, 'edit', FILTER_VALIDATE_INT) ?: 0;
$showForm = isset($_GET['new']) || $editId > 0;
$error = '';
$success = (string)($_SESSION['master_user_flash'] ?? '');
unset($_SESSION['master_user_flash']);

$programs = $pdo->query('SELECT id_prodi, nama_prodi FROM prodi ORDER BY nama_prodi')->fetchAll();
$lecturers = $pdo->query('SELECT id_dosen, nama_dosen, posisi FROM dosen ORDER BY nama_dosen')->fetchAll();
$values = [
    'id' => '', 'name' => '', 'position' => 'dosen', 'program' => '',
    'advisor' => '', 'cohort' => (string)date('Y'), 'semester' => '1',
];

if ($editId > 0) {
    $query = $pdo->prepare("SELECT * FROM {$meta['table']} WHERE {$meta['id']} = ? LIMIT 1");
    $query->execute([$editId]);
    $row = $query->fetch();
    if (!$row) {
        $editId = 0;
        $showForm = false;
        $error = 'Data akun yang dipilih tidak ditemukan.';
    } else {
        $values['id'] = (string)$row[$meta['id']];
        $values['name'] = (string)$row[$meta['name']];
        if ($type === 'dosen') {
            $values['position'] = (string)$row['posisi'];
        } elseif ($type === 'mahasiswa') {
            $values['program'] = (string)$row['id_prodi'];
            $values['advisor'] = (string)($row['id_dosen_pa'] ?? '');
            $values['cohort'] = (string)$row['angkatan'];
            $values['semester'] = (string)$row['semester'];
        }
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $token = (string)($_SESSION['csrf_token'] ?? '');
    $sent = (string)($_POST['csrf_token'] ?? '');
    if ($token === '' || !hash_equals($token, $sent)) {
        http_response_code(403);
        $error = 'Sesi formulir tidak valid. Muat ulang halaman dan coba lagi.';
    } else {
        $action = (string)($_POST['action'] ?? '');
        $id = filter_var($_POST['id'] ?? '', FILTER_VALIDATE_INT);
        $id = ($id === false || $id === null || $id < 1) ? 0 : (int)$id;
        try {
            if ($action === 'delete') {
                if ($id < 1) {
                    $error = 'ID akun tidak valid.';
                } elseif ($type === 'admin' && $id === (int)$account['id']) {
                    $error = 'Akun Admin yang sedang digunakan tidak boleh dihapus.';
                } else {
                    $pdo->beginTransaction();
                    $guardMessage = '';
                    if ($type === 'mahasiswa') {
                        $check = $pdo->prepare('SELECT COUNT(*) FROM pengajuan_krs WHERE id_mahasiswa = ?');
                        $check->execute([$id]);
                        if ((int)$check->fetchColumn() > 0) {
                            $guardMessage = 'Mahasiswa tidak dapat dihapus karena sudah memiliki riwayat pengajuan KRS.';
                        }
                    } elseif ($type === 'dosen') {
                        $checks = [
                            ['SELECT COUNT(*) FROM mahasiswa WHERE id_dosen_pa = ?', 'Dosen masih ditugaskan sebagai Dosen PA.'],
                            ['SELECT COUNT(*) FROM prodi WHERE id_kaprodi = ?', 'Dosen masih ditetapkan sebagai Kaprodi.'],
                            ['SELECT COUNT(*) FROM jadwal WHERE id_dosen = ?', 'Dosen masih memiliki jadwal mengajar.'],
                            ['SELECT COUNT(*) FROM pengajuan_krs WHERE id_dosen_acc = ?', 'Dosen tercatat sebagai pemeriksa pengajuan KRS.'],
                        ];
                        foreach ($checks as [$sql, $message]) {
                            $check = $pdo->prepare($sql);
                            $check->execute([$id]);
                            if ((int)$check->fetchColumn() > 0) {
                                $guardMessage = $message;
                                break;
                            }
                        }
                    }
                    if ($guardMessage !== '') {
                        $pdo->rollBack();
                        $error = $guardMessage;
                    } else {
                        $delete = $pdo->prepare("DELETE FROM {$meta['table']} WHERE {$meta['id']} = ?");
                        $delete->execute([$id]);
                        $deleted = $delete->rowCount();
                        $pdo->commit();
                        if ($deleted > 0) {
                            $_SESSION['master_user_flash'] = 'Data akun berhasil dihapus.';
                            header('Location: master-user.php?type=' . rawurlencode($type), true, 303);
                            exit;
                        }
                        $error = 'Data akun tidak ditemukan.';
                    }
                }
            } elseif ($action === 'save') {
                $values = [
                    'id' => trim((string)($_POST['id'] ?? '')),
                    'name' => trim((string)($_POST['name'] ?? '')),
                    'position' => trim((string)($_POST['position'] ?? 'dosen')),
                    'program' => trim((string)($_POST['program'] ?? '')),
                    'advisor' => trim((string)($_POST['advisor'] ?? '')),
                    'cohort' => trim((string)($_POST['cohort'] ?? '')),
                    'semester' => trim((string)($_POST['semester'] ?? '')),
                ];
                $password = (string)($_POST['password'] ?? '');
                $idValue = $values['id'] === '' ? null : filter_var($values['id'], FILTER_VALIDATE_INT);
                $programId = filter_var($values['program'], FILTER_VALIDATE_INT);
                $advisorId = $values['advisor'] === '' ? null : filter_var($values['advisor'], FILTER_VALIDATE_INT);
                $cohort = filter_var($values['cohort'], FILTER_VALIDATE_INT);
                $semester = filter_var($values['semester'], FILTER_VALIDATE_INT);
                if (strlen($values['name']) < 2 || strlen($values['name']) > 200) {
                    $error = 'Nama harus terdiri dari 2 sampai 50 karakter.';
                } elseif ($idValue === false || ($idValue !== null && $idValue < 1)) {
                    $error = 'ID akun harus berupa bilangan bulat positif.';
                } elseif ($editId > 0 && (int)$idValue !== $editId) {
                    $error = 'ID utama akun tidak dapat diubah.';
                } elseif ($editId === 0 && $idValue !== null) {
                    $exists = $pdo->prepare("SELECT COUNT(*) FROM {$meta['table']} WHERE {$meta['id']} = ?");
                    $exists->execute([(int)$idValue]);
                    if ((int)$exists->fetchColumn() > 0) {
                        $error = 'ID tersebut sudah digunakan.';
                    }
                }
                if ($error === '' && $editId === 0 && $password === '') {
                    $error = 'Password wajib diisi untuk akun baru.';
                } elseif ($error === '' && $password !== '' && (strlen($password) < 8 || strlen($password) > 1024)) {
                    $error = 'Password baru harus memiliki panjang 8 sampai 1024 byte.';
                }
                if ($error === '' && $type === 'dosen' && !in_array($values['position'], ['dosen', 'dosen pa', 'kaprodi'], true)) {
                    $error = 'Posisi dosen tidak valid.';
                }
                if ($error === '' && $type === 'mahasiswa') {
                    if ($programId === false || $programId < 1) {
                        $error = 'Pilih program studi yang valid.';
                    } elseif ($values['advisor'] !== '' && ($advisorId === false || $advisorId < 1)) {
                        $error = 'Pilih Dosen PA yang valid.';
                    } elseif ($cohort === false || $cohort < 2000 || $cohort > 2099) {
                        $error = 'Angkatan harus berupa tahun empat digit.';
                    } elseif ($semester === false || $semester < 1 || $semester > 14) {
                        $error = 'Semester harus berada pada rentang 1 sampai 14.';
                    } else {
                        $check = $pdo->prepare('SELECT COUNT(*) FROM prodi WHERE id_prodi = ?');
                        $check->execute([$programId]);
                        if ((int)$check->fetchColumn() !== 1) {
                            $error = 'Program studi tidak ditemukan.';
                        }
                        if ($error === '' && $advisorId !== null) {
                            $check = $pdo->prepare('SELECT COUNT(*) FROM dosen WHERE id_dosen = ?');
                            $check->execute([$advisorId]);
                            if ((int)$check->fetchColumn() !== 1) {
                                $error = 'Dosen yang dipilih tidak ditemukan.';
                            }
                        }
                    }
                }

                if ($error === '') {
                    $passwordHash = $password !== '' ? krs_password_hash($password) : null;
                    if ($type === 'admin') {
                        if ($editId > 0) {
                            $sql = 'UPDATE admin SET nama_admin = ?' . ($passwordHash !== null ? ', password = ?' : '') . ' WHERE id_admin = ?';
                            $params = [$values['name']];
                            if ($passwordHash !== null) $params[] = $passwordHash;
                            $params[] = $editId;
                        } else {
                            $sql = 'INSERT INTO admin (nama_admin, password' . ($idValue !== null ? ', id_admin' : '') . ') VALUES (?, ?' . ($idValue !== null ? ', ?' : '') . ')';
                            $params = [$values['name'], $passwordHash];
                            if ($idValue !== null) $params[] = (int)$idValue;
                        }
                    } elseif ($type === 'dosen') {
                        if ($editId > 0) {
                            $sql = 'UPDATE dosen SET nama_dosen = ?, posisi = ?' . ($passwordHash !== null ? ', password = ?' : '') . ' WHERE id_dosen = ?';
                            $params = [$values['name'], $values['position']];
                            if ($passwordHash !== null) $params[] = $passwordHash;
                            $params[] = $editId;
                        } else {
                            $sql = 'INSERT INTO dosen (nama_dosen, posisi, password' . ($idValue !== null ? ', id_dosen' : '') . ') VALUES (?, ?, ?' . ($idValue !== null ? ', ?' : '') . ')';
                            $params = [$values['name'], $values['position'], $passwordHash];
                            if ($idValue !== null) $params[] = (int)$idValue;
                        }
                    } else {
                        if ($editId > 0) {
                            $sql = 'UPDATE mahasiswa SET nama_mahasiswa = ?, id_prodi = ?, id_dosen_pa = ?, angkatan = ?, semester = ?' . ($passwordHash !== null ? ', password = ?' : '') . ' WHERE id_mahasiswa = ?';
                            $params = [$values['name'], $programId, $advisorId, $cohort, $semester];
                            if ($passwordHash !== null) $params[] = $passwordHash;
                            $params[] = $editId;
                        } else {
                            $sql = 'INSERT INTO mahasiswa (nama_mahasiswa, id_prodi, id_dosen_pa, angkatan, semester, password' . ($idValue !== null ? ', id_mahasiswa' : '') . ') VALUES (?, ?, ?, ?, ?, ?' . ($idValue !== null ? ', ?' : '') . ')';
                            $params = [$values['name'], $programId, $advisorId, $cohort, $semester, $passwordHash];
                            if ($idValue !== null) $params[] = (int)$idValue;
                        }
                    }
                    $save = $pdo->prepare($sql);
                    $save->execute($params);
                    $_SESSION['master_user_flash'] = $editId > 0 ? 'Data akun berhasil diperbarui.' : 'Akun baru berhasil ditambahkan.';
                    header('Location: master-user.php?type=' . rawurlencode($type), true, 303);
                    exit;
                }
                $editId = (int)($idValue ?: 0);
                $showForm = true;
            } else {
                $error = 'Aksi Master User tidak dikenal.';
            }
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            error_log('KRS master user error: ' . $exception->getMessage());
            $error = 'Data akun gagal disimpan. Pastikan nama/ID belum digunakan dan relasi data valid.';
            if ($action === 'save') $showForm = true;
        }
    }
}

if ($type === 'mahasiswa') {
    $rows = $pdo->query(
        'SELECT m.id_mahasiswa, m.nama_mahasiswa, p.nama_prodi, d.nama_dosen,
                m.angkatan, m.semester
         FROM mahasiswa m LEFT JOIN prodi p ON p.id_prodi = m.id_prodi
         LEFT JOIN dosen d ON d.id_dosen = m.id_dosen_pa
         ORDER BY m.id_mahasiswa'
    )->fetchAll();
    $headers = ['ID Mahasiswa', 'Nama', 'Program Studi', 'Dosen PA', 'Angkatan', 'Semester'];
} elseif ($type === 'dosen') {
    $rows = $pdo->query('SELECT id_dosen, nama_dosen, posisi FROM dosen ORDER BY id_dosen')->fetchAll();
    $headers = ['ID Dosen', 'Nama Dosen', 'Posisi'];
} else {
    $rows = $pdo->query('SELECT id_admin, nama_admin FROM admin ORDER BY id_admin')->fetchAll();
    $headers = ['ID Admin', 'Nama Admin'];
}
$editLabel = $editId > 0 ? 'Edit' : 'Tambah';
?>
<!doctype html>
<html lang="id"><head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Master User — KRS</title>
<style>
*{box-sizing:border-box}body{margin:0;padding:18px;background:#d9ddd8;font-family:"Segoe UI",Arial,sans-serif;color:#303735}
.canvas{position:relative;width:min(1024px,100%);min-height:680px;margin:auto;background:#d9d5bb;box-shadow:0 12px 38px #24332f42;overflow:hidden}
.bg{position:absolute;inset:0;width:100%;height:100%;object-fit:fill}.content{position:relative;padding:42px 5% 32px}
h1{margin:0 0 16px;color:#344641;font-size:30px}.nav{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:16px}
.nav a,.back{display:inline-block;padding:8px 14px;background:#f7f8f4;border:1px solid #6a817a;color:#30544b;text-decoration:none;font-weight:700}
.nav a.active,.primary{background:#0b7068;color:#fff;border-color:#0b7068}
.panel{background:#ffffffdf;border:1px solid #a4b8b0;padding:14px;margin-bottom:16px}
.toolbar{display:flex;gap:10px;flex-wrap:wrap;align-items:center;margin-bottom:12px}
button,.button{display:inline-block;border:1px solid #6a817a;background:#f7f8f4;color:#30544b;padding:9px 14px;font:700 13px "Segoe UI",Arial,sans-serif;text-decoration:none;cursor:pointer}
button.primary{background:#0b7068;color:white;border-color:#0b7068}.danger{color:#98372e;border-color:#c58b84;background:#fff1ee}
.table-wrap{overflow:auto;background:#fff}table{width:100%;border-collapse:collapse;font-size:13px}th{background:#0b7068;color:#fff;text-align:left;padding:10px;white-space:nowrap}td{padding:9px;border-bottom:1px solid #dce5e1}tbody tr:nth-child(even){background:#f1f5f3}
.alert{padding:10px 12px;background:#eef8f1;border:1px solid #b8d6c3;color:#265c40;margin-bottom:13px;font-size:13px}.alert.error{background:#fff0ed;border-color:#dfb8ae;color:#8c3428}
.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px 16px}
.field{display:flex;flex-direction:column;gap:5px}.field.full{grid-column:1/-1}label{font-size:13px;font-weight:700;color:#38534c}
input,select{min-height:39px;padding:8px 10px;border:1px solid #aebfba;background:white;color:#283a36;font:14px "Segoe UI",Arial,sans-serif}
.hint{font-size:11px;color:#73817d;margin:0}.actions{display:flex;gap:9px;justify-content:flex-end;margin-top:16px}
@media(max-width:720px){body{padding:0}.canvas{min-height:100vh}.content{padding:22px 14px}.form-grid{grid-template-columns:1fr}.field.full{grid-column:auto}h1{font-size:23px}}
</style></head><body>
<main class="canvas">
<img class="bg" src="https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Master%20User.png" alt="">
<div class="content">
<h1>Master User</h1>
<nav class="nav">
<?php foreach ($types as $key => $item): ?><a class="<?=($type === $key ? 'active' : '')?>" href="master-user.php?type=<?=krs_e($key)?>"><?=krs_e($item['label'])?></a><?php endforeach; ?>
<a class="back" href="index.php">Back</a>
</nav>
<?php if ($success !== ''): ?><div class="alert" role="status"><?=krs_e($success)?></div><?php endif; ?>
<?php if ($error !== ''): ?><div class="alert error" role="alert"><?=krs_e($error)?></div><?php endif; ?>
<section class="panel">
<div class="toolbar"><strong><?=krs_e($meta['label'])?></strong><a class="button primary" href="master-user.php?type=<?=krs_e($type)?>&new=1">Add</a></div>
<div class="table-wrap"><table>
<thead><tr><?php foreach ($headers as $header): ?><th><?=krs_e($header)?></th><?php endforeach; ?><th>Aksi</th></tr></thead>
<tbody>
<?php if (!$rows): ?><tr><td colspan="<?=count($headers)+1?>">Belum ada akun.</td></tr>
<?php else: foreach ($rows as $row): ?><tr>
<?php foreach ($row as $value): ?><td><?=krs_e((string)($value ?? '-'))?></td><?php endforeach; ?>
<td><div class="toolbar">
<a class="button" href="master-user.php?type=<?=krs_e($type)?>&edit=<?=krs_e((string)$row[$meta['id']])?>">Edit</a>
<form method="post" action="master-user.php?type=<?=krs_e($type)?>" onsubmit="return confirm('Hapus akun ini?')">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="type" value="<?=krs_e($type)?>"><input type="hidden" name="id" value="<?=krs_e((string)$row[$meta['id']])?>">
<button class="danger" name="action" value="delete" type="submit">Delete</button>
</form></div></td>
</tr><?php endforeach; endif; ?>
</tbody></table></div>
</section>

<?php if ($showForm): ?>
<section class="panel">
<h2 style="margin:0 0 14px;color:#20675f"><?=krs_e($editLabel)?> <?=krs_e($meta['label'])?></h2>
<form method="post" action="master-user.php?type=<?=krs_e($type)?>">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="type" value="<?=krs_e($type)?>"><input type="hidden" name="action" value="save">
<div class="form-grid">
<div class="field"><label for="id">ID Akun (opsional)</label><input id="id" name="id" type="number" min="1" value="<?=krs_e((string)$values['id'])?>" <?=($editId > 0 ? 'readonly' : '')?> placeholder="Kosongkan untuk ID otomatis"><p class="hint">ID utama dapat dibuat otomatis oleh database.</p></div>
<div class="field"><label for="name">Nama / Username</label><input id="name" name="name" maxlength="50" required value="<?=krs_e((string)$values['name'])?>"></div>
<?php if ($type === 'dosen'): ?>
<div class="field"><label for="position">Posisi</label><select id="position" name="position"><option value="dosen" <?=($values['position']==='dosen'?'selected':'')?>>Dosen</option><option value="dosen pa" <?=($values['position']==='dosen pa'?'selected':'')?>>Dosen PA</option><option value="kaprodi" <?=($values['position']==='kaprodi'?'selected':'')?>>Kaprodi</option></select></div>
<?php elseif ($type === 'mahasiswa'): ?>
<div class="field"><label for="program">Program Studi</label><select id="program" name="program" required><option value="">-- Pilih Prodi --</option><?php foreach($programs as $program): ?><option value="<?=krs_e((string)$program['id_prodi'])?>" <?=((string)$values['program']===(string)$program['id_prodi']?'selected':'')?>><?=krs_e((string)$program['nama_prodi'])?></option><?php endforeach; ?></select></div>
<div class="field"><label for="advisor">Dosen Pembimbing Akademik</label><select id="advisor" name="advisor"><option value="">-- Belum ditetapkan --</option><?php foreach($lecturers as $lecturer): ?><option value="<?=krs_e((string)$lecturer['id_dosen'])?>" <?=((string)$values['advisor']===(string)$lecturer['id_dosen']?'selected':'')?>><?=krs_e((string)$lecturer['nama_dosen'])?> (<?=krs_e((string)$lecturer['posisi'])?>)</option><?php endforeach; ?></select></div>
<div class="field"><label for="cohort">Angkatan</label><input id="cohort" name="cohort" type="number" min="2000" max="2099" value="<?=krs_e((string)$values['cohort'])?>" required></div>
<div class="field"><label for="semester">Semester</label><select id="semester" name="semester"><?php for($i=1;$i<=14;$i++): ?><option value="<?=$i?>" <?=((string)$values['semester']===(string)$i?'selected':'')?>><?=$i?></option><?php endfor; ?></select></div>
<?php endif; ?>
<div class="field <?=($type==='mahasiswa'?'full':'')?>"><label for="password">Password <?=($editId>0?'(kosongkan jika tidak diubah)':'')?></label><input id="password" name="password" type="password" minlength="8" maxlength="1024" autocomplete="new-password" <?=($editId===0?'required':'')?>><p class="hint">Password baru disimpan menggunakan PBKDF2-SHA256, kompatibel dengan login web.</p></div>
</div>
<div class="actions"><a class="button" href="master-user.php?type=<?=krs_e($type)?>">Batal</a><button class="primary" type="submit">Save</button></div>
</form>
</section>
<?php endif; ?>
</div></main></body></html>
