# XSD BOOST Build Tools

Build helper untuk Windows, Linux, macOS, dan Termux. Tool ini memakai Gradle 9.3.1 sesuai `gradle/wrapper/gradle-wrapper.properties`. Jika Gradle tidak terpasang, tool mengunduh distribusi Gradle resmi ke `.build-tools/` (tidak mengubah source project).

## Persyaratan

- JDK 17 atau lebih baru (JDK 21 disarankan).
- Android SDK yang sudah memiliki folder `platforms/` dan `build-tools/`.
- Koneksi internet saat pertama kali mengunduh Gradle dan dependency Maven.
- Ruang penyimpanan cukup untuk Gradle, Android SDK, dependency, dan hasil build.

Pastikan Android SDK Platform dan Build Tools sesuai kebutuhan `compileSdk` project (36, minor API 1). Jika build menyatakan platform tidak ditemukan, buka SDK Manager dan pasang platform Android 36/ext yang diminta serta Build Tools yang kompatibel.

## Windows

1. Pasang JDK 17/21 dan Android Studio atau Android SDK Command-line Tools.
2. Atur `ANDROID_HOME` ke folder SDK, biasanya `%LOCALAPPDATA%\Android\Sdk`.
3. Ekstrak source ZIP ke folder biasa (hindari folder yang butuh izin administrator).
4. Klik dua kali `tools/build.bat`, lalu pilih Debug atau Release.

Alternatif PowerShell:

```powershell
.\tools\build.ps1
```

## Linux / macOS

```bash
export JAVA_HOME="/path/to/jdk"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
chmod +x tools/build.sh
./tools/build.sh
```

## Termux (Android)

1. Pasang JDK dan utilitas dasar melalui Termux:

```bash
pkg update
pkg install openjdk-21 curl unzip
```

2. Siapkan Android SDK yang valid. `ANDROID_HOME` harus menunjuk SDK yang memiliki `platforms/` dan `build-tools/`; folder kosong tidak cukup. Termux tidak otomatis menyediakan Android SDK lengkap hanya dengan memasang JDK.
3. Ekstrak project ke penyimpanan privat Termux untuk menghindari masalah permission/symlink saat Gradle bekerja. Contoh:

```bash
mkdir -p "$HOME/projects/xsd-boost"
# Ekstrak ZIP ke folder tersebut, lalu:
cd "$HOME/projects/xsd-boost"
export ANDROID_HOME="$HOME/android-sdk"  # ganti dengan lokasi SDK yang benar
export ANDROID_SDK_ROOT="$ANDROID_HOME"
chmod +x tools/build.sh
./tools/build.sh
```

Jika project berada di `/storage/emulated/0`, build bisa mengalami masalah permission/symlink. Simpan source dan cache build di `$HOME` Termux; hasil APK dapat disalin ke shared storage setelah selesai.

## Perintah langsung (Linux/macOS/Termux)

```bash
./tools/build.sh debug    # APK debug
./tools/build.sh release  # APK release, membutuhkan signing key + env
./tools/build.sh clean    # hapus output build Gradle
./tools/build.sh test     # unit test debug
```

Windows menggunakan menu `tools/build.bat` atau `tools/build.ps1`.

## Release signing

Buat upload keystore satu kali:

```bash
./tools/create-release-key.sh
```

Simpan keystore dan password dengan aman. Jangan unggah ke GitHub atau bagikan. Kemudian isi environment sebelum menjalankan release:

Linux/macOS/Termux:

```bash
export KEYSTORE_PATH="$PWD/upload-key.jks"
read -s -p 'Store password: ' STORE_PASSWORD; export STORE_PASSWORD; echo
read -s -p 'Key password: ' KEY_PASSWORD; export KEY_PASSWORD; echo
./tools/build.sh release
```

PowerShell:

```powershell
$env:KEYSTORE_PATH = (Join-Path (Get-Location) 'upload-key.jks')
$env:STORE_PASSWORD = Read-Host 'Store password'
$env:KEY_PASSWORD = Read-Host 'Key password'
# Password hanya tersimpan di environment session PowerShell saat ini. Jangan commit ke repository.
.\tools\build.ps1
```

Catatan: konfigurasi project saat ini menggunakan alias key `upload`. APK debug dapat dibangun tanpa upload keystore. Untuk distribusi publik, jaga keystore dan password; kehilangan keystore dapat menghambat update aplikasi dengan identitas signing yang sama.

## Output

- Debug: `dist/app-debug.apk`
- Release: `dist/app-release.apk`
- Log error lengkap ditampilkan oleh Gradle (`--stacktrace`).

Tool ini menjalankan build project; tool tidak menjamin build sukses jika SDK/dependency/source memiliki error. Pastikan periksa pesan Gradle.
