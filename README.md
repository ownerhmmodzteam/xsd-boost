<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/8ef4ac03-e4d6-4d9c-b507-528b73959955

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.

## Build APK melalui GitHub Actions

Project ini sudah menyertakan workflow GitHub Actions di `.github/workflows/android-build.yml`.
Upload source ke repository GitHub, buka tab **Actions**, pilih **XSD BOOST Android Build**, lalu jalankan workflow. Pilih `debug` untuk APK testing atau `release` untuk build release. APK yang berhasil dibuat dapat diunduh dari bagian **Artifacts** pada halaman hasil workflow. Push ke branch `main` otomatis menjalankan build Debug.

Panduan lengkap dan pengaturan signing ada di `.github/README.md`.
