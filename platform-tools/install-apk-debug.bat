adb install -r app/build/outputs/apk/debug/app-debug.apk
pause
adb shell monkey -p id.hirejob.kiosk 1