# FindIt Campus

Aplikasi Android native untuk melaporkan, menemukan, dan mengembalikan barang di kampus. Project PBO ini mendukung SDG 11 melalui penggunaan kembali barang dan koordinasi pengembalian.

## Cara menjalankan

Gunakan Android Studio atau jalankan `./gradlew :app:assembleDebug`. APK debug berada di `app/build/outputs/apk/debug/app-debug.apk`. Perangkat memerlukan Android 8.0 (API 26) atau lebih baru.

Alur demo menggunakan dua akun pada **satu instalasi**. Buat laporan Ditemukan di akun penemu, keluar, lalu ajukan klaim dengan akun pemilik. Masuk sebagai penemu dan periksa bukti di **Klaim → Kelola Klaim**. Setelah menyetujui klaim, ambil kode dari **Klaim Saya** di akun pemilik. Masukkan kode tersebut sebagai penemu ketika barang diserahkan.

## Struktur dan OOP

| Package | Tanggung jawab |
| --- | --- |
| `model` | Model dengan field privat. `LostItem` dan `FoundItem` mewarisi kontrak `Item` dan menyatakan kemampuan menerima klaim lewat metode polimorfik. |
| `service` | Validasi akun, pembuatan laporan, keputusan klaim, konfirmasi serah terima, dan riwayat. |
| `repository` | Kontrak penyimpanan akun, barang, klaim, sesi, dan transaksi. Service menerima kontrak yang diperlukan melalui constructor. |
| `database` | Entitas Room, kueri DAO, dan adapter yang memetakan data SQLite ke model. |
| `ui` | Pengendali layar Java yang mengikat XML, mengatur navigasi, dan meneruskan aksi pengguna ke service. |
| `utils` | Hash kata sandi, sesi Android Keystore, dan penyimpanan foto privat. |

Pembagian tanggung jawab tersebut menerapkan SRP. Layar dan operasi pencarian bekerja dengan kontrak dasar `Item` (LSP), dan implementasi penyimpanan bisa diganti melalui adapter (OCP dan DIP). Antarmuka repository dipisah berdasarkan data dan kegunaannya (ISP). `FindItApplication` merangkai dependensi. Backend baru perlu menyediakan transaksi dan otorisasi yang setara.

## Penyimpanan dan aturan

Data akun, laporan, ciri rahasia, bukti, kode, dan riwayat disimpan lokal di Room. Foto disalin ke penyimpanan privat aplikasi. Kata sandi disimpan sebagai hash PBKDF2-HMAC-SHA256 dengan salt acak per kata sandi. Sesi dienkripsi dengan AES-GCM menggunakan Android Keystore, berlaku 30 hari setelah masuk, dan dihapus saat keluar.

Kueri barang publik hanya membaca tabel laporan. Ciri rahasia dibaca melalui kueri terpisah yang mencocokkan akun penemu. Bukti klaim tersedia untuk pengklaim dan penemu terkait. Satu persetujuan menutup klaim lain pada barang yang sama. Hanya pengklaim yang disetujui yang melihat kontak dan kode aktif. Verifikasi bukti dilakukan penemu sebelum menyetujui klaim. Penyerahan dan pemakaian kode diselesaikan bersama dalam satu transaksi.

## Asumsi desain dan cakupan

- Identitas FindIt Campus dan lima layar utama mengikuti mockup terbaru. Logo, garis rute, serta bentuk teal digambar ulang sebagai vector drawable. Tipografi memakai sans serif sistem. Form bisa digulir pada layar kecil dan lebar konten dibatasi pada layar besar.
- Form Lapor Barang memiliki jalan pintas untuk mencari dan memfilter laporan. Menutup hasil pencarian mengembalikan draf yang sedang diisi, termasuk foto yang dipilih.
- Tab Semua, Hilang, dan Ditemukan mengikuti desain. Tombol Filter membuka pilihan kategori dan status. Detail kode dibuka dari kartu pada Klaim Saya.
- Tampilan daftar mulai kosong hingga ada laporan. Foto barang dan foto bukti opsional dan berasal dari pemilih berkas Android. Jenis barang dipilih dari daftar, dan tanggal laporan awal adalah hari ini.
- Email diperiksa formatnya. Verifikasi domain kampus, pemulihan kata sandi, dan sinkronisasi antarperangkat memerlukan backend.

## Pengujian

Project memakai JUnit 4, Robolectric, dan Espresso dengan constructor injection. `./gradlew :app:testDebugUnitTest` menjalankan tes hash kata sandi, Room dengan SQLite native, pengukuran layout pada lebar 320 dp dan 800 dp, serta interaksi dua akun dengan sesi yang disimulasikan. `./gradlew :app:connectedDebugAndroidTest` menjalankan skenario Room pada Android dan tes navigasi Espresso. Tes navigasi menggunakan instalasi uji yang belum masuk akun.
