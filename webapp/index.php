<?php
declare(strict_types=1);
require __DIR__ . '/config.php';

$error = '';
$account = $_SESSION['krs_user'] ?? null;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!hash_equals($_SESSION['csrf_token'] ?? '', (string)($_POST['csrf_token'] ?? ''))) {
        $error = 'Sesi formulir tidak valid. Muat ulang halaman dan coba lagi.';
    } elseif (isset($_POST['logout'])) {
        unset($_SESSION['krs_user']);
        session_regenerate_id(true);
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
        header('Location: index.php');
        exit;
    } else {
        $role = trim((string)($_POST['role'] ?? ''));
        $username = trim((string)($_POST['username'] ?? ''));
        $password = (string)($_POST['password'] ?? '');
        if (!in_array($role, ['Mahasiswa', 'Admin', 'Kaprodi', 'Dosen PA', 'Dosen'], true)) {
            $error = 'Silakan pilih jenis akun yang benar.';
        } elseif ($username === '' || $password === '') {
            $error = 'Username dan password tidak boleh kosong.';
        } else {
            try {
                $pdo = krs_db();
                $table = '';
                $idColumn = '';
                $nameColumn = '';
                $position = '';

                if ($role === 'Mahasiswa') {
                    $table = 'mahasiswa';
                    $idColumn = 'id_mahasiswa';
                    $nameColumn = 'nama_mahasiswa';
                } elseif ($role === 'Admin') {
                    $table = 'admin';
                    $idColumn = 'id_admin';
                    $nameColumn = 'nama_admin';
                } else {
                    $table = 'dosen';
                    $idColumn = 'id_dosen';
                    $nameColumn = 'nama_dosen';
                    $position = match ($role) {
                        'Dosen' => 'dosen',
                        'Dosen PA' => 'dosen pa',
                        'Kaprodi' => 'kaprodi',
                    };
                }

                $sql = "SELECT * FROM {$table} WHERE {$nameColumn} = ? LIMIT 1";
                $stmt = $pdo->prepare($sql);
                $stmt->execute([$username]);
                $row = $stmt->fetch();

                $roleMatches = true;
                if ($row && $table === 'dosen') {
                    $roleMatches = strtolower(trim((string)$row['posisi'])) === $position;
                }

                if (!$row || !$roleMatches || !krs_password_verify($password, (string)$row['password'])) {
                    $error = 'Username, password, atau jenis akun tidak sesuai.';
                } else {
                    $stored = (string)$row['password'];
                    if (!str_starts_with($stored, 'pbkdf2-sha256$')) {
                        $upgrade = $pdo->prepare("UPDATE {$table} SET password = ? WHERE {$idColumn} = ?");
                        $upgrade->execute([krs_password_hash($password), (int)$row[$idColumn]]);
                    }
                    session_regenerate_id(true);
                    $_SESSION['krs_user'] = [
                        'id' => (int)$row[$idColumn],
                        'name' => (string)$row[$nameColumn],
                        'role' => $role,
                        'position' => $position,
                    ];
                    $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
                    header('Location: index.php');
                    exit;
                }
            } catch (Throwable $exception) {
                error_log('KRS login error: ' . $exception->getMessage());
                $error = 'Koneksi database belum siap. Periksa konfigurasi hosting.';
            }
        }
    }
}

$account = $_SESSION['krs_user'] ?? null;
$counts = ['mahasiswa' => 0, 'dosen' => 0, 'matkul' => 0, 'jadwal' => 0, 'pengajuan' => 0];
if ($account) {
    try {
        $pdo = krs_db();
        foreach (array_keys($counts) as $table) {
            $counts[$table] = (int)$pdo->query("SELECT COUNT(*) FROM {$table}")->fetchColumn();
        }
    } catch (Throwable $exception) {
        error_log('KRS dashboard error: ' . $exception->getMessage());
    }
}

$features = match ($account['role'] ?? '') {
    'Mahasiswa' => ['Edit Profile', 'Data KRS'],
    'Dosen' => ['Edit Profile', 'Jadwal Ajar'],
    'Dosen PA' => ['Edit Profile', 'Jadwal Ajar', 'Mahasiswa PA', 'Data KRS'],
    'Kaprodi' => ['Edit Profile', 'Jadwal Ajar', 'Mahasiswa PA', 'Data KRS', 'Finalisasi KRS'],
    'Admin' => ['Edit Profile', 'Data KRS', 'Master User', 'Master Data', 'Riwayat KRS'],
    default => [],
};
?>
<!doctype html>
<html lang="id">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>KRS — Sistem Informasi Akademik</title>
<style>
*{box-sizing:border-box}body{margin:0;background:#e7e9eb;color:#1f2e2c;font:15px/1.45 "Segoe UI",Arial,sans-serif;min-height:100vh;display:grid;place-items:center;padding:14px}
button,input,select{font:inherit}.screen{width:min(1024px,100%);height:min(600px,calc(100vh - 28px));min-height:520px;position:relative;background:#fff;box-shadow:0 15px 45px #182d2a30;overflow:hidden}
.login-bg{position:absolute;inset:0;width:100%;height:100%;object-fit:fill;user-select:none}
.login-form{position:absolute;inset:0}
.login-form select,.login-form input{position:absolute;left:34.18%;width:34.18%;height:6.67%;border:1px solid #b7c3c8;background:#fff;padding:0 12px;color:#23383b;font-size:16px;outline-color:#147d77}
.login-form select{top:36.67%}.login-form input.username{top:48.33%}.login-form input.password{top:60%}
.login-form .show{position:absolute;left:69.5%;top:61%;width:28px;height:28px;accent-color:#087d73;cursor:pointer}
.login-form button.submit{position:absolute;left:39.06%;top:70%;width:22.46%;height:6.67%;border:0;background:#08796e;color:#fff;font-weight:700;font-size:18px;cursor:pointer}
.login-form button.submit:hover{background:#00665d}
.login-form .signup{position:absolute;right:30.6%;bottom:10.5%;border:0;background:transparent;color:#1468b1;text-decoration:underline;cursor:pointer;font-size:14px}
.error{position:absolute;left:34.18%;top:13%;width:49%;background:#fff0f0;border:1px solid #e6b5b5;color:#982d2d;padding:9px 12px;font-size:13px;z-index:2}
.dashboard{height:100%;padding:22px 25px;background:linear-gradient(135deg,#f4f7f7,#fff)}
.top{display:flex;justify-content:space-between;align-items:center;border-bottom:5px solid #08796e;padding:0 0 13px;gap:14px}.top h1{margin:0;color:#08796e;font-size:25px}.top form{margin:0}.btn{border:0;background:#08796e;color:white;padding:9px 14px;cursor:pointer}.welcome{margin:20px 0 14px}.tiles{display:grid;grid-template-columns:repeat(5,1fr);gap:12px}.tile{border:1px solid #d7e0df;border-top:4px solid #08796e;background:white;padding:14px}.tile small{display:block;color:#677a78}.tile strong{display:block;font-size:25px;margin-top:7px}.panel-title{margin:24px 0 10px;color:#08796e;font-size:18px}.features{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}.feature-link,.feature-disabled{display:flex;flex-direction:column;justify-content:center;gap:3px;min-height:56px;padding:10px 15px;text-decoration:none}.feature-link{background:#08796e;color:#fff}.feature-link:hover{background:#f0a72d;color:#243c3a}.feature-disabled{background:#edf2f1;color:#627470;border:1px dashed #cad7d4}.feature-disabled small{font-size:11px;color:#71817f}.footnote{color:#70817f;font-size:12px;margin-top:18px}
@media(max-width:720px){body{padding:0}.screen{height:auto;min-height:100vh;width:100%;overflow:auto}.login-bg{object-fit:cover}.login-form select,.login-form input{left:9%;width:82%;height:44px}.login-form select{top:36%}.login-form input.username{top:48%}.login-form input.password{top:60%}.login-form .show{left:auto;right:10%;top:61%}.login-form button.submit{left:20%;top:71%;width:60%;height:46px}.login-form .signup{right:15%;bottom:9%}.error{left:8%;top:10%;width:84%}.dashboard{padding:18px}.tiles{grid-template-columns:repeat(2,1fr)}.features{grid-template-columns:1fr}.top h1{font-size:19px}}
</style>
</head><body>
<div class="screen">
<?php if (!$account): ?>
<img class="login-bg" src="https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Login%20(2).png" alt="">
<?php if ($error !== ''): ?><div class="error"><?=krs_e($error)?></div><?php endif; ?>
<form class="login-form" method="post" autocomplete="on">
<input type="hidden" name="csrf_token" value="<?=krs_e($_SESSION['csrf_token'])?>">
<select name="role" aria-label="Jenis akun">
<option value="Mahasiswa">Mahasiswa</option><option value="Admin">Admin</option><option value="Kaprodi">Kaprodi</option><option value="Dosen PA">Dosen PA</option><option value="Dosen">Dosen</option>
</select>
<input class="username" type="text" name="username" placeholder="Username" autocomplete="username" required>
<input class="password" id="password" type="password" name="password" placeholder="Password" autocomplete="current-password" required>
<input class="show" type="checkbox" aria-label="Tampilkan password" onchange="document.getElementById('password').type=this.checked?'text':'password'">
<button class="submit" type="submit">Login</button>
<button class="signup" type="button" onclick="alert('Pendaftaran akun mengikuti proses pada aplikasi asli dan belum dibuka pada portal demo.')">Sign Up</button>
</form>
<?php else: ?>
<div class="dashboard">
<div class="top"><h1>Sistem Informasi KRS</h1><form method="post"><input type="hidden" name="csrf_token" value="<?=krs_e($_SESSION['csrf_token'])?>"><button class="btn" name="logout" value="1">Log Out</button></form></div>
<p class="welcome">Selamat datang, <b><?=krs_e($account['name'])?></b> — <?=krs_e($account['role'])?><?php if ($account['position'] !== ''): ?> (<?=krs_e($account['position'])?>)<?php endif; ?></p>
<div class="tiles">
<div class="tile"><small>Mahasiswa</small><strong><?=$counts['mahasiswa']?></strong></div>
<div class="tile"><small>Dosen</small><strong><?=$counts['dosen']?></strong></div>
<div class="tile"><small>Mata Kuliah</small><strong><?=$counts['matkul']?></strong></div>
<div class="tile"><small>Jadwal</small><strong><?=$counts['jadwal']?></strong></div>
<div class="tile"><small>Pengajuan KRS</small><strong><?=$counts['pengajuan']?></strong></div>
</div>
<h2 class="panel-title">Pilih Fitur yang Ingin Digunakan:</h2>
<div class="features">
<?php foreach ($features as $feature): ?>
    <?php if ($feature === 'Edit Profile'): ?>
        <a class="feature-link" href="profile.php"><?=krs_e($feature)?></a>
    <?php elseif ($feature === 'Data KRS' && ($account['role'] ?? '') === 'Admin'): ?>
        <a class="feature-link" href="krs.php"><?=krs_e($feature)?></a>
    <?php elseif ($feature === 'Data KRS' && ($account['role'] ?? '') === 'Mahasiswa'): ?>
        <a class="feature-link" href="pengajuan.php"><?=krs_e($feature)?></a>
    <?php elseif ($feature === 'Data KRS' && in_array(($account['role'] ?? ''), ['Dosen PA', 'Kaprodi'], true)): ?>
        <a class="feature-link" href="review.php"><?=krs_e($feature)?></a>
    <?php else: ?>
        <div class="feature-disabled" aria-disabled="true">
            <span><?=krs_e($feature)?></span>
            <small>Belum tersedia di versi web</small>
        </div>
    <?php endif; ?>
<?php endforeach; ?>
</div>
<p class="footnote">Edit Profile, manajemen sesi Admin, pengajuan Mahasiswa, dan review KRS Dosen PA sudah tersedia. Finalisasi Kaprodi dan laporan masih dalam tahap pemindahan.</p>
</div>
<?php endif; ?>
</div></body></html>
