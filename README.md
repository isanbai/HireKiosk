# HireKiosk
# Kiosk App (CI)
- Build otomatis via GitHub Actions.
- Debug APK ada di artifacts setiap push ke `main`.
- Release APK: push tag (mis. `v1.0.0`) + isi secrets keystore.

## Jalankan lokal
- Android Studio Narwhal (JDK 21 embedded)
- Sync → Build APKs → app-debug.apk

Supaya lock task auto tanpa dialog dan bisa matikan keyguard:

Factory reset device (syarat Android).

Hubungkan ADB, lalu:

python
Copy
Edit
adb shell dpm set-device-owner id.hirejob.kiosk/.device.AdminReceiver
Buka app lagi → KioskPolicy.apply() akan whitelist paket → startLockTask() langsung aktif tanpa prompt.