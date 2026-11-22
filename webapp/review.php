<?php
declare(strict_types=1);

require __DIR__ . '/config.php';

$account = $_SESSION['krs_user'] ?? null;
if (!is_array($account)) {
    header('Location: index.php?notice=login-required', true, 302);
    exit;
}

$role = (string)($account['role'] ?? '');
$allowedPositions = ['Dosen PA' => 'dosen pa', 'Kaprodi' => 'kaprodi'];
if (!isset($allowedPositions[$role])) {
    http_response_code(403);
    echo 'Akses ditolak. Menu ini hanya untuk Dosen PA atau Kaprodi.';
    exit;
}

$pdo = krs_db();
$advisorQuery = $pdo->prepare('SELECT id_dosen, nama_dosen, posisi FROM dosen WHERE id_dosen = ? LIMIT 1');
$advisorQuery->execute([(int)($account['id'] ?? 0)]);
$advisor = $advisorQuery->fetch();
if (!$advisor || strtolower(trim((string)$advisor['posisi'])) !== $allowedPositions[$role]) {
    unset($_SESSION['krs_user']);
    session_regenerate_id(true);
    header('Location: index.php?notice=login-required', true, 302);
    exit;
}
$advisorId = (int)$advisor['id_dosen'];

function krs_review_application(PDO $pdo, int $applicationId, int $advisorId): ?array
{
    $query = $pdo->prepare(
        'SELECT pk.id_pengajuan, pk.id_krs, pk.id_mahasiswa, pk.tanggal_pengajuan,
                pk.status_acc, pk.catatan_revisi, m.nama_mahasiswa, m.id_prodi,
                k.semester, k.tahun_ajaran, k.status_krs,
                k.tanggal_mulai, k.tanggal_selesai, p.nama_prodi
         FROM pengajuan_krs pk
         JOIN mahasiswa m ON m.id_mahasiswa = pk.id_mahasiswa
         JOIN krs k ON k.id_krs = pk.id_krs
         LEFT JOIN prodi p ON p.id_prodi = m.id_prodi
         WHERE pk.id_pengajuan = ? AND m.id_dosen_pa = ?
         LIMIT 1'
    );
    $query->execute([$applicationId, $advisorId]);
    $row = $query->fetch();
    return $row ?: null;
}

function krs_review_is_open(array $application, string $today): bool
{
    return $application['status_acc'] === 'Ditinjau'
        && $application['status_krs'] === 'terbuka'
        && $today >= (string)$application['tanggal_mulai']
        && $today <= (string)$application['tanggal_selesai'];
}

$today = (string)$pdo->query('SELECT CURRENT_DATE')->fetchColumn();
$applicationId = filter_input(INPUT_GET, 'id', FILTER_VALIDATE_INT) ?: 0;
$error = '';
$success = (string)($_SESSION['krs_review_flash'] ?? '');
unset($_SESSION['krs_review_flash']);

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $knownToken = (string)($_SESSION['csrf_token'] ?? '');
    $sentToken = (string)($_POST['csrf_token'] ?? '');
    if ($knownToken === '' || !hash_equals($knownToken, $sentToken)) {
        http_response_code(403);
        $error = 'Sesi formulir tidak valid. Muat ulang halaman dan coba lagi.';
    } else {
        $action = (string)($_POST['action'] ?? '');
        $postedId = filter_var($_POST['id_pengajuan'] ?? '', FILTER_VALIDATE_INT);
        $postedId = ($postedId === false || $postedId === null || $postedId < 1) ? 0 : (int)$postedId;
        if ($postedId < 1) {
            $error = 'ID pengajuan tidak valid.';
        } else {
            $applicationId = $postedId;
            try {
                $pdo->beginTransaction();
                $lockQuery = $pdo->prepare(
                    'SELECT pk.id_pengajuan, pk.id_krs, pk.id_mahasiswa, pk.status_acc,
                            k.status_krs, k.tanggal_mulai, k.tanggal_selesai,
                            m.id_dosen_pa
                     FROM pengajuan_krs pk
                     JOIN krs k ON k.id_krs = pk.id_krs
                     JOIN mahasiswa m ON m.id_mahasiswa = pk.id_mahasiswa
                     WHERE pk.id_pengajuan = ? AND m.id_dosen_pa = ?
                     FOR UPDATE'
                );
                $lockQuery->execute([$postedId, $advisorId]);
                $locked = $lockQuery->fetch();
                $freshToday = (string)$pdo->query('SELECT CURRENT_DATE')->fetchColumn();
                $isOpen = $locked
                    && $locked['status_acc'] === 'Ditinjau'
                    && $locked['status_krs'] === 'terbuka'
                    && $freshToday >= (string)$locked['tanggal_mulai']
                    && $freshToday <= (string)$locked['tanggal_selesai'];

                if (!$locked) {
                    $pdo->rollBack();
                    http_response_code(404);
                    $error = 'Pengajuan tidak ditemukan pada daftar mahasiswa bimbingan Anda.';
                } elseif (!$isOpen) {
                    $pdo->rollBack();
                    $error = 'Pengajuan tidak lagi berada dalam masa review aktif.';
                } elseif (in_array($action, ['approve_selected', 'reject_selected'], true)) {
                    $rawIds = $_POST['detail_ids'] ?? [];
                    if (!is_array($rawIds)) {
                        $rawIds = [];
                    }
                    $detailIds = [];
                    foreach ($rawIds as $rawId) {
                        $value = filter_var($rawId, FILTER_VALIDATE_INT);
                        if ($value !== false && $value !== null && $value > 0) {
                            $detailIds[] = (int)$value;
                        }
                    }
                    $detailIds = array_values(array_unique($detailIds));
                    if (!$detailIds || count($detailIds) > 40) {
                        $pdo->rollBack();
                        $error = 'Pilih satu sampai 40 mata kuliah yang masih menunggu tinjauan.';
                    } else {
                        $reason = trim((string)($_POST['catatan_revisi'] ?? ''));
                        if ($action === 'reject_selected' && ($reason === '' || mb_strlen($reason, 'UTF-8') > 1000)) {
                            $pdo->rollBack();
                            $error = 'Catatan revisi wajib diisi dan maksimal 1.000 karakter.';
                        } else {
                            $updated = 0;
                            $approve = $pdo->prepare(
                                "UPDATE krs_detail kd
                                 JOIN pengajuan_krs pk ON pk.id_pengajuan = kd.id_pengajuan
                                 JOIN mahasiswa m ON m.id_mahasiswa = pk.id_mahasiswa
                                 SET kd.status_acc = 'Disetujui', kd.catatan_revisi = ''
                                 WHERE kd.id_detail = ? AND kd.id_pengajuan = ?
                                   AND m.id_dosen_pa = ? AND pk.status_acc = 'Ditinjau'
                                   AND kd.status_acc = 'Ditinjau'"
                            );
                            $reject = $pdo->prepare(
                                "UPDATE krs_detail kd
                                 JOIN pengajuan_krs pk ON pk.id_pengajuan = kd.id_pengajuan
                                 JOIN mahasiswa m ON m.id_mahasiswa = pk.id_mahasiswa
                                 SET kd.status_acc = 'Ditolak', kd.catatan_revisi = ?
                                 WHERE kd.id_detail = ? AND kd.id_pengajuan = ?
                                   AND m.id_dosen_pa = ? AND pk.status_acc = 'Ditinjau'
                                   AND kd.status_acc = 'Ditinjau'"
                            );
                            foreach ($detailIds as $detailId) {
                                if ($action === 'approve_selected') {
                                    $approve->execute([$detailId, $postedId, $advisorId]);
                                } else {
                                    $reject->execute([$reason, $detailId, $postedId, $advisorId]);
                                }
                                $updated += $approve->rowCount() + $reject->rowCount();
                            }
                            if ($updated < 1) {
                                $pdo->rollBack();
                                $error = 'Tidak ada mata kuliah yang dapat diperbarui. Periksa pilihan dan status terkini.';
                            } else {
                                $pdo->commit();
                                $_SESSION['krs_review_flash'] = $updated . ' mata kuliah berhasil '
                                    . ($action === 'approve_selected' ? 'disetujui.' : 'dikembalikan untuk revisi.');
                                header('Location: review.php?id=' . $postedId, true, 303);
                                exit;
                            }
                        }
                    }
                } elseif ($action === 'finish_review') {
                    $details = $pdo->prepare(
                        'SELECT kd.status_acc, kd.catatan_revisi, m.nama_matkul
                         FROM krs_detail kd
                         JOIN jadwal j ON j.id_jadwal = kd.id_jadwal
                         JOIN matkul m ON m.id_matkul = j.id_matkul
                         WHERE kd.id_pengajuan = ?
                         FOR UPDATE'
                    );
                    $details->execute([$postedId]);
                    $detailStatuses = $details->fetchAll();
                    $pending = array_filter($detailStatuses, static fn(array $item): bool =>
                        in_array($item['status_acc'], ['Draft', 'Ditinjau'], true)
                    );
                    if (!$detailStatuses || $pending) {
                        $pdo->rollBack();
                        $error = !$detailStatuses
                            ? 'Pengajuan belum memiliki mata kuliah untuk ditinjau.'
                            : 'Masih ada mata kuliah berstatus Draft atau Ditinjau. Berikan keputusan pada semuanya terlebih dahulu.';
                    } else {
                        $rejected = array_filter($detailStatuses, static fn(array $item): bool =>
                            $item['status_acc'] === 'Ditolak'
                        );
                        $nextStatus = $rejected ? 'Ditolak' : 'AccDosenPA';
                        $summaryLines = [];
                        foreach ($rejected as $item) {
                            $summaryLines[] = (string)$item['nama_matkul'] . ': ' . (string)$item['catatan_revisi'];
                        }
                        $summary = $rejected ? implode("\n", $summaryLines) : null;
                        $update = $pdo->prepare(
                            'UPDATE pengajuan_krs
                             SET status_acc = ?, id_dosen_acc = ?, catatan_revisi = ?
                             WHERE id_pengajuan = ? AND status_acc = \'Ditinjau\''
                        );
                        $update->execute([$nextStatus, $advisorId, $summary, $postedId]);
                        if ($update->rowCount() !== 1) {
                            $pdo->rollBack();
                            $error = 'Status pengajuan berubah. Muat ulang antrean review.';
                        } else {
                            $pdo->commit();
                            $_SESSION['krs_review_flash'] = $nextStatus === 'AccDosenPA'
                                ? 'Review selesai. Pengajuan diteruskan kepada Kaprodi untuk finalisasi.'
                                : 'Review selesai. Pengajuan dikembalikan kepada Mahasiswa karena ada mata kuliah yang perlu direvisi.';
                            header('Location: review.php', true, 303);
                            exit;
                        }
                    }
                } else {
                    $pdo->rollBack();
                    $error = 'Aksi review tidak dikenal.';
                }
            } catch (Throwable $exception) {
                if ($pdo->inTransaction()) {
                    $pdo->rollBack();
                }
                error_log('KRS advisor review error: ' . $exception->getMessage());
                $error = 'Review tidak dapat disimpan. Periksa status pengajuan dan data mata kuliah.';
            }
        }
    }
}

$application = null;
$detailRows = [];
$canAct = false;
$pendingCount = 0;
$rejectedCount = 0;
if ($applicationId > 0) {
    $application = krs_review_application($pdo, $applicationId, $advisorId);
    if (!$application) {
        http_response_code(404);
        $error = $error !== '' ? $error : 'Pengajuan tidak ditemukan pada daftar mahasiswa bimbingan Anda.';
    } else {
        $canAct = krs_review_is_open($application, $today);
        $detailQuery = $pdo->prepare(
            'SELECT kd.id_detail, kd.status_acc, kd.catatan_revisi,
                    m.nama_matkul, m.sks, j.hari, j.jam_mulai, j.jam_selesai,
                    k.nama_kelas, r.nama_ruang
             FROM krs_detail kd
             JOIN jadwal j ON j.id_jadwal = kd.id_jadwal
             JOIN matkul m ON m.id_matkul = j.id_matkul
             LEFT JOIN kelas k ON k.id_kelas = j.kelas
             LEFT JOIN ruang r ON r.id_ruang = j.ruang
             WHERE kd.id_pengajuan = ?
             ORDER BY FIELD(LOWER(j.hari), \'senin\', \'selasa\', \'rabu\', \'kamis\', \'jumat\', \'sabtu\', \'minggu\'), j.jam_mulai'
        );
        $detailQuery->execute([$applicationId]);
        $detailRows = $detailQuery->fetchAll();
        foreach ($detailRows as $detail) {
            if (in_array($detail['status_acc'], ['Draft', 'Ditinjau'], true)) {
                $pendingCount++;
            }
            if ($detail['status_acc'] === 'Ditolak') {
                $rejectedCount++;
            }
        }
    }
}

$queueQuery = $pdo->prepare(
    "SELECT pk.id_pengajuan, pk.id_mahasiswa, pk.tanggal_pengajuan,
            m.nama_mahasiswa, p.nama_prodi, k.semester, k.tahun_ajaran,
            (SELECT COUNT(*) FROM krs_detail kd WHERE kd.id_pengajuan = pk.id_pengajuan) AS jumlah_matkul,
            (SELECT COALESCE(SUM(mc.sks), 0)
             FROM krs_detail kd
             JOIN jadwal j ON j.id_jadwal = kd.id_jadwal
             JOIN matkul mc ON mc.id_matkul = j.id_matkul
             WHERE kd.id_pengajuan = pk.id_pengajuan) AS total_sks
     FROM pengajuan_krs pk
     JOIN mahasiswa m ON m.id_mahasiswa = pk.id_mahasiswa
     JOIN krs k ON k.id_krs = pk.id_krs
     LEFT JOIN prodi p ON p.id_prodi = m.id_prodi
     WHERE pk.status_acc = 'Ditinjau'
       AND m.id_dosen_pa = ?
       AND k.status_krs = 'terbuka'
       AND CURRENT_DATE BETWEEN k.tanggal_mulai AND k.tanggal_selesai
     ORDER BY pk.tanggal_pengajuan ASC, pk.id_pengajuan ASC"
);
$queueQuery->execute([$advisorId]);
$queue = $queueQuery->fetchAll();

$statusLabels = ['Draft' => 'Draft', 'Ditinjau' => 'Menunggu Tinjauan', 'Ditolak' => 'Perlu Revisi', 'AccDosenPA' => 'Disetujui Dosen PA', 'Disetujui' => 'Disetujui'];
function krs_review_status_class(string $status): string
{
    return match ($status) {
        'Disetujui', 'AccDosenPA' => 'approved',
        'Ditolak' => 'rejected',
        'Ditinjau' => 'review',
        default => 'draft',
    };
}
?>
<!doctype html>
<html lang="id">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title><?=($applicationId > 0 ? 'Detail Review KRS' : 'Review KRS Mahasiswa Bimbingan')?> — KRS</title>
<style>
*{box-sizing:border-box}html,body{margin:0;min-height:100%;font-family:"Segoe UI",Arial,sans-serif;color:#303735}
body{background:#d9ddd8;padding:18px}
.canvas{position:relative;width:min(1024px,100%);margin:0 auto;min-height:660px;box-shadow:0 12px 38px #24332f42;background:#d9d5bb}
.bg{position:absolute;inset:0;width:100%;height:100%;object-fit:fill;user-select:none}
.content{position:relative;z-index:1;padding:44px 7% 34px}
.heading{display:flex;align-items:center;justify-content:space-between;gap:14px;margin:0 0 18px}
.heading h1{margin:0;color:#263d38;font-size:clamp(21px,3vw,30px)}
.heading p{margin:4px 0 0;color:#5a6964;font-size:13px}
.back{padding:9px 15px;border:1px solid #69817a;background:#f7f7f0;color:#31514a;text-decoration:none;font-weight:700}
.panel{background:#ffffffdf;border:1px solid #a4b8b0;border-radius:6px;padding:14px 16px;margin:0 0 17px;box-shadow:0 2px 7px #2a443a14}
.panel h2{margin:0 0 10px;color:#20675f;font-size:17px}
.panel-note{margin:0 0 10px;color:#63716c;font-size:12px}
.table-scroll{overflow:auto;max-width:100%}
table{width:100%;border-collapse:collapse;font-size:13px;background:#fff}
th{background:#0b7068;color:#fff;text-align:left;padding:10px 9px;white-space:nowrap}
td{padding:9px;border-bottom:1px solid #dfe6e2;color:#33413d;vertical-align:middle}
tbody tr:nth-child(even){background:#f1f5f3}
.status{display:inline-block;padding:4px 9px;border-radius:14px;font-size:11px;font-weight:700;white-space:nowrap}
.status.draft{background:#e9eef0;color:#435a61}.status.review{background:#fff0c8;color:#86580b}
.status.rejected{background:#fde5e2;color:#a03229}.status.approved{background:#d8f0e4;color:#216b48}
.btn{display:inline-flex;align-items:center;justify-content:center;padding:8px 12px;border:1px solid #607d75;background:#f8f8f3;color:#24514a;text-decoration:none;font-size:12px;font-weight:700;cursor:pointer}
.btn.primary{background:#0b7068;color:#fff;border-color:#0b7068}.btn.reject{background:#fff0ed;color:#963a31;border-color:#c68c84}
.btn:disabled{opacity:.45;cursor:not-allowed}
.alert{padding:10px 13px;margin:0 0 15px;background:#eef8f1;border:1px solid #b8d6c3;color:#265c40;font-size:13px}
.alert.error{background:#fff0ed;border-color:#dfb8ae;color:#8c3428}
.action-row{display:flex;flex-wrap:wrap;align-items:center;gap:9px;margin-top:14px}
.meta{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-bottom:15px}
.meta-item{background:#ffffffdf;border:1px solid #b1c3bc;padding:9px 11px}
.meta-item small{display:block;color:#6c7974;font-size:11px}.meta-item strong{display:block;color:#2a4b44;font-size:14px;margin-top:3px}
.revision{white-space:pre-line;margin:0 0 15px;background:#fff0ed;border:1px solid #dfb8ae;padding:11px 13px;color:#87362e;font-size:13px}
textarea{width:100%;min-height:76px;padding:9px 11px;border:1px solid #aebfba;background:#fff;color:#283a36;font:13px/1.45 "Segoe UI",Arial,sans-serif;resize:vertical}
label{font-size:12px;font-weight:700;color:#38534c;display:block;margin:0 0 5px}
.small{font-size:11px;color:#6b7974}.footer{font-size:11px;color:#66736e;margin:14px 0 0}
@media(max-width:720px){body{padding:0}.canvas{min-height:100vh}.content{padding:22px 14px}.heading{align-items:flex-start}.meta{grid-template-columns:1fr 1fr}.panel{padding:11px}.table-scroll table{min-width:720px}}
</style>
</head>
<body>
<main class="canvas">
<img class="bg" src="https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Rectangle%20(right).png" alt="">
<div class="content">
<header class="heading">
<div><h1><?=($applicationId > 0 ? 'Detail Review KRS' : 'Daftar Pengajuan KRS Mahasiswa Bimbingan')?></h1><p><?=krs_e((string)$advisor['nama_dosen'])?> · <?=krs_e($role)?></p></div>
<a class="back" href="<?=($applicationId > 0 ? 'review.php' : 'index.php')?>">Back</a>
</header>

<?php if ($success !== ''): ?><div class="alert" role="status"><?=krs_e($success)?></div><?php endif; ?>
<?php if ($error !== ''): ?><div class="alert error" role="alert"><?=krs_e($error)?></div><?php endif; ?>

<?php if ($applicationId > 0 && $application): ?>
<div class="meta">
<div class="meta-item"><small>Nomor Pengajuan</small><strong>#<?=krs_e((string)$application['id_pengajuan'])?></strong></div>
<div class="meta-item"><small>Mahasiswa</small><strong><?=krs_e((string)$application['nama_mahasiswa'])?></strong></div>
<div class="meta-item"><small>Program Studi</small><strong><?=krs_e((string)($application['nama_prodi'] ?? '-'))?></strong></div>
<div class="meta-item"><small>Sesi</small><strong>Semester <?=krs_e((string)$application['semester'])?> · <?=krs_e((string)$application['tahun_ajaran'])?></strong></div>
</div>
<div class="panel">
<h2>Daftar Mata Kuliah</h2>
<p class="panel-note">Centang mata kuliah yang akan disetujui atau dikembalikan untuk revisi. Setiap keputusan disimpan pada mata kuliah yang dipilih.</p>
<form method="post" action="review.php?id=<?=$applicationId?>">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="id_pengajuan" value="<?=$applicationId?>">
<div class="table-scroll">
<table>
<thead><tr><th>Pilih</th><th>ID Detail</th><th>Mata Kuliah</th><th>SKS</th><th>Kelas</th><th>Hari</th><th>Jam</th><th>Ruang</th><th>Status</th><th>Catatan Revisi</th></tr></thead>
<tbody>
<?php if (!$detailRows): ?><tr><td colspan="10">Pengajuan ini belum memiliki mata kuliah.</td></tr>
<?php else: foreach ($detailRows as $row): ?>
<tr>
<td><input type="checkbox" name="detail_ids[]" value="<?=krs_e((string)$row['id_detail'])?>" <?=($canAct && $row['status_acc'] === 'Ditinjau' ? '' : 'disabled')?> aria-label="Pilih <?=krs_e((string)$row['nama_matkul'])?>"></td>
<td><?=krs_e((string)$row['id_detail'])?></td>
<td><?=krs_e((string)$row['nama_matkul'])?></td>
<td><?=krs_e((string)$row['sks'])?></td>
<td><?=krs_e((string)($row['nama_kelas'] ?? '-'))?></td>
<td><?=krs_e(ucfirst((string)$row['hari']))?></td>
<td><?=krs_e((string)$row['jam_mulai'])?>–<?=krs_e((string)$row['jam_selesai'])?></td>
<td><?=krs_e((string)($row['nama_ruang'] ?? '-'))?></td>
<td><span class="status <?=krs_review_status_class((string)$row['status_acc'])?>"><?=krs_e($statusLabels[$row['status_acc']] ?? (string)$row['status_acc'])?></span></td>
<td><?=krs_e((string)($row['catatan_revisi'] ?? '-'))?></td>
</tr>
<?php endforeach; endif; ?>
</tbody>
</table>
</div>
<?php if ($canAct): ?>
<div class="action-row">
<button class="btn primary" type="submit" name="action" value="approve_selected">Setujui yang Dipilih</button>
<button class="btn reject" type="submit" name="action" value="reject_selected">Revisi yang Dipilih</button>
</div>
<div style="margin-top:12px">
<label for="catatan_revisi">Catatan revisi (wajib diisi saat memilih Revisi)</label>
<textarea id="catatan_revisi" name="catatan_revisi" maxlength="1000" placeholder="Tuliskan alasan revisi yang perlu diperbaiki mahasiswa..."></textarea>
<p class="small">Catatan akan disimpan pada setiap mata kuliah yang dipilih untuk revisi.</p>
</div>
<?php endif; ?>
</form>
</div>

<div class="panel">
<h2>Penyelesaian Review</h2>
<p class="panel-note"><?=count($detailRows)?> mata kuliah · <?=$pendingCount?> masih menunggu keputusan · <?=$rejectedCount?> perlu revisi.</p>
<?php if ($canAct && $pendingCount === 0 && count($detailRows) > 0): ?>
<form method="post" action="review.php?id=<?=$applicationId?>" onsubmit="return confirm('Selesaikan review ini dan kirim hasilnya ke tahap berikutnya?')">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="id_pengajuan" value="<?=$applicationId?>">
<input type="hidden" name="action" value="finish_review">
<button class="btn primary" type="submit">Selesaikan Review</button>
</form>
<?php elseif ($canAct): ?>
<p class="small">Semua mata kuliah harus diberi keputusan Setujui atau Revisi sebelum review dapat diselesaikan.</p>
<?php else: ?>
<p class="small">Pengajuan ini tidak lagi dalam status review aktif dan hanya dapat dilihat.</p>
<?php endif; ?>
</div>
<?php else: ?>
<div class="panel">
<h2>Antrean Pengajuan KRS</h2>
<p class="panel-note">Hanya pengajuan berstatus Ditinjau dari mahasiswa yang ditugaskan kepada Anda dan masih dalam periode KRS aktif yang ditampilkan.</p>
<div class="table-scroll">
<table>
<thead><tr><th>ID Pengajuan</th><th>Nama Mahasiswa</th><th>Program Studi</th><th>Semester</th><th>Tahun Ajaran</th><th>Tanggal Pengajuan</th><th>Jumlah Matkul</th><th>Total SKS</th><th>Aksi</th></tr></thead>
<tbody>
<?php if (!$queue): ?><tr><td colspan="9">Belum ada pengajuan KRS yang menunggu review.</td></tr>
<?php else: foreach ($queue as $item): ?>
<tr>
<td>#<?=krs_e((string)$item['id_pengajuan'])?></td>
<td><?=krs_e((string)$item['nama_mahasiswa'])?></td>
<td><?=krs_e((string)($item['nama_prodi'] ?? '-'))?></td>
<td><?=krs_e((string)$item['semester'])?></td>
<td><?=krs_e((string)$item['tahun_ajaran'])?></td>
<td><?=krs_e((string)$item['tanggal_pengajuan'])?></td>
<td><?=krs_e((string)$item['jumlah_matkul'])?></td>
<td><?=krs_e((string)$item['total_sks'])?></td>
<td><a class="btn primary" href="review.php?id=<?=krs_e((string)$item['id_pengajuan'])?>">Lihat Detail</a></td>
</tr>
<?php endforeach; endif; ?>
</tbody>
</table>
</div>
</div>
<?php endif; ?>

<p class="footer">Status Disetujui Dosen PA diteruskan kepada Kaprodi. Jika ada mata kuliah yang direvisi, pengajuan dikembalikan kepada Mahasiswa.</p>
</div>
</main>
</body>
</html>
