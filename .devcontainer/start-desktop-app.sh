#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

echo "[KRS] Menunggu database Codespaces..."
for attempt in $(seq 1 60); do
  if (echo > /dev/tcp/database/3306) >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

if ! (echo > /dev/tcp/database/3306) >/dev/null 2>&1; then
  echo "[KRS] Database belum siap. Periksa container database."
  exit 1
fi

echo "[KRS] Menyiapkan aplikasi Java..."
mvn -B -DskipTests package dependency:copy-dependencies

echo "[KRS] Membuka form login asli..."
nohup java -cp "target/classes:target/dependency/*" PBO_Project_2.login_form \
  >/tmp/krs-desktop.log 2>&1 </dev/null &

echo "[KRS] Aplikasi berjalan. Buka tab Ports, lalu port 6080 (Private)."
