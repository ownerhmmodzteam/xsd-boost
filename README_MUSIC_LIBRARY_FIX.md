# XSD BOOST — Music Library & Overlay Scroll Fix

## Perbaikan
- Dynamic Island Home: konten yang terbuka dibatasi tinggi dan dapat di-scroll vertikal.
- In-game Booster > Music: menampilkan Local Music Library untuk folder `/storage/emulated/0/music_hmmodz`.
- Pemindaian menggunakan MediaStore dan mencakup subfolder yang terindeks Android.
- Mendukung MP3, M4A, AAC, FLAC, OGG, OPUS, WAV, 3GP, AMR, dan MKA.
- Daftar lagu dapat dicari dan di-scroll; mengetuk lagu langsung memutarnya melalui MusicPlayerEngine yang sama.
- Izin `READ_MEDIA_AUDIO` (Android 13+) atau `READ_EXTERNAL_STORAGE` (Android 12L ke bawah) diminta saat aplikasi dibuka.
- Tombol Scan Ulang disediakan untuk memperbarui daftar setelah file musik ditambahkan.

## Catatan penggunaan
1. Instal APK hasil build.
2. Izinkan akses Musik dan audio saat diminta. Jika pernah ditolak, aktifkan dari Pengaturan Android > Aplikasi > XSD BOOST > Izin.
3. Letakkan file audio di `/storage/emulated/0/music_hmmodz` atau subfoldernya.
4. Buka Game Booster > Music, lalu tekan Scan Ulang.
5. Jika file baru saja disalin dan belum terindeks Android, tunggu pemindaian media selesai atau restart perangkat, lalu Scan Ulang.

Pemindaian memakai MediaStore, bukan akses filesystem mentah. Karena pembatasan scoped storage Android, hanya audio yang sudah terdaftar oleh MediaStore dan dapat diakses oleh izin media yang akan ditampilkan.
