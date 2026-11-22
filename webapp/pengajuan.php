<?php
declare(strict_types=1);

require __DIR__ . '/config.php';

$account = $_SESSION['krs_user'] ?? null;
if (!is_array($account)) {
    header('Location: index.php?notice=login-required', true, 302);
    exit;
}
if (($account['role'] ?? '') !== 'Mahasiswa') {
    http_response_code(403);
    echo 'Akses ditolak. Pengajuan KRS hanya dapat digunakan oleh Mahasiswa.';
    exit;
}

$pdo = krs_db();
$studentQuery = $pdo->prepare(
    'SELECT id_mahasiswa, id_prodi, id_dosen_pa, nama_mahasiswa, semester
     FROM mahasiswa WHERE id_mahasiswa = ? LIMIT 1'
);
$studentQuery->execute([(int)$account['id']]);
$student = $studentQuery->fetch();
if (!$student) {
    unset($_SESSION['krs_user']);
    session_regenerate_id(true);
    header('Location: index.php?notice=login-required', true, 302);
    exit;
}

$studentId = (int)$student['id_mahasiswa'];
$programId = (int)$student['id_prodi'];
$studentSemester = (int)$student['semester'];
$advisorId = (int)($student['id_dosen_pa'] ?? 0);
$advisorAssigned = $advisorId > 0;
$detailId = filter_input(INPUT_GET, 'detail', FILTER_VALIDATE_INT) ?: 0;
$error = '';
$success = (string)($_SESSION['krs_application_flash'] ?? '');
unset($_SESSION['krs_application_flash']);

function krs_application_for_student(PDO $pdo, int $applicationId, int $studentId): ?array
{
    $query = $pdo->prepare(
        'SELECT pk.id_pengajuan, pk.id_krs, pk.id_mahasiswa, pk.tanggal_pengajuan,
                pk.status_acc, pk.catatan_revisi, k.semester, k.tahun_ajaran,
                k.status_krs, k.tanggal_mulai, k.tanggal_selesai,
                d.nama_dosen AS nama_dosen_pa
         FROM pengajuan_krs pk
         JOIN krs k ON k.id_krs = pk.id_krs
         JOIN mahasiswa m ON m.id_mahasiswa = pk.id_mahasiswa
         LEFT JOIN dosen d ON d.id_dosen = m.id_dosen_pa
         WHERE pk.id_pengajuan = ? AND pk.id_mahasiswa = ?
         LIMIT 1'
    );
    $query->execute([$applicationId, $studentId]);
    $row = $query->fetch();
    return $row ?: null;
}

function krs_application_is_open(PDO $pdo, int $sessionId): bool
{
    $query = $pdo->prepare(
        "SELECT COUNT(*) FROM krs
         WHERE id_krs = ? AND status_krs = 'terbuka'
           AND CURRENT_DATE BETWEEN tanggal_mulai AND tanggal_selesai"
    );
    $query->execute([$sessionId]);
    return (int)$query->fetchColumn() === 1;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $knownToken = (string)($_SESSION['csrf_token'] ?? '');
    $sentToken = (string)($_POST['csrf_token'] ?? '');
    if ($knownToken === '' || !hash_equals($knownToken, $sentToken)) {
        http_response_code(403);
        $error = 'Sesi formulir tidak valid. Muat ulang halaman dan coba lagi.';
    } elseif (!$advisorAssigned) {
        $error = 'Dosen Pembimbing Akademik belum ditentukan. Hubungi Admin untuk penugasan.';
    } else {
        $action = (string)($_POST['action'] ?? '');
        $applicationId = filter_var($_POST['id_pengajuan'] ?? '', FILTER_VALIDATE_INT);
        if ($applicationId === false || $applicationId === null || $applicationId < 1) {
            $applicationId = 0;
        }

        try {
            if ($action === 'create') {
                $sessionId = filter_var($_POST['id_krs'] ?? '', FILTER_VALIDATE_INT);
                if ($sessionId === false || $sessionId === null || $sessionId < 1) {
                    $error = 'Pilih sesi KRS yang valid.';
                } else {
                    $sessionCheck = $pdo->prepare(
                        "SELECT id_krs FROM krs
                         WHERE id_krs = ? AND id_prodi = ? AND semester = ?
                           AND status_krs = 'terbuka'
                           AND CURRENT_DATE BETWEEN tanggal_mulai AND tanggal_selesai
                         LIMIT 1"
                    );
                    $sessionCheck->execute([$sessionId, $programId, $studentSemester]);
                    if (!$sessionCheck->fetch()) {
                        $error = 'Sesi tersebut tidak sedang dibuka untuk program studi dan semester Anda.';
                    } else {
                        $duplicate = $pdo->prepare('SELECT id_pengajuan FROM pengajuan_krs WHERE id_krs = ? AND id_mahasiswa = ? LIMIT 1');
                        $duplicate->execute([$sessionId, $studentId]);
                        if ($duplicate->fetch()) {
                            $error = 'Anda sudah memiliki pengajuan untuk sesi KRS tersebut.';
                        } else {
                            $insert = $pdo->prepare(
                                "INSERT INTO pengajuan_krs (id_krs, id_mahasiswa, tanggal_pengajuan, status_acc)
                                 VALUES (?, ?, CURRENT_DATE, 'Draft')"
                            );
                            $insert->execute([$sessionId, $studentId]);
                            $newId = (int)$pdo->lastInsertId();
                            $_SESSION['krs_application_flash'] = 'Draft pengajuan KRS berhasil dibuat.';
                            header('Location: pengajuan.php?detail=' . $newId, true, 303);
                            exit;
                        }
                    }
                }
            } else {
                $application = $applicationId > 0
                    ? krs_application_for_student($pdo, $applicationId, $studentId)
                    : null;
                if (!$application) {
                    http_response_code(404);
                    $error = 'Pengajuan KRS tidak ditemukan.';
                } else {
                    $detailId = $applicationId;
                    $editable = $application['status_acc'] === 'Draft'
                        && krs_application_is_open($pdo, (int)$application['id_krs']);

                    if ($action === 'resume') {
                        if ($application['status_acc'] !== 'Ditolak'
                            || !krs_application_is_open($pdo, (int)$application['id_krs'])) {
                            $error = 'Pengajuan ini tidak dapat dikembalikan ke Draft.';
                        } else {
                            $pdo->beginTransaction();
                            $update = $pdo->prepare(
                                "UPDATE pengajuan_krs SET status_acc = 'Draft', catatan_revisi = NULL
                                 WHERE id_pengajuan = ? AND id_mahasiswa = ? AND status_acc = 'Ditolak'"
                            );
                            $update->execute([$applicationId, $studentId]);
                            $reset = $pdo->prepare(
                                "UPDATE krs_detail SET status_acc = 'Draft', catatan_revisi = ''
                                 WHERE id_pengajuan = ? AND status_acc = 'Ditolak'"
                            );
                            $reset->execute([$applicationId]);
                            $pdo->commit();
                            $_SESSION['krs_application_flash'] = 'Pengajuan dikembalikan ke Draft. Periksa catatan revisi sebelum mengirim ulang.';
                            header('Location: pengajuan.php?detail=' . $applicationId, true, 303);
                            exit;
                        }
                    } elseif ($action === 'add_course') {
                        if (!$editable) {
                            $error = 'Pengajuan ini tidak lagi dalam status Draft atau sesi KRS sudah ditutup.';
                        } else {
                            $scheduleId = filter_var($_POST['id_jadwal'] ?? '', FILTER_VALIDATE_INT);
                            if ($scheduleId === false || $scheduleId === null || $scheduleId < 1) {
                                $error = 'Pilih jadwal mata kuliah yang valid.';
                            } else {
                                $pdo->beginTransaction();
                                $lock = $pdo->prepare(
                                    'SELECT pk.status_acc, k.status_krs, k.tanggal_mulai, k.tanggal_selesai
                                     FROM pengajuan_krs pk JOIN krs k ON k.id_krs = pk.id_krs
                                     WHERE pk.id_pengajuan = ? AND pk.id_mahasiswa = ? FOR UPDATE'
                                );
                                $lock->execute([$applicationId, $studentId]);
                                $locked = $lock->fetch();
                                $windowOpen = $locked
                                    && $locked['status_acc'] === 'Draft'
                                    && $locked['status_krs'] === 'terbuka'
                                    && $pdo->query('SELECT CURRENT_DATE')->fetchColumn() >= $locked['tanggal_mulai']
                                    && $pdo->query('SELECT CURRENT_DATE')->fetchColumn() <= $locked['tanggal_selesai'];

                                $scheduleQuery = $pdo->prepare(
                                    'SELECT j.id_jadwal, j.id_matkul, j.hari, j.jam_mulai, j.jam_selesai,
                                            m.nama_matkul, m.sks, m.id_prodi, m.semester
                                     FROM jadwal j JOIN matkul m ON m.id_matkul = j.id_matkul
                                     WHERE j.id_jadwal = ? AND m.id_prodi = ? AND m.semester = ?
                                     LIMIT 1'
                                );
                                $scheduleQuery->execute([$scheduleId, $programId, $studentSemester]);
                                $schedule = $scheduleQuery->fetch();

                                if (!$windowOpen) {
                                    $pdo->rollBack();
                                    $error = 'Pengajuan sudah dikunci atau masa pengisian KRS berakhir.';
                                } elseif (!$schedule) {
                                    $pdo->rollBack();
                                    $error = 'Jadwal tidak tersedia untuk program studi dan semester Anda.';
                                } else {
                                    $duplicateCourse = $pdo->prepare(
                                        'SELECT COUNT(*) FROM krs_detail kd
                                         JOIN jadwal j ON j.id_jadwal = kd.id_jadwal
                                         WHERE kd.id_pengajuan = ? AND j.id_matkul = ?'
                                    );
                                    $duplicateCourse->execute([$applicationId, (int)$schedule['id_matkul']]);
                                    if ((int)$duplicateCourse->fetchColumn() > 0) {
                                        $pdo->rollBack();
                                        $error = 'Mata kuliah tersebut sudah ada di dalam pengajuan KRS.';
                                    } else {
                                        $conflict = $pdo->prepare(
                                            'SELECT COUNT(*) FROM krs_detail kd
                                             JOIN jadwal j ON j.id_jadwal = kd.id_jadwal
                                             WHERE kd.id_pengajuan = ? AND LOWER(j.hari) = LOWER(?)
                                               AND j.jam_mulai < ? AND j.jam_selesai > ?'
                                        );
                                        $conflict->execute([
                                            $applicationId,
                                            $schedule['hari'],
                                            $schedule['jam_selesai'],
                                            $schedule['jam_mulai'],
                                        ]);
                                        if ((int)$conflict->fetchColumn() > 0) {
                                            $pdo->rollBack();
                                            $error = 'Jadwal mata kuliah bertabrakan dengan mata kuliah lain di hari dan jam yang sama.';
                                        } else {
                                            $totalQuery = $pdo->prepare(
                                                'SELECT COALESCE(SUM(m.sks), 0)
                                                 FROM krs_detail kd
                                                 JOIN jadwal j ON j.id_jadwal = kd.id_jadwal
                                                 JOIN matkul m ON m.id_matkul = j.id_matkul
                                                 WHERE kd.id_pengajuan = ?'
                                            );
                                            $totalQuery->execute([$applicationId]);
                                            $totalSks = (int)$totalQuery->fetchColumn();
                                            $courseSks = (int)$schedule['sks'];
                                            if ($totalSks + $courseSks > 24) {
                                                $pdo->rollBack();
                                                $error = 'Batas maksimal 24 SKS terlampaui.';
                                            } else {
                                                $insert = $pdo->prepare(
                                                    "INSERT INTO krs_detail (id_pengajuan, id_jadwal, status_acc)
                                                     VALUES (?, ?, 'Draft')"
                                                );
                                                $insert->execute([$applicationId, $scheduleId]);
                                                $pdo->commit();
                                                $_SESSION['krs_application_flash'] = 'Mata kuliah berhasil ditambahkan ke pengajuan.';
                                                header('Location: pengajuan.php?detail=' . $applicationId, true, 303);
                                                exit;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } elseif ($action === 'remove_course') {
                        if (!$editable) {
                            $error = 'Mata kuliah hanya dapat dihapus saat pengajuan masih Draft dan sesi KRS dibuka.';
                        } else {
                            $detailRowId = filter_var($_POST['id_detail'] ?? '', FILTER_VALIDATE_INT);
                            if ($detailRowId === false || $detailRowId === null || $detailRowId < 1) {
                                $error = 'Pilih mata kuliah yang valid.';
                            } else {
                                $delete = $pdo->prepare(
                                    'DELETE kd FROM krs_detail kd
                                     JOIN pengajuan_krs pk ON pk.id_pengajuan = kd.id_pengajuan
                                     JOIN krs k ON k.id_krs = pk.id_krs
                                     WHERE kd.id_detail = ? AND kd.id_pengajuan = ?
                                       AND pk.id_mahasiswa = ? AND pk.status_acc = \'Draft\'
                                       AND k.status_krs = \'terbuka\'
                                       AND CURRENT_DATE BETWEEN k.tanggal_mulai AND k.tanggal_selesai'
                                );
                                $delete->execute([$detailRowId, $applicationId, $studentId]);
                                if ($delete->rowCount() > 0) {
                                    $_SESSION['krs_application_flash'] = 'Mata kuliah dihapus dari pengajuan.';
                                    header('Location: pengajuan.php?detail=' . $applicationId, true, 303);
                                    exit;
                                }
                                $error = 'Mata kuliah tidak ditemukan atau tidak dapat dihapus.';
                            }
                        }
                    } elseif ($action === 'lock') {
                        if (!$editable) {
                            $error = 'Pengajuan tidak dapat dikunci karena bukan Draft atau sesi KRS telah ditutup.';
                        } else {
                            $pdo->beginTransaction();
                            $lock = $pdo->prepare(
                                'SELECT pk.status_acc, k.status_krs, k.tanggal_mulai, k.tanggal_selesai
                                 FROM pengajuan_krs pk JOIN krs k ON k.id_krs = pk.id_krs
                                 WHERE pk.id_pengajuan = ? AND pk.id_mahasiswa = ? FOR UPDATE'
                            );
                            $lock->execute([$applicationId, $studentId]);
                            $locked = $lock->fetch();
                            $today = (string)$pdo->query('SELECT CURRENT_DATE')->fetchColumn();
                            $stillOpen = $locked && $locked['status_acc'] === 'Draft'
                                && $locked['status_krs'] === 'terbuka'
                                && $today >= $locked['tanggal_mulai'] && $today <= $locked['tanggal_selesai'];
                            $count = $pdo->prepare('SELECT COUNT(*) FROM krs_detail WHERE id_pengajuan = ?');
                            $count->execute([$applicationId]);
                            if (!$stillOpen) {
                                $pdo->rollBack();
                                $error = 'Sesi KRS sudah tidak terbuka.';
                            } elseif ((int)$count->fetchColumn() < 1) {
                                $pdo->rollBack();
                                $error = 'Tambahkan minimal satu mata kuliah sebelum mengunci KRS.';
                            } else {
                                $update = $pdo->prepare(
                                    "UPDATE pengajuan_krs SET status_acc = 'Ditinjau'
                                     WHERE id_pengajuan = ? AND id_mahasiswa = ? AND status_acc = 'Draft'"
                                );
                                $update->execute([$applicationId, $studentId]);
                                $updateDetails = $pdo->prepare(
                                    "UPDATE krs_detail SET status_acc = 'Ditinjau'
                                     WHERE id_pengajuan = ? AND status_acc = 'Draft'"
                                );
                                $updateDetails->execute([$applicationId]);
                                $pdo->commit();
                                $_SESSION['krs_application_flash'] = 'KRS berhasil dikunci dan dikirim untuk ditinjau Dosen PA.';
                                header('Location: pengajuan.php?detail=' . $applicationId, true, 303);
                                exit;
                            }
                        }
                    } elseif ($action === 'delete_request') {
                        if (!in_array($application['status_acc'], ['Draft', 'Ditolak'], true)
                            || !krs_application_is_open($pdo, (int)$application['id_krs'])) {
                            $error = 'Hanya pengajuan Draft atau Ditolak pada masa pengisian aktif yang dapat dihapus.';
                        } else {
                            $pdo->beginTransaction();
                            $deleteDetails = $pdo->prepare('DELETE FROM krs_detail WHERE id_pengajuan = ?');
                            $deleteDetails->execute([$applicationId]);
                            $deleteRequest = $pdo->prepare(
                                'DELETE FROM pengajuan_krs WHERE id_pengajuan = ? AND id_mahasiswa = ?'
                            );
                            $deleteRequest->execute([$applicationId, $studentId]);
                            $pdo->commit();
                            $_SESSION['krs_application_flash'] = 'Pengajuan KRS berhasil dihapus.';
                            header('Location: pengajuan.php', true, 303);
                            exit;
                        }
                    } else {
                        $error = 'Aksi pengajuan tidak dikenal.';
                    }
                }
            }
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            error_log('KRS student application error: ' . $exception->getMessage());
            $error = 'Perubahan pengajuan KRS gagal diproses. Periksa status sesi dan data jadwal.';
        }
    }
}

$activeSessions = [];
$history = [];
$request = null;
$detailRows = [];
$availableSchedules = [];
$totalSks = 0;
$canEdit = false;
$canResume = false;
$canChangeRequest = false;

if (!$advisorAssigned) {
    $error = $error !== '' ? $error : 'Dosen Pembimbing Akademik belum ditentukan. Hubungi Admin untuk penugasan.';
} else {
    $sessionsQuery = $pdo->prepare(
        "SELECT k.id_krs, p.nama_prodi, k.semester, k.tahun_ajaran,
                k.tanggal_mulai, k.tanggal_selesai
         FROM krs k LEFT JOIN prodi p ON p.id_prodi = k.id_prodi
         WHERE k.id_prodi = ? AND k.semester = ?
           AND k.status_krs = 'terbuka'
           AND CURRENT_DATE BETWEEN k.tanggal_mulai AND k.tanggal_selesai
         ORDER BY k.tanggal_selesai ASC"
    );
    $sessionsQuery->execute([$programId, $studentSemester]);
    $activeSessions = $sessionsQuery->fetchAll();
}

$historyQuery = $pdo->prepare(
    'SELECT pk.id_pengajuan, pk.id_krs, pk.tanggal_pengajuan, pk.status_acc,
            pk.catatan_revisi, k.semester, k.tahun_ajaran, k.status_krs,
            k.tanggal_mulai, k.tanggal_selesai, d.nama_dosen AS nama_dosen_pa,
            (SELECT COUNT(*) FROM krs_detail kd WHERE kd.id_pengajuan = pk.id_pengajuan) AS jumlah_matkul,
            (SELECT COALESCE(SUM(m.sks), 0)
             FROM krs_detail kd JOIN jadwal j ON j.id_jadwal = kd.id_jadwal
             JOIN matkul m ON m.id_matkul = j.id_matkul
             WHERE kd.id_pengajuan = pk.id_pengajuan) AS total_sks
     FROM pengajuan_krs pk
     JOIN krs k ON k.id_krs = pk.id_krs
     LEFT JOIN dosen d ON d.id_dosen = ? 
     WHERE pk.id_mahasiswa = ?
     ORDER BY pk.id_pengajuan DESC'
);
$historyQuery->execute([$advisorId, $studentId]);
$history = $historyQuery->fetchAll();

if ($detailId > 0) {
    $request = krs_application_for_student($pdo, $detailId, $studentId);
    if (!$request) {
        http_response_code(404);
        $error = 'Pengajuan KRS tidak ditemukan.';
    } else {
        $canEdit = $request['status_acc'] === 'Draft'
            && krs_application_is_open($pdo, (int)$request['id_krs']);
        $canResume = $request['status_acc'] === 'Ditolak'
            && krs_application_is_open($pdo, (int)$request['id_krs']);
        $canChangeRequest = in_array($request['status_acc'], ['Draft', 'Ditolak'], true)
            && krs_application_is_open($pdo, (int)$request['id_krs']);

        $detailsQuery = $pdo->prepare(
            'SELECT kd.id_detail, kd.status_acc, kd.catatan_revisi,
                    j.id_jadwal, j.hari, j.jam_mulai, j.jam_selesai,
                    m.nama_matkul, m.sks, k.nama_kelas, r.nama_ruang
             FROM krs_detail kd
             JOIN jadwal j ON j.id_jadwal = kd.id_jadwal
             JOIN matkul m ON m.id_matkul = j.id_matkul
             LEFT JOIN kelas k ON k.id_kelas = j.kelas
             LEFT JOIN ruang r ON r.id_ruang = j.ruang
             WHERE kd.id_pengajuan = ?
             ORDER BY FIELD(LOWER(j.hari), \'senin\', \'selasa\', \'rabu\', \'kamis\', \'jumat\', \'sabtu\', \'minggu\'), j.jam_mulai'
        );
        $detailsQuery->execute([$detailId]);
        $detailRows = $detailsQuery->fetchAll();

        $totalQuery = $pdo->prepare(
            'SELECT COALESCE(SUM(m.sks), 0)
             FROM krs_detail kd JOIN jadwal j ON j.id_jadwal = kd.id_jadwal
             JOIN matkul m ON m.id_matkul = j.id_matkul
             WHERE kd.id_pengajuan = ?'
        );
        $totalQuery->execute([$detailId]);
        $totalSks = (int)$totalQuery->fetchColumn();

        if ($canEdit) {
            $scheduleQuery = $pdo->prepare(
                'SELECT j.id_jadwal, j.hari, j.jam_mulai, j.jam_selesai,
                        j.kelas, j.ruang, m.nama_matkul, m.sks,
                        k.nama_kelas, r.nama_ruang, d.nama_dosen,
                        EXISTS(SELECT 1 FROM krs_detail kd
                               WHERE kd.id_pengajuan = ? AND kd.id_jadwal = j.id_jadwal) AS sudah_dipilih
                 FROM jadwal j
                 JOIN matkul m ON m.id_matkul = j.id_matkul
                 LEFT JOIN kelas k ON k.id_kelas = j.kelas
                 LEFT JOIN ruang r ON r.id_ruang = j.ruang
                 LEFT JOIN dosen d ON d.id_dosen = j.id_dosen
                 WHERE m.id_prodi = ? AND m.semester = ?
                 ORDER BY FIELD(LOWER(j.hari), \'senin\', \'selasa\', \'rabu\', \'kamis\', \'jumat\', \'sabtu\', \'minggu\'), j.jam_mulai'
            );
            $scheduleQuery->execute([$detailId, $programId, $studentSemester]);
            $availableSchedules = $scheduleQuery->fetchAll();
        }
    }
}

$statusLabels = [
    'Draft' => 'Draft',
    'Ditinjau' => 'Menunggu Tinjauan',
    'Ditolak' => 'Perlu Revisi',
    'AccDosenPA' => 'Disetujui Dosen PA',
    'Disetujui' => 'Disetujui',
];
function krs_status_class(string $status): string
{
    return match ($status) {
        'Draft' => 'draft',
        'Ditinjau' => 'review',
        'Ditolak' => 'rejected',
        'AccDosenPA', 'Disetujui' => 'approved',
        default => 'neutral',
    };
}
?>
<!doctype html>
<html lang="id">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title><?=($detailId > 0 ? 'Detail Pengajuan KRS' : 'Pengajuan KRS')?> — KRS</title>
<style>
*{box-sizing:border-box}html,body{margin:0;min-height:100%;font-family:"Segoe UI",Arial,sans-serif;color:#303735}
body{background:#d9ddd8;padding:18px}
.canvas{position:relative;width:min(1024px,100%);margin:0 auto;min-height:690px;box-shadow:0 12px 38px #24332f42;background:#d9d5bb}
.bg{position:absolute;inset:0;width:100%;height:100%;object-fit:fill;user-select:none}
.content{position:relative;z-index:1;padding:46px 7% 34px}
.heading{display:flex;align-items:center;justify-content:space-between;gap:14px;margin:0 0 18px}
.heading h1{margin:0;color:#263d38;font-size:clamp(21px,3vw,30px)}
.heading p{margin:4px 0 0;color:#5a6964;font-size:13px}
.back{padding:9px 15px;border:1px solid #69817a;background:#f7f7f0;color:#31514a;text-decoration:none;font-weight:700}
.panel{background:#ffffffd9;border:1px solid #a4b8b0;border-radius:6px;padding:14px 16px;margin:0 0 17px;box-shadow:0 2px 7px #2a443a14}
.panel h2{margin:0 0 10px;color:#20675f;font-size:17px}
.panel-note{margin:0 0 10px;color:#63716c;font-size:12px}
.table-scroll{overflow:auto;max-width:100%}
table{width:100%;border-collapse:collapse;font-size:13px;background:#fff}
th{background:#0b7068;color:#fff;text-align:left;padding:10px 9px;white-space:nowrap}
td{padding:9px;border-bottom:1px solid #dfe6e2;color:#33413d;vertical-align:middle}
tbody tr:nth-child(even){background:#f1f5f3}
tbody tr:hover{background:#fff5dc}
.status{display:inline-block;padding:4px 9px;border-radius:14px;font-size:11px;font-weight:700;white-space:nowrap}
.status.draft{background:#e9eef0;color:#435a61}.status.review{background:#fff0c8;color:#86580b}
.status.rejected{background:#fde5e2;color:#a03229}.status.approved{background:#d8f0e4;color:#216b48}
.status.neutral{background:#e5eae8;color:#53645e}
.btn{display:inline-flex;align-items:center;justify-content:center;padding:8px 12px;border:1px solid #607d75;background:#f8f8f3;color:#24514a;text-decoration:none;font-size:12px;font-weight:700;cursor:pointer}
.btn.primary{background:#0b7068;color:#fff;border-color:#0b7068}.btn.danger{color:#9e352d;border-color:#c38c87;background:#fff3f1}
.btn:disabled{opacity:.45;cursor:not-allowed}
.inline-form{display:inline;margin:0}
.alert{padding:10px 13px;margin:0 0 15px;background:#eef8f1;border:1px solid #b8d6c3;color:#265c40;font-size:13px}
.alert.error{background:#fff0ed;border-color:#dfb8ae;color:#8c3428}
.action-row{display:flex;flex-wrap:wrap;align-items:center;gap:8px}
.meta{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-bottom:15px}
.meta-item{background:#ffffffd9;border:1px solid #b1c3bc;padding:9px 11px}
.meta-item small{display:block;color:#6c7974;font-size:11px}.meta-item strong{display:block;color:#2a4b44;font-size:14px;margin-top:3px}
.note{font-size:12px;color:#66736e;margin:9px 0 0}.revision{margin:0 0 15px;background:#fff0ed;border:1px solid #dfb8ae;padding:11px 13px;color:#87362e}
.total{display:flex;justify-content:flex-end;gap:10px;align-items:center;margin:12px 0 0;color:#31544d;font-weight:700}
@media(max-width:720px){body{padding:0}.canvas{min-height:100vh}.content{padding:22px 14px}.heading{align-items:flex-start}.meta{grid-template-columns:1fr 1fr}.panel{padding:11px}.table-scroll table{min-width:650px}}
</style>
</head>
<body>
<main class="canvas">
<img class="bg" src="<?=($detailId > 0 ? 'https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Detail%20KRS.png' : 'https://raw.githubusercontent.com/SamuelPhe/krs_pbo2_semester4/master/src/main/resources/desain/Rectangle%20(right).png')?>" alt="">
<div class="content">
<header class="heading">
<div><h1><?=($detailId > 0 ? 'Detail Pengajuan KRS' : 'Pengajuan KRS Mahasiswa')?></h1><p><?=krs_e((string)$student['nama_mahasiswa'])?> · <?=krs_e((string)$studentSemester)?> semester · ID <?=krs_e((string)$studentId)?></p></div>
<a class="back" href="<?=($detailId > 0 ? 'pengajuan.php' : 'index.php')?>">Back</a>
</header>
<?php if ($success !== ''): ?><div class="alert" role="status"><?=krs_e($success)?></div><?php endif; ?>
<?php if ($error !== ''): ?><div class="alert error" role="alert"><?=krs_e($error)?></div><?php endif; ?>

<?php if ($detailId > 0 && $request): ?>
<div class="meta">
<div class="meta-item"><small>Nomor Pengajuan</small><strong>#<?=krs_e((string)$request['id_pengajuan'])?></strong></div>
<div class="meta-item"><small>Sesi KRS</small><strong>Semester <?=krs_e((string)$request['semester'])?> · <?=krs_e((string)$request['tahun_ajaran'])?></strong></div>
<div class="meta-item"><small>Dosen PA</small><strong><?=krs_e((string)($request['nama_dosen_pa'] ?? 'Belum diatur'))?></strong></div>
<div class="meta-item"><small>Status Pengajuan</small><strong><span class="status <?=krs_status_class((string)$request['status_acc'])?>"><?=krs_e($statusLabels[$request['status_acc']] ?? (string)$request['status_acc'])?></span></strong></div>
</div>

<?php if ($request['status_acc'] === 'Ditolak' && (string)$request['catatan_revisi'] !== ''): ?>
<div class="revision"><strong>Catatan revisi:</strong> <?=krs_e((string)$request['catatan_revisi'])?></div>
<?php endif; ?>

<section class="panel">
<h2>Mata Kuliah dalam Pengajuan</h2>
<div class="table-scroll">
<table>
<thead><tr><th>Mata Kuliah</th><th>SKS</th><th>Kelas</th><th>Hari</th><th>Jam</th><th>Ruang</th><th>Status</th><th>Catatan Revisi</th><?php if ($canEdit): ?><th>Aksi</th><?php endif; ?></tr></thead>
<tbody>
<?php if (!$detailRows): ?><tr><td colspan="<?=($canEdit ? 9 : 8)?>">Belum ada mata kuliah yang ditambahkan.</td></tr>
<?php else: foreach ($detailRows as $row): ?>
<tr>
<td><?=krs_e((string)$row['nama_matkul'])?></td><td><?=krs_e((string)$row['sks'])?></td>
<td><?=krs_e((string)($row['nama_kelas'] ?? $row['kelas'] ?? '-'))?></td>
<td><?=krs_e(ucfirst((string)$row['hari']))?></td>
<td><?=krs_e((string)$row['jam_mulai'])?>–<?=krs_e((string)$row['jam_selesai'])?></td>
<td><?=krs_e((string)($row['nama_ruang'] ?? '-'))?></td>
<td><span class="status <?=krs_status_class((string)$row['status_acc'])?>"><?=krs_e((string)$row['status_acc'])?></span></td>
<td><?=krs_e((string)($row['catatan_revisi'] ?? '') !== '' ? (string)$row['catatan_revisi'] : '-')?></td>
<?php if ($canEdit): ?><td><form class="inline-form" method="post" action="pengajuan.php?detail=<?=$detailId?>">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="action" value="remove_course"><input type="hidden" name="id_pengajuan" value="<?=$detailId?>">
<input type="hidden" name="id_detail" value="<?=krs_e((string)$row['id_detail'])?>">
<button class="btn danger" type="submit">Hapus</button></form></td><?php endif; ?>
</tr>
<?php endforeach; endif; ?>
</tbody>
</table>
</div>
<div class="total">Total SKS: <?=krs_e((string)$totalSks)?> / 24</div>
</section>

<?php if ($canEdit): ?>
<section class="panel">
<h2>Pilih Jadwal Mata Kuliah</h2>
<p class="panel-note">Jadwal yang tersedia mengikuti program studi dan semester Anda. Satu mata kuliah tidak dapat dipilih dua kali.</p>
<div class="table-scroll">
<table>
<thead><tr><th>Mata Kuliah</th><th>SKS</th><th>Kelas</th><th>Hari</th><th>Jam</th><th>Dosen</th><th>Ruang</th><th>Aksi</th></tr></thead>
<tbody>
<?php if (!$availableSchedules): ?><tr><td colspan="8">Belum ada jadwal yang tersedia untuk semester ini.</td></tr>
<?php else: foreach ($availableSchedules as $schedule): ?>
<tr>
<td><?=krs_e((string)$schedule['nama_matkul'])?></td><td><?=krs_e((string)$schedule['sks'])?></td>
<td><?=krs_e((string)($schedule['nama_kelas'] ?? '-'))?></td>
<td><?=krs_e(ucfirst((string)$schedule['hari']))?></td>
<td><?=krs_e((string)$schedule['jam_mulai'])?>–<?=krs_e((string)$schedule['jam_selesai'])?></td>
<td><?=krs_e((string)($schedule['nama_dosen'] ?? '-'))?></td>
<td><?=krs_e((string)($schedule['nama_ruang'] ?? '-'))?></td>
<td>
<?php if ((int)$schedule['sudah_dipilih'] > 0): ?><span class="status approved">Ditambahkan</span>
<?php else: ?><form method="post" action="pengajuan.php?detail=<?=$detailId?>">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="action" value="add_course"><input type="hidden" name="id_pengajuan" value="<?=$detailId?>">
<input type="hidden" name="id_jadwal" value="<?=krs_e((string)$schedule['id_jadwal'])?>">
<button class="btn primary" type="submit">Tambah</button></form><?php endif; ?>
</td>
</tr>
<?php endforeach; endif; ?>
</tbody>
</table>
</div>
<div class="action-row" style="margin-top:14px">
<form method="post" action="pengajuan.php?detail=<?=$detailId?>" onsubmit="return confirm('Kunci KRS sekarang? Setelah dikirim, mata kuliah tidak dapat diubah sampai dikembalikan untuk revisi.')">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="action" value="lock"><input type="hidden" name="id_pengajuan" value="<?=$detailId?>">
<button class="btn primary" type="submit" <?=($totalSks < 1 ? 'disabled' : '')?>>Lock KRS</button>
</form>
<span class="note">Maksimal 24 SKS. Jadwal bentrok dan mata kuliah duplikat akan ditolak.</span>
</div>
<?php elseif ($canResume): ?>
<div class="panel">
<p class="panel-note">Pengajuan dikembalikan untuk diperbaiki. Pastikan sesi KRS masih aktif sebelum mengedit.</p>
<form method="post" action="pengajuan.php?detail=<?=$detailId?>">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="action" value="resume"><input type="hidden" name="id_pengajuan" value="<?=$detailId?>">
<button class="btn primary" type="submit">Perbaiki Pengajuan</button>
</form>
</div>
<?php else: ?>
<div class="panel"><p class="panel-note">Pengajuan ini hanya dapat dilihat. Statusnya sudah dikunci atau periode KRS telah berakhir.</p></div>
<?php endif; ?>

<?php else: ?>
<div class="panel">
<h2>Daftar Sesi KRS yang Sedang Buka</h2>
<p class="panel-note">Pilih sesi sesuai program studi dan semester Anda untuk membuat draft pengajuan.</p>
<div class="table-scroll">
<table><thead><tr><th>ID Sesi</th><th>Program Studi</th><th>Semester</th><th>Tahun Ajaran</th><th>Batas Tutup</th><th>Aksi</th></tr></thead>
<tbody>
<?php if (!$advisorAssigned): ?><tr><td colspan="6">Dosen PA belum ditetapkan oleh Admin.</td></tr>
<?php elseif (!$activeSessions): ?><tr><td colspan="6">Belum ada sesi KRS yang dibuka untuk program studi dan semester Anda.</td></tr>
<?php else: foreach ($activeSessions as $session): ?>
<tr>
<td><?=krs_e((string)$session['id_krs'])?></td>
<td><?=krs_e((string)$session['nama_prodi'])?></td>
<td><?=krs_e((string)$session['semester'])?></td>
<td><?=krs_e((string)$session['tahun_ajaran'])?></td>
<td><?=krs_e((string)$session['tanggal_selesai'])?></td>
<td><form method="post" action="pengajuan.php">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="action" value="create"><input type="hidden" name="id_krs" value="<?=krs_e((string)$session['id_krs'])?>">
<button class="btn primary" type="submit">Buat Pengajuan</button></form></td>
</tr>
<?php endforeach; endif; ?>
</tbody></table>
</div>
</section>

<section class="panel">
<h2>Riwayat Pengajuan KRS</h2>
<div class="table-scroll">
<table><thead><tr><th>ID Pengajuan</th><th>Sesi</th><th>Tgl Pengajuan</th><th>Dosen PA</th><th>Matkul</th><th>Total SKS</th><th>Status</th><th>Aksi</th></tr></thead>
<tbody>
<?php if (!$history): ?><tr><td colspan="8">Belum ada riwayat pengajuan.</td></tr>
<?php else: foreach ($history as $item): ?>
<tr>
<td>#<?=krs_e((string)$item['id_pengajuan'])?></td>
<td>Sem <?=krs_e((string)$item['semester'])?> · <?=krs_e((string)$item['tahun_ajaran'])?></td>
<td><?=krs_e((string)$item['tanggal_pengajuan'])?></td>
<td><?=krs_e((string)($item['nama_dosen_pa'] ?? 'Belum diatur'))?></td>
<td><?=krs_e((string)$item['jumlah_matkul'])?></td>
<td><?=krs_e((string)$item['total_sks'])?></td>
<td><span class="status <?=krs_status_class((string)$item['status_acc'])?>"><?=krs_e($statusLabels[$item['status_acc']] ?? (string)$item['status_acc'])?></span></td>
<td><div class="action-row">
<a class="btn" href="pengajuan.php?detail=<?=krs_e((string)$item['id_pengajuan'])?>">Lihat Detail</a>
<?php if (in_array($item['status_acc'], ['Draft', 'Ditolak'], true) && krs_application_is_open($pdo, (int)$item['id_krs'])): ?>
<form class="inline-form" method="post" action="pengajuan.php" onsubmit="return confirm('Hapus pengajuan KRS ini?')">
<input type="hidden" name="csrf_token" value="<?=krs_e((string)($_SESSION['csrf_token'] ?? ''))?>">
<input type="hidden" name="action" value="delete_request">
<input type="hidden" name="id_pengajuan" value="<?=krs_e((string)$item['id_pengajuan'])?>">
<button class="btn danger" type="submit">Hapus</button>
</form>
<?php endif; ?>
</div></td>
</tr>
<?php endforeach; endif; ?>
</tbody></table>
</div>
</section>
<?php endif; ?>
</div>
</main>
</body>
</html>
