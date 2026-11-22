<?php
declare(strict_types=1);

$secureCookie = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off');
if (session_status() !== PHP_SESSION_ACTIVE) {
    session_name('KRS_WEB_SESSION');
    session_set_cookie_params([
        'lifetime' => 0,
        'path' => '/',
        'secure' => $secureCookie,
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    session_start();
}

function krs_db(): PDO
{
    static $pdo = null;
    if ($pdo instanceof PDO) {
        return $pdo;
    }

    $host = getenv('DB_HOST') ?: '127.0.0.1';
    $port = getenv('DB_PORT') ?: '3306';
    $name = getenv('DB_DATABASE') ?: 'krs_db';
    $user = getenv('DB_USER');
    $password = getenv('DB_PASSWORD');

    if ($user === false || $user === '') {
        throw new RuntimeException('DB_USER belum dikonfigurasi pada hosting.');
    }
    if ($password === false) {
        throw new RuntimeException('DB_PASSWORD belum dikonfigurasi pada hosting.');
    }

    $dsn = "mysql:host={$host};port={$port};dbname={$name};charset=utf8mb4";
    $pdo = new PDO($dsn, $user, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
    return $pdo;
}

function krs_password_hash(string $password): string
{
    $iterations = 120000;
    $salt = random_bytes(16);
    $derived = hash_pbkdf2('sha256', $password, $salt, $iterations, 32, true);
    return 'pbkdf2-sha256$' . $iterations . '$'
        . rtrim(base64_encode($salt), '=') . '$'
        . rtrim(base64_encode($derived), '=');
}

function krs_password_verify(string $password, string $stored): bool
{
    if (!str_starts_with($stored, 'pbkdf2-sha256$')) {
        return hash_equals($stored, $password);
    }

    $parts = explode('$', $stored);
    if (count($parts) !== 4 || $parts[0] !== 'pbkdf2-sha256') {
        return false;
    }
    $iterations = filter_var($parts[1], FILTER_VALIDATE_INT);
    if ($iterations === false || $iterations < 10000 || $iterations > 1000000) {
        return false;
    }
    $salt = base64_decode($parts[2] . str_repeat('=', (4 - strlen($parts[2]) % 4) % 4), true);
    $expected = base64_decode($parts[3] . str_repeat('=', (4 - strlen($parts[3]) % 4) % 4), true);
    if ($salt === false || $expected === false) {
        return false;
    }
    $actual = hash_pbkdf2('sha256', $password, $salt, $iterations, strlen($expected), true);
    return hash_equals($expected, $actual);
}

function krs_e(string $value): string
{
    return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

if (empty($_SESSION['csrf_token'])) {
    $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
}
