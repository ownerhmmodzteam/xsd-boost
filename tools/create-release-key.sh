#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
command -v keytool >/dev/null 2>&1 || { echo 'keytool tidak ditemukan. Pasang JDK 17+.' >&2; exit 1; }
OUT="$ROOT/upload-key.jks"
[[ ! -e "$OUT" ]] || { echo "File sudah ada: $OUT"; exit 1; }
keytool -genkeypair -v -keystore "$OUT" -keyalg RSA -keysize 2048 -validity 10000 -alias upload
chmod 600 "$OUT" 2>/dev/null || true
echo "Keystore dibuat: $OUT"
echo 'Simpan file dan password di tempat aman. Jangan commit ke Git.'
