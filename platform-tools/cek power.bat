@echo off
title ADB Battery Simulation Loop with Reset Option
setlocal ENABLEDELAYEDEXPANSION

:: Ganti ini dengan device ID kalau ada lebih dari 1 device
:: Contoh: set DEVICE=127.0.0.1:5555
set DEVICE=

:DETECT
:: Cek device
for /f "skip=1 tokens=1,2" %%i in ('adb devices') do (
    if "%%j"=="device" (
        if not defined DEVICE set DEVICE=%%i
    )
)
if not defined DEVICE (
    echo [X] Tidak ada device terdeteksi.
    echo Pastikan emulator/HP terhubung dan USB debugging aktif.
    pause
    goto DETECT
)

echo Menggunakan device: %DEVICE%
echo Tekan CTRL+C atau pilih Q untuk berhenti.
echo.

:LOOP
echo --- Unplug Charger ---
adb -s %DEVICE% shell cmd battery unplug
pause

echo --- Set AC 0 ---
adb -s %DEVICE% shell cmd battery set ac 0
pause

echo --- Set AC 1 ---
adb -s %DEVICE% shell cmd battery set ac 1
pause

echo.
set /p CH=[Enter untuk ulang / R untuk reset / Q untuk keluar]: 
if /I "%CH%"=="R" (
    echo --- Reset Battery ---
    adb -s %DEVICE% shell cmd battery reset
    pause
)
if /I "%CH%"=="Q" goto END
goto LOOP

:END
echo Bye!
endlocal
