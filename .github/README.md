# GitHub Actions Android Compiler — XSD BOOST

Workflow: `.github/workflows/android-build.yml`

## Cara menggunakan

1. Upload/push seluruh isi source ke repository GitHub.
2. Buka tab **Actions** pada repository.
3. Pilih **XSD BOOST Android Build**.
4. Tekan **Run workflow**.
5. Pilih `debug` untuk APK testing atau `release` untuk build release.
6. Setelah job selesai, buka ringkasan workflow dan unduh artifact `xsd-boost-...-apk`.

Push ke branch `main` juga otomatis menjalankan build Debug. Pull request ke `main` menjalankan build Debug untuk memeriksa apakah source dapat dikompilasi.

## Signing APK Release

Agar Release APK ditandatangani, tambahkan repository secrets berikut di **Settings → Secrets and variables → Actions**:

- `ANDROID_KEYSTORE_BASE64`: isi file keystore dalam format Base64 satu baris.
- `ANDROID_STORE_PASSWORD`: password keystore.
- `ANDROID_KEY_PASSWORD`: password key.
- `ANDROID_KEY_ALIAS`: alias key (biasanya `upload`).

Jika secrets tidak tersedia, workflow tetap mencoba build Release tanpa konfigurasi signing khusus. APK release tanpa tanda tangan bukan APK distribusi final.

## Catatan

- Workflow menggunakan GitHub-hosted Ubuntu runner, Temurin JDK 21, Android SDK API 36, dan Gradle 9.3.1.
- File APK tersedia sebagai GitHub Actions artifact selama 14 hari.
- Workflow tidak mengunggah source atau APK ke layanan lain.
- Jika build gagal, buka job log pada langkah yang berwarna merah; error kompilasi akan terlihat di sana.
