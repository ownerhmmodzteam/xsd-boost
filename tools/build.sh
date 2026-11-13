#!/usr/bin/env bash
# XSD BOOST build helper for Linux, macOS and Termux.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
GRADLE_VERSION="9.3.1"
TOOLS_DIR="$ROOT/.build-tools"
GRADLE_HOME_LOCAL="$TOOLS_DIR/gradle-$GRADLE_VERSION"
DIST_DIR="$ROOT/dist"

info(){ printf '\n[ XSD BOOST ] %s\n' "$*"; }
fail(){ printf '\n[ERROR] %s\n' "$*" >&2; exit 1; }

info "Memeriksa Java..."
command -v java >/dev/null 2>&1 || fail "Java tidak ditemukan. Pasang JDK 17 atau lebih baru (disarankan JDK 21), lalu atur JAVA_HOME/PATH."
JAVA_LINE="$(java -version 2>&1 | head -n 1)"
JAVA_MAJOR="$(printf '%s' "$JAVA_LINE" | sed -E 's/.*version "([0-9]+).*/\1/')"
[[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] || fail "Tidak dapat membaca versi Java: $JAVA_LINE"
(( JAVA_MAJOR >= 17 )) || fail "Java $JAVA_MAJOR terdeteksi. Project membutuhkan JDK 17+; gunakan JDK 21 jika tersedia."
printf 'Java: %s\n' "$JAVA_LINE"

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK_DIR" ]]; then
  for candidate in "$HOME/Android/Sdk" "$HOME/android-sdk" "${PREFIX:-/nonexistent}/share/android-sdk"; do
    if [[ -d "$candidate/platforms" ]]; then SDK_DIR="$candidate"; break; fi
  done
fi
[[ -n "$SDK_DIR" && -d "$SDK_DIR" ]] || fail "Android SDK tidak ditemukan. Atur ANDROID_HOME ke folder SDK yang berisi platforms/ dan build-tools/."
export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
printf 'Android SDK: %s\n' "$SDK_DIR"

if [[ ! -d "$SDK_DIR/platforms" ]]; then fail "Folder SDK platforms/ tidak ada. Pasang Android SDK Platform yang dibutuhkan project."; fi
if [[ ! -d "$SDK_DIR/build-tools" ]]; then fail "Folder SDK build-tools/ tidak ada. Pasang Android SDK Build-Tools."; fi

if command -v gradle >/dev/null 2>&1; then
  GRADLE_BIN="$(command -v gradle)"
  info "Menggunakan Gradle sistem: $GRADLE_BIN"
else
  GRADLE_BIN="$GRADLE_HOME_LOCAL/bin/gradle"
  if [[ ! -x "$GRADLE_BIN" ]]; then
    mkdir -p "$TOOLS_DIR"
    ARCHIVE="$TOOLS_DIR/gradle-$GRADLE_VERSION-bin.zip"
    URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    info "Gradle belum tersedia. Mengunduh Gradle $GRADLE_VERSION..."
    if command -v curl >/dev/null 2>&1; then curl -fL --retry 3 "$URL" -o "$ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then wget -O "$ARCHIVE" "$URL"
    else fail "curl/wget tidak ditemukan. Pasang curl atau wget untuk mengunduh Gradle."; fi
    if command -v unzip >/dev/null 2>&1; then unzip -q -o "$ARCHIVE" -d "$TOOLS_DIR"
    else python - "$ARCHIVE" "$TOOLS_DIR" <<'PY'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as z: z.extractall(sys.argv[2])
PY
    fi
  fi
fi

TASK="${1:-menu}"
case "$TASK" in
  menu)
    echo ""
    echo "1) Build APK Debug"
    echo "2) Build APK Release (perlu signing key)"
    echo "3) Clean project"
    echo "4) Build + Unit Test"
    read -r -p "Pilih [1-4]: " choice
    case "$choice" in
      1) TASK=debug;; 2) TASK=release;; 3) TASK=clean;; 4) TASK=test;;
      *) fail "Pilihan tidak valid.";;
    esac
    ;;
esac
case "$TASK" in
  debug) GRADLE_TASK="assembleDebug";;
  release)
    [[ -n "${KEYSTORE_PATH:-}" && -n "${STORE_PASSWORD:-}" && -n "${KEY_PASSWORD:-}" ]] || fail "Release membutuhkan KEYSTORE_PATH, STORE_PASSWORD, dan KEY_PASSWORD. Lihat tools/README.md."
    [[ -f "$KEYSTORE_PATH" ]] || fail "Keystore tidak ditemukan: $KEYSTORE_PATH"
    GRADLE_TASK="assembleRelease";;
  clean) GRADLE_TASK="clean";;
  test) GRADLE_TASK="testDebugUnitTest";;
  *) fail "Perintah: ./tools/build.sh [menu|debug|release|clean|test]";;
esac

info "Menjalankan :app:$GRADLE_TASK ..."
"$GRADLE_BIN" --no-daemon --stacktrace ":app:$GRADLE_TASK"
if [[ "$TASK" == "debug" || "$TASK" == "release" ]]; then
  mkdir -p "$DIST_DIR"
  if [[ "$TASK" == "debug" ]]; then APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"; else APK="$ROOT/app/build/outputs/apk/release/app-release.apk"; fi
  [[ -f "$APK" ]] || fail "Build selesai tetapi APK tidak ditemukan di lokasi yang diharapkan: $APK"
  cp -f "$APK" "$DIST_DIR/$(basename "$APK")"
  info "BUILD BERHASIL: $DIST_DIR/$(basename "$APK")"
fi
