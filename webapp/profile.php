<?php
declare(strict_types=1);

require __DIR__ . '/config.php';

$account = $_SESSION['krs_user'] ?? null;
if (!is_array($account)) {
    header('Location: index.php?notice=login-required', true, 302);
    exit;
}

$role = (string)($account['role'] ?? '');
$profiles = [
    'Mahasiswa' => ['table' => 'mahasiswa', 'id' => 'id_mahasiswa', 'name' => 'nama_mahasiswa', 'extras' => ['angkatan', 'semester'], 'position' => null],
    'Admin' => ['table' => 'admin', 'id' => 'id_admin', 'name' => 'nama_admin', 'extras' => [], 'position' => null],
    'Dosen' => ['table' => 'dosen', 'id' => 'id_dosen', 'name' => 'nama_dosen', 'extras' => ['posisi'], 'position' => 'dosen'],
    'Dosen PA' => ['table' => 'dosen', 'id' => 'id_dosen', 'name' => 'nama_dosen', 'extras' => ['posisi'], 'position' => 'dosen pa'],
    'Kaprodi' => ['table' => 'dosen', 'id' => 'id_dosen', 'name' => 'nama_dosen', 'extras' => ['posisi'], 'position' => 'kaprodi'],
];

$definition = $profiles[$role] ?? null;
if ($definition === null || (int)($account['id'] ?? 0) < 1) {
    unset($_SESSION['krs_user']);
    session_regenerate_id(true);
    header('Location: index.php?notice=login-required', true, 302);
    exit;
}

$error = '';
$flash = (string)($_SESSION['krs_profile_flash'] ?? '');
unset($_SESSION['krs_profile_flash']);
$profile = null;

try {
    $pdo = krs_db();
    $select = [$definition['id'] . ' AS account_id', $definition['name'] . ' AS display_name', 'password'];
    foreach ($definition['extras'] as $extra) {
        $select[] = $extra;
    }

    $sql = 'SELECT ' . implode(', ', $select) . ' FROM ' . $definition['table']
        . ' WHERE ' . $definition['id'] . ' = ? LIMIT 1';
    $query = $pdo->prepare($sql);
    $query->execute([(int)$account['id']]);
    $profile = $query->fetch();

    if (!$profile || ($definition['position'] !== null
        && strtolower(trim((string)$profile['posisi'])) !== $definition['position'])) {
        unset($_SESSION['krs_user']);
        session_regenerate_id(true);
        header('Location: index.php?notice=login-required', true, 302);
        exit;
    }

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $knownToken = (string)($_SESSION['csrf_token'] ?? '');
        $sentToken = (string)($_POST['csrf_token'] ?? '');
        if ($knownToken === '' || !hash_equals($knownToken, $sentToken)) {
            http_response_code(403);
            $error = 'Sesi formulir tidak valid. Muat ulang halaman dan coba lagi.';
        } else {
            $newName = trim((string)($_POST['name'] ?? ''));
            $newPassword = (string)($_POST['new_password'] ?? '');
            $confirmation = (string)($_POST['confirm_password'] ?? '');
            preg_match_all('/./us', $newName, $characters);
            $nameLength = count($characters[0] ?? []);

            if ($newName === '' || $nameLength < 1 || $nameLength > 50) {
                $error = 'Nama wajib diisi dan tidak boleh lebih dari 50 karakter.';
            } elseif (($newPassword === '') !== ($confirmation === '')) {
                $error = 'Isi kedua kolom password untuk mengganti password, atau biarkan keduanya kosong.';
            } elseif ($newPassword !== '' && !hash_equals($newPassword, $confirmation)) {
                $error = 'Konfirmasi password tidak cocok.';
            } elseif ($newPassword !== '' && (strlen($newPassword) < 8 || strlen($newPassword) > 1024)) {
                $error = 'Password baru harus terdiri dari 8 sampai 1024 karakter.';
            } else {
                $passwordChanged = $newPassword !== '';
                $nameChanged = $newName !== (string)$profile['display_name'];

                if (!$passwordChanged && !$nameChanged) {
                    $_SESSION['krs_profile_flash'] = 'Tidak ada perubahan yang perlu disimpan.';
                    header('Location: profile.php', true, 303);
                    exit;
                }

                $sets = [$definition['name'] . ' = ?'];
                $values = [$newName];
                if ($passwordChanged) {
                    $sets[] = 'password = ?';
                    $values[] = krs_password_hash($newPassword);
                }
                $values[] = (int)$account['id'];

                $update = $pdo->prepare(
                    'UPDATE ' . $definition['table'] . ' SET ' . implode(', ', $sets)
                    . ' WHERE ' . $definition['id'] . ' = ?'
                );
                $update->execute($values);

                $_SESSION['krs_user']['name'] = $newName;
                session_regenerate_id(true);
                $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
                $_SESSION['krs_profile_flash'] = $passwordChanged
                    ? 'Profil dan password berhasil diperbarui.'
                    : 'Profil berhasil diperbarui.';
                header('Location: profile.php', true, 303);
                exit;
            }
        }
    }
} catch (PDOException $exception) {
    error_log('KRS profile database error: ' . $exception->getMessage());
    if ((string)$exception->getCode() === '23000') {
        $error = 'Nama tersebut sudah digunakan oleh akun lain.';
    } else {
        http_response_code(500);
        $error = 'Profil tidak dapat diproses karena terjadi kendala database.';
    }
} catch (Throwable $exception) {
    error_log('KRS profile error: ' . $exception->getMessage());
    http_response_code(500);
    $error = 'Profil tidak dapat dimuat saat ini. Periksa koneksi database.';
}

if (!is_array($profile)) {
    $profile = ['account_id' => (int)$account['id'], 'display_name' => (string)($account['name'] ?? '')];
}
$labels = ['angkatan' => 'Angkatan', 'semester' => 'Semester', 'posisi' => 'Posisi / Jabatan'];
?>
<!doctype html>
<html lang="id">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Edit Profile — KRS</title>
<style>
*{box-sizing:border-box}body{margin:0;min-height:100vh;padding:28px;background:#e7e9eb;color:#1f2e2c;font:15px/1.5 "Segoe UI",Arial,sans-serif;display:grid;place-items:center}
button,input{font:inherit}.page{width:min(760px,100%);background:#fff;box-shadow:0 15px 45px #182d2a25;border:1px solid #dce4e3}
.header{padding:22px 28px;background:linear-gradient(110deg,#00665f,#08796e);color:#fff;border-bottom:5px solid #f0a72d}
.header h1{margin:0;font-size:28px}.header p{margin:5px 0 0;color:#d9eeeb}.content{padding:26px 30px 30px}
.identity{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:14px 16px;margin-bottom:22px;background:#f2f7f6;border:1px solid #dbe7e5}
.identity strong{display:block;color:#08796e;font-size:17px}.identity span{display:block;color:#697b79;font-size:13px}
.role{white-space:nowrap;background:#fff;border:1px solid #cbdad7;border-radius:20px;padding:5px 12px;color:#126d65;font-size:12px;font-weight:700}
.alert{padding:11px 14px;margin-bottom:18px;border:1px solid #b8d9cd;background:#edf8f2;color:#245f48}.alert.error{border-color:#e5b9b9;background:#fff1f1;color:#922d2d}
.grid{display:grid;grid-template-columns:1fr 1fr;gap:17px 20px}.field{display:flex;flex-direction:column;gap:6px}.field.full{grid-column:1/-1}
label{font-weight:600;color:#314542}input{width:100%;min-height:43px;padding:9px 12px;border:1px solid #bacac7;background:#fff;color:#203734;border-radius:2px;outline-color:#08796e}
input[readonly]{background:#f0f2f2;color:#64716f}.help{margin:5px 0 0;color:#71817f;font-size:12px}
.section-title{margin:25px 0 13px;padding-bottom:7px;border-bottom:2px solid #dce8e6;color:#08796e;font-size:16px}
.actions{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:26px}.button{display:inline-flex;align-items:center;justify-content:center;min-height:42px;padding:9px 17px;border:0;border-radius:2px;text-decoration:none;cursor:pointer;font-weight:700}
.primary{background:#08796e;color:#fff}.primary:hover{background:#00665f}.secondary{background:#edf2f1;color:#314542;border:1px solid #cddbd8}
.note{margin:20px 0 0;color:#71817f;font-size:12px}
@media(max-width:600px){body{padding:12px}.header{padding:19px}.content{padding:20px}.grid{grid-template-columns:1fr}.field.full{grid-column:auto}.actions{align-items:stretch;flex-direction:column-reverse}.button{width:100%}.role{white-space:normal;text-align:center}}
</style>
</head>
<body>
<main class="page">
<header class="header"><h1>Edit Profile Saya</h1><p>Perbarui informasi akun KRS Anda</p></header>
<section class="content">
<div class="identity"><div><strong><?=krs_e((string)$profile['display_name'])?></strong><span>Informasi akun yang sedang digunakan</span></div><span class="role"><?=krs_e($role)?></span></div>
<?php if ($flash !== ''): ?><div class="alert" role="status"><?=krs_e($flash)?></div><?php endif; ?>
<?php if ($error !== ''): ?><div class="alert error" role="alert"><?=krs_e($error)?></div><?php endif; ?>
<form method="post" action="profile.php" autocomplete="off">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<div class="grid">
<div class="field full"><label for="name">Nama Lengkap</label><input id="name" name="name" type="text" maxlength="50" value="<?=krs_e((string)$profile['display_name'])?>" required></div>
<div class="field full"><label for="account_id">ID Anda</label><input id="account_id" type="text" value="<?=krs_e((string)$profile['account_id'])?>" readonly></div>
<?php foreach ($definition['extras'] as $column): ?>
<div class="field"><label for="extra_<?=krs_e($column)?>"><?=krs_e($labels[$column] ?? $column)?></label><input id="extra_<?=krs_e($column)?>" type="text" value="<?=krs_e((string)($profile[$column] ?? ''))?>" readonly></div>
<?php endforeach; ?>
</div>
<h2 class="section-title">Ganti Password</h2>
<div class="grid">
<div class="field"><label for="new_password">Password Baru</label><input id="new_password" name="new_password" type="password" minlength="8" maxlength="1024" autocomplete="new-password"></div>
<div class="field"><label for="confirm_password">Konfirmasi Password</label><input id="confirm_password" name="confirm_password" type="password" minlength="8" maxlength="1024" autocomplete="new-password"></div>
</div>
<p class="help">Kosongkan kedua kolom password jika tidak ingin menggantinya. Password baru minimal 8 karakter.</p>
<div class="actions"><a class="button secondary" href="index.php">Kembali ke Dashboard</a><button class="button primary" type="submit">Simpan Perubahan</button></div>
</form>
<p class="note">ID, angkatan, semester, dan posisi merupakan informasi tetap yang dikelola melalui data akademik.</p>
</section>
</main>
</body>
</html>
