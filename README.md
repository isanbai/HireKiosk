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

You are an expert Android/Kotlin engineer. Build a production-ready kiosk-style Android app for a 7” tablet with the following spec.

## Goals
- When external trigger is ON: play a local MP4 splash video either ONCE or LOOP based on a setting.
- When trigger is OFF: show a static image (JPG/PNG) or GIF (loop).
- Fullscreen, immersive mode, keep screen on, autostart on boot. Optional kiosk (Lock Task Mode).
- Provide multiple trigger sources: USB HID (OTG), Bluetooth HID, Headset media-button (if supported), Wi-Fi HTTP webhook, BLE GATT custom, and Volume buttons (fallback).
- Simple Settings screen:
  - Select video file (mp4) and image/GIF file from storage.
  - Toggle “Loop video” (true/false).
  - Choose trigger source: [USB_HID | BT_HID | HEADSET | HTTP | BLE_GATT | VOLUME].
  - Minimal dwell/debounce time in ms (default 200 ms).
  - Autostart on boot (enable/disable).
  - Kiosk mode (enable/disable).

## Tech stack
- Language: Kotlin, minSdk {{minSdk=24}}, targetSdk latest stable.
- Video: ExoPlayer.
- Images/GIF: Glide (GIF auto loop).
- DI: simple (no heavy framework).
- Permissions: READ_EXTERNAL_STORAGE/Media picker, BLUETOOTH/BLUETOOTH_ADMIN/BLUETOOTH_CONNECT (Android 12+), INTERNET (for HTTP server), FOREGROUND_SERVICE (if used), RECEIVE_BOOT_COMPLETED.
- Build with Gradle KTS.

## App behavior
- State machine: IDLE (show image/GIF) vs ACTIVE (play video).
- Transition to ACTIVE when trigger=ON; to IDLE when trigger=OFF.
- Debounce trigger (configurable). Enforce minimal display time (e.g., 1s) to avoid flicker.
- Video:
  - Use ExoPlayer in fit center, mute/unmute option (default muted).
  - If “Loop video” is OFF → play once then auto-return to IDLE (show image/GIF).
  - If “Loop video” is ON → repeat forever until trigger becomes OFF.
- Image/GIF:
  - Glide into ImageView; if GIF, loop forever.
- Fullscreen immersive sticky, keep screen on, disable system bars.
- Autostart: register BOOT_COMPLETED to relaunch app if enabled.
- Kiosk mode: if enabled, pin the task and suppress back/home (with device owner or screen pinning fallback).

## Trigger sources (implement each behind a common interface TriggerSource):
- USB_HID: listen to key events from InputManager/Activity. Map KEYCODE_VOLUME_UP=ON, KEYCODE_VOLUME_DOWN=OFF.
- BT_HID: identical handling as USB; just reacts to key events from paired device/selfie remote.
- HEADSET: handle media button via MediaSessionCompat; map PLAY_PAUSE/PLAY = ON, PAUSE = OFF. Note: only if device supports analog TRRS remote.
- HTTP: run a lightweight embedded HTTP server on port {{port=8765}} with endpoints:
  - GET /health → 200 OK
  - POST /trigger { "state": "on" | "off" }
  Parse JSON and update trigger state.
- BLE_GATT: act as GATT **client** to a peripheral advertising service UUID {{service_uuid}} characteristic {{char_uuid}}. Subscribe notifications; value 0/1 → OFF/ON.
- VOLUME: VOL_DOWN → ON / OFF (for demo/testing).

## Settings & storage
- Use DataStore for preferences (videoUri, imageUri, loopVideo, triggerType, debounceMs, autostart, kiosk).
- Add a simple file picker using Storage Access Framework.
- Add a diagnostic screen: shows current trigger source, last event time, current state, and a manual ON/OFF toggle.

## Reliability & UX
- Handle runtime permissions gracefully.
- Show an overlay toast/log line in diagnostic mode only.
- Keep a ForegroundService if required for HTTP/BLE listeners, with a minimal notification channel.
- Add watchdog to restart listeners on error.
- Ensure the app resumes the correct state after process death.

## Project structure

## Acceptance criteria
- Switch trigger to ON → video starts within <200ms on test device.
- Switch trigger to OFF:
  - If loop mode → stop immediately and show image/GIF.
  - If playing once mode → if video already ended, remain in image; if mid-play, stop and show image.
- Works with:
  - USB HID keyboard keys via OTG (simulate with Pro Micro).
  - BT selfie remote mapped to volume keys.
  - HTTP: curl -X POST localhost:8765/trigger -d '{"state":"on"}'.
- Survives screen off/on and app relaunch. Autostarts on BOOT if enabled.
- Kiosk mode prevents exiting app (when enabled and supported).

## Bonus (nice to have)
- Local file copy option to app-private storage.
- Brightness lock in app.
- Optional “minimum active time” and “minimum idle time”.
- Basic telemetry log to a rolling file for debugging.

## Deliverables
- Full Android Studio project (Gradle KTS).
- README with wiring guides for:
  - USB HID via Pro Micro (pin -> key mapping),
  - ESP32 BLE HID (sample Arduino .ino),
  - Headset remote caveat (digital DAC warning),
  - HTTP examples.
- Short QA checklist and test plan.