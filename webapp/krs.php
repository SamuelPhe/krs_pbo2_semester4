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
    echo 'Akses ditolak. Manajemen sesi KRS hanya dapat digunakan oleh Admin.';
    exit;
}

$pdo = krs_db();
$pdo->exec("UPDATE krs SET status_krs = 'tertutup' WHERE tanggal_selesai < CURRENT_DATE AND status_krs = 'terbuka'");
$pdo->exec("UPDATE krs SET status_krs = 'terbuka' WHERE CURRENT_DATE BETWEEN tanggal_mulai AND tanggal_selesai AND status_krs = 'tertutup'");

function krs_period_date_is_valid(string $value): bool
{
    $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value);
    $errors = DateTimeImmutable::getLastErrors();
    return $date !== false
        && ($errors === false || ($errors['warning_count'] === 0 && $errors['error_count'] === 0))
        && $date->format('Y-m-d') === $value;
}

$error = '';
$success = (string)($_SESSION['krs_period_flash'] ?? '');
unset($_SESSION['krs_period_flash']);
$formValues = [
    'id_prodi' => '',
    'semester' => '1',
    'tahun_ajaran' => (string)date('Y'),
    'status_krs' => 'tertutup',
    'tanggal_mulai' => date('Y-m-d'),
    'tanggal_selesai' => date('Y-m-d'),
];
$editId = filter_input(INPUT_GET, 'edit', FILTER_VALIDATE_INT) ?: 0;
$showForm = isset($_GET['new']) || $editId > 0;

$programs = $pdo->query('SELECT id_prodi, nama_prodi FROM prodi ORDER BY nama_prodi')->fetchAll();

if ($editId > 0) {
    $editQuery = $pdo->prepare('SELECT id_krs, id_prodi, semester, tahun_ajaran, status_krs, tanggal_mulai, tanggal_selesai FROM krs WHERE id_krs = ? LIMIT 1');
    $editQuery->execute([$editId]);
    $existing = $editQuery->fetch();
    if (!$existing) {
        $editId = 0;
        $showForm = false;
        $error = 'Sesi KRS yang dipilih tidak ditemukan.';
    } else {
        $formValues = [
            'id_prodi' => (string)$existing['id_prodi'],
            'semester' => (string)$existing['semester'],
            'tahun_ajaran' => (string)$existing['tahun_ajaran'],
            'status_krs' => (string)$existing['status_krs'],
            'tanggal_mulai' => (string)$existing['tanggal_mulai'],
            'tanggal_selesai' => (string)$existing['tanggal_selesai'],
        ];
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $token = (string)($_SESSION['csrf_token'] ?? '');
    $sentToken = (string)($_POST['csrf_token'] ?? '');
    if ($token === '' || !hash_equals($token, $sentToken)) {
        http_response_code(403);
        $error = 'Sesi formulir tidak valid. Muat ulang halaman dan coba lagi.';
    } else {
        $action = (string)($_POST['action'] ?? '');
        $selectedId = filter_var($_POST['id_krs'] ?? '', FILTER_VALIDATE_INT);
        if ($selectedId === false || $selectedId === null || $selectedId < 1) {
            $selectedId = 0;
        }

        try {
            if ($action === 'prepare_edit') {
                if ($selectedId < 1) {
                    $error = 'Pilih satu sesi KRS pada tabel terlebih dahulu.';
                } else {
                    header('Location: krs.php?edit=' . $selectedId, true, 303);
                    exit;
                }
            } elseif ($action === 'delete') {
                if ($selectedId < 1) {
                    $error = 'Pilih satu sesi KRS yang ingin dihapus.';
                } else {
                    $pdo->beginTransaction();
                    $check = $pdo->prepare('SELECT COUNT(*) FROM pengajuan_krs WHERE id_krs = ?');
                    $check->execute([$selectedId]);
                    $submissionCount = (int)$check->fetchColumn();

                    if ($submissionCount > 0) {
                        $pdo->rollBack();
                        $error = 'Sesi tidak dapat dihapus karena sudah memiliki ' . $submissionCount . ' pengajuan KRS. Ubah statusnya menjadi tertutup jika tidak lagi digunakan.';
                    } else {
                        $delete = $pdo->prepare('DELETE FROM krs WHERE id_krs = ?');
                        $delete->execute([$selectedId]);
                        $deleted = $delete->rowCount();
                        $pdo->commit();
                        if ($deleted > 0) {
                            $_SESSION['krs_period_flash'] = 'Sesi KRS berhasil dihapus.';
                            header('Location: krs.php', true, 303);
                            exit;
                        }
                        $error = 'Sesi KRS tidak ditemukan atau sudah dihapus.';
                    }
                }
            } elseif ($action === 'save') {
                $formValues = [
                    'id_prodi' => trim((string)($_POST['id_prodi'] ?? '')),
                    'semester' => trim((string)($_POST['semester'] ?? '')),
                    'tahun_ajaran' => trim((string)($_POST['tahun_ajaran'] ?? '')),
                    'status_krs' => trim((string)($_POST['status_krs'] ?? '')),
                    'tanggal_mulai' => trim((string)($_POST['tanggal_mulai'] ?? '')),
                    'tanggal_selesai' => trim((string)($_POST['tanggal_selesai'] ?? '')),
                ];
                $idProdi = filter_var($formValues['id_prodi'], FILTER_VALIDATE_INT);
                $semester = filter_var($formValues['semester'], FILTER_VALIDATE_INT);
                $year = filter_var($formValues['tahun_ajaran'], FILTER_VALIDATE_INT);
                $validStatuses = ['terbuka', 'tertutup'];

                if ($idProdi === false || $idProdi < 1) {
                    $error = 'Pilih program studi yang valid.';
                } elseif ($semester === false || $semester < 1 || $semester > 14) {
                    $error = 'Semester harus berupa angka 1 sampai 14.';
                } elseif ($year === false || $year < 2000 || $year > 2099 || strlen($formValues['tahun_ajaran']) !== 4) {
                    $error = 'Tahun ajaran harus berupa angka empat digit, misalnya 2026.';
                } elseif (!in_array($formValues['status_krs'], $validStatuses, true)) {
                    $error = 'Status sesi KRS tidak valid.';
                } elseif (!krs_period_date_is_valid($formValues['tanggal_mulai'])
                    || !krs_period_date_is_valid($formValues['tanggal_selesai'])) {
                    $error = 'Tanggal mulai dan selesai harus valid.';
                } elseif ($formValues['tanggal_selesai'] < $formValues['tanggal_mulai']) {
                    $error = 'Tanggal selesai tidak boleh mendahului tanggal mulai.';
                } else {
                    $programCheck = $pdo->prepare('SELECT COUNT(*) FROM prodi WHERE id_prodi = ?');
                    $programCheck->execute([$idProdi]);
                    if ((int)$programCheck->fetchColumn() !== 1) {
                        $error = 'Program studi yang dipilih tidak ditemukan.';
                    } else {
                        $overlapSql = 'SELECT id_krs FROM krs
                            WHERE id_prodi = ? AND semester = ? AND tahun_ajaran = ?
                            AND tanggal_mulai <= ? AND tanggal_selesai >= ?';
                        $overlapValues = [
                            $idProdi,
                            $semester,
                            $year,
                            $formValues['tanggal_selesai'],
                            $formValues['tanggal_mulai'],
                        ];
                        if ($selectedId > 0) {
                            $overlapSql .= ' AND id_krs <> ?';
                            $overlapValues[] = $selectedId;
                        }
                        $overlapSql .= ' LIMIT 1';
                        $overlap = $pdo->prepare($overlapSql);
                        $overlap->execute($overlapValues);

                        if ($overlap->fetch()) {
                            $error = 'Tanggal sesi beririsan dengan sesi KRS lain untuk program studi, semester, dan tahun ajaran yang sama.';
                        } else {
                            if ($selectedId > 0) {
                                $save = $pdo->prepare('UPDATE krs SET id_prodi = ?, semester = ?, tahun_ajaran = ?, status_krs = ?, tanggal_mulai = ?, tanggal_selesai = ? WHERE id_krs = ?');
                                $save->execute([
                                    $idProdi,
                                    $semester,
                                    $year,
                                    $formValues['status_krs'],
                                    $formValues['tanggal_mulai'],
                                    $formValues['tanggal_selesai'],
                                    $selectedId,
                                ]);
                                $_SESSION['krs_period_flash'] = 'Sesi KRS berhasil diperbarui.';
                            } else {
                                $save = $pdo->prepare('INSERT INTO krs (id_prodi, semester, tahun_ajaran, status_krs, tanggal_mulai, tanggal_selesai) VALUES (?, ?, ?, ?, ?, ?)');
                                $save->execute([
                                    $idProdi,
                                    $semester,
                                    $year,
                                    $formValues['status_krs'],
                                    $formValues['tanggal_mulai'],
                                    $formValues['tanggal_selesai'],
                                ]);
                                $_SESSION['krs_period_flash'] = 'Sesi KRS berhasil ditambahkan.';
                            }
                            header('Location: krs.php', true, 303);
                            exit;
                        }
                    }
                }

                $editId = $selectedId;
                $showForm = true;
            } else {
                $error = 'Aksi yang diminta tidak dikenal.';
            }
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            error_log('KRS period action error: ' . $exception->getMessage());
            $error = 'Perubahan sesi KRS gagal disimpan. Periksa data dan hubungan pengajuannya.';
            if ($action === 'save') {
                $editId = $selectedId;
                $showForm = true;
            }
        }
    }
}

$rows = $pdo->query(
    'SELECT k.id_krs, k.id_prodi, p.nama_prodi, k.semester, k.tahun_ajaran,
            k.status_krs, k.tanggal_mulai, k.tanggal_selesai,
            (SELECT COUNT(*) FROM pengajuan_krs pk WHERE pk.id_krs = k.id_krs) AS jumlah_pengajuan
     FROM krs k
     LEFT JOIN prodi p ON p.id_prodi = k.id_prodi
     ORDER BY k.id_krs DESC'
)->fetchAll();

if ($showForm && $editId === 0 && $formValues['id_prodi'] === '' && count($programs) === 1) {
    $formValues['id_prodi'] = (string)$programs[0]['id_prodi'];
}
?>
<!doctype html>
<html lang="id">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Manajemen Aktifasi Sesi KRS</title>
<style>
*{box-sizing:border-box}html,body{margin:0;min-height:100%;font-family:"Segoe UI",Arial,sans-serif;color:#303735}
body{min-height:100vh;background:#d9ddd8;display:flex;align-items:center;justify-content:center;padding:18px}
.canvas{position:relative;width:min(1000px,100%);aspect-ratio:5/4;min-height:620px;background:#d9d5bb;box-shadow:0 12px 38px #24332f42;overflow:hidden}
.bg{position:absolute;inset:0;width:100%;height:100%;object-fit:fill;user-select:none}
.subtitle{position:absolute;left:2%;top:8.2%;margin:0;font-size:clamp(12px,1.7vw,17px);font-weight:700;color:#3a4642}
.table-wrap{position:absolute;left:2%;top:12.5%;width:96%;height:65%;overflow:auto;background:#ffffffed;border:1px solid #788d88;box-shadow:0 3px 12px #38443d2a}
table{width:100%;border-collapse:collapse;font-size:clamp(10px,1.35vw,14px);background:#fff}
thead th{position:sticky;top:0;z-index:1;background:#0b7068;color:#fff;text-align:left;padding:12px 10px;font-size:clamp(10px,1.25vw,13px);font-weight:700;white-space:nowrap;border-right:1px solid #ffffff30}
tbody td{padding:11px 10px;border-bottom:1px solid #dbe2df;color:#303f3c;white-space:nowrap}
tbody tr:nth-child(even){background:#f1f5f4}tbody tr:hover{background:#fff3d7;cursor:pointer}tbody tr.selected{background:#fce8b2;outline:2px solid #d49b2e;outline-offset:-2px}
.status{display:inline-block;border-radius:14px;padding:3px 9px;font-size:11px;font-weight:700;text-transform:uppercase}
.status.open{background:#d7f0e5;color:#126849}.status.closed{background:#e5e9e8;color:#566562}
.empty{padding:30px;text-align:center;color:#64716e}
.toolbar{position:absolute;left:2%;right:2%;bottom:12%;display:grid;grid-template-columns:1fr 1fr 1fr 1fr;gap:12px}
.toolbar a,.toolbar button{display:flex;align-items:center;justify-content:center;min-height:54px;background:#f8f8f4;border:1px solid #526d67;color:#214e49;text-decoration:none;font-weight:700;font-size:clamp(13px,2vw,22px);box-shadow:0 3px 0 #a8b1aa;cursor:pointer}
.toolbar a:hover,.toolbar button:hover{background:#0b7068;color:#fff}.toolbar .delete{color:#a33c33;border-color:#ad6c64}.toolbar .delete:hover{background:#a33c33;color:white}
.selected-id{position:absolute;width:1px;height:1px;opacity:0;pointer-events:none}
.feedback{position:absolute;left:2%;right:2%;top:9.2%;z-index:2;padding:7px 12px;border:1px solid #b4d8c6;background:#edfaf2;color:#205b40;font-size:13px}
.feedback.error{border-color:#e3b7b3;background:#fff0ee;color:#8b2f29}
.footer{position:absolute;left:2%;bottom:5%;font-size:11px;color:#52615d}
.dialog-shade{position:absolute;inset:0;z-index:5;background:#172824a8;display:flex;align-items:center;justify-content:center;padding:20px}
.dialog{width:min(590px,100%);max-height:94%;overflow:auto;background:#f8f8f3;border:1px solid #47655e;box-shadow:0 18px 55px #142b27aa}
.dialog-head{padding:15px 20px;background:#0b7068;border-bottom:4px solid #e2b54e;color:#fff}
.dialog-head h2{margin:0;font-size:22px}.dialog-body{padding:18px 20px}
.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:13px 15px}
.field{display:flex;flex-direction:column;gap:5px}.field.full{grid-column:1/-1}
label{font-size:13px;font-weight:700;color:#384d48}
input,select{min-height:39px;padding:8px 10px;border:1px solid #aebfba;background:white;color:#283a36;font:14px "Segoe UI",Arial,sans-serif;border-radius:0}
.hint{margin:6px 0 0;color:#71817d;font-size:11px;line-height:1.45}
.dialog-actions{display:flex;justify-content:flex-end;gap:10px;margin-top:20px}
.dialog-actions a,.dialog-actions button{padding:10px 16px;border:1px solid #789089;background:#e9efec;color:#294740;text-decoration:none;font-weight:700;cursor:pointer}
.dialog-actions button{background:#0b7068;border-color:#0b7068;color:#fff}
@media(max-width:700px){
 body{padding:0;align-items:flex-start}.canvas{width:100%;min-height:760px;aspect-ratio:auto;height:100vh}
 .table-wrap{top:13%;height:61%}.toolbar{grid-template-columns:1fr 1fr;bottom:10%;gap:8px}
 .toolbar a,.toolbar button{min-height:48px}.form-grid{grid-template-columns:1fr}.field.full{grid-column:auto}
 .dialog-shade{position:fixed;overflow:auto}.footer{bottom:3%}
}
</style>
</head>
<body>
<main class="canvas">
<img class="bg" src="https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Manajemen%20data.png" alt="">
<p class="subtitle">Manajemen Aktifasi Sesi KRS</p>
<?php if ($success !== ''): ?><div class="feedback" role="status"><?=krs_e($success)?></div><?php endif; ?>
<?php if ($error !== ''): ?><div class="feedback error" role="alert"><?=krs_e($error)?></div><?php endif; ?>

<div class="table-wrap">
<table>
<thead><tr>
<th>ID Sesi</th><th>Program Studi</th><th>Semester</th><th>Tahun Ajaran</th><th>Status KRS</th><th>Tanggal Mulai</th><th>Tanggal Selesai</th><th>Pengajuan</th>
</tr></thead>
<tbody>
<?php if (!$rows): ?>
<tr><td class="empty" colspan="8">Belum ada sesi KRS yang terdaftar.</td></tr>
<?php else: foreach ($rows as $row): ?>
<tr data-id="<?=krs_e((string)$row['id_krs'])?>" tabindex="0" aria-label="Pilih sesi KRS <?=krs_e((string)$row['id_krs'])?>">
<td><?=krs_e((string)$row['id_krs'])?></td>
<td><?=krs_e((string)($row['nama_prodi'] ?? 'Program studi tidak ditemukan'))?></td>
<td><?=krs_e((string)$row['semester'])?></td>
<td><?=krs_e((string)$row['tahun_ajaran'])?></td>
<td><span class="status <?=($row['status_krs'] === 'terbuka' ? 'open' : 'closed')?>"><?=krs_e((string)$row['status_krs'])?></span></td>
<td><?=krs_e((string)$row['tanggal_mulai'])?></td>
<td><?=krs_e((string)$row['tanggal_selesai'])?></td>
<td><?=krs_e((string)$row['jumlah_pengajuan'])?></td>
</tr>
<?php endforeach; endif; ?>
</tbody>
</table>
</div>

<form id="selection-form" method="post" action="krs.php">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input class="selected-id" id="selected-id" name="id_krs" value="" aria-hidden="true" tabindex="-1">
<div class="toolbar">
<a href="index.php">Back</a>
<a href="krs.php?new=1">Add</a>
<button type="submit" name="action" value="prepare_edit">Edit</button>
<button class="delete" type="submit" name="action" value="delete" onclick="return confirm('Hapus sesi KRS yang dipilih? Sesi yang sudah memiliki pengajuan tidak dapat dihapus.')">Delete</button>
</div>
</form>
<p class="footer">Pilih satu baris tabel sebelum menggunakan Edit atau Delete. Semua perubahan disimpan ke database KRS.</p>

<?php if ($showForm): ?>
<div class="dialog-shade" role="presentation">
<section class="dialog" role="dialog" aria-modal="true" aria-labelledby="dialog-title">
<header class="dialog-head"><h2 id="dialog-title"><?=($editId > 0 ? 'Edit Sesi KRS' : 'Tambah Sesi Pembukaan KRS')?></h2></header>
<div class="dialog-body">
<?php if ($error !== ''): ?><div class="feedback error" style="position:static;margin-bottom:14px"><?=krs_e($error)?></div><?php endif; ?>
<form method="post" action="krs.php">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="action" value="save">
<input type="hidden" name="id_krs" value="<?=krs_e((string)$editId)?>">
<div class="form-grid">
<div class="field full">
<label for="id_prodi">Program Studi</label>
<select id="id_prodi" name="id_prodi" required>
<option value="">-- Pilih Program Studi --</option>
<?php foreach ($programs as $program): ?>
<option value="<?=krs_e((string)$program['id_prodi'])?>" <?=((string)$formValues['id_prodi'] === (string)$program['id_prodi'] ? 'selected' : '')?>><?=krs_e((string)$program['nama_prodi'])?></option>
<?php endforeach; ?>
</select>
</div>
<div class="field">
<label for="semester">Semester</label>
<select id="semester" name="semester" required>
<?php for ($semesterOption = 1; $semesterOption <= 14; $semesterOption++): ?>
<option value="<?=$semesterOption?>" <?=((string)$formValues['semester'] === (string)$semesterOption ? 'selected' : '')?>>Semester <?=$semesterOption?></option>
<?php endfor; ?>
</select>
</div>
<div class="field">
<label for="tahun_ajaran">Tahun Ajaran</label>
<input id="tahun_ajaran" name="tahun_ajaran" type="number" min="2000" max="2099" step="1" inputmode="numeric" value="<?=krs_e((string)$formValues['tahun_ajaran'])?>" required>
<p class="hint">Struktur database tugas saat ini menyimpan tahun sebagai angka empat digit, misalnya 2026.</p>
</div>
<div class="field">
<label for="status_krs">Status Sesi</label>
<select id="status_krs" name="status_krs" required>
<option value="terbuka" <?=($formValues['status_krs'] === 'terbuka' ? 'selected' : '')?>>terbuka</option>
<option value="tertutup" <?=($formValues['status_krs'] === 'tertutup' ? 'selected' : '')?>>tertutup</option>
</select>
</div>
<div class="field">
<label for="tanggal_mulai">Tanggal Mulai</label>
<input id="tanggal_mulai" name="tanggal_mulai" type="date" value="<?=krs_e((string)$formValues['tanggal_mulai'])?>" required>
</div>
<div class="field">
<label for="tanggal_selesai">Tanggal Selesai</label>
<input id="tanggal_selesai" name="tanggal_selesai" type="date" value="<?=krs_e((string)$formValues['tanggal_selesai'])?>" required>
</div>
</div>
<div class="dialog-actions">
<a href="krs.php">Batal</a>
<button type="submit">Simpan Sesi KRS</button>
</div>
</form>
</div>
</section>
</div>
<?php endif; ?>
</main>
<script>
(function(){
  const selected = document.getElementById('selected-id');
  document.querySelectorAll('tbody tr[data-id]').forEach(function(row){
    function choose(){
      document.querySelectorAll('tbody tr[data-id]').forEach(function(other){other.classList.remove('selected');});
      row.classList.add('selected');
      selected.value = row.dataset.id;
    }
    row.addEventListener('click', choose);
    row.addEventListener('keydown', function(event){
      if(event.key === 'Enter' || event.key === ' '){event.preventDefault();choose();}
    });
  });
})();
</script>
</body>
</html>
