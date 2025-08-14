@echo off
setlocal ENABLEDELAYEDEXPANSION
title LDPlayer RAM/CPU Monitor (ADB)

:: -------- Locate ADB --------
set ADB=
if exist "%ProgramFiles%\LDPlayer\adb.exe" set ADB="%ProgramFiles%\LDPlayer\adb.exe"
if exist "%ProgramFiles(x86)%\LDPlayer\adb.exe" set ADB="%ProgramFiles(x86)%\LDPlayer\adb.exe"
if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" set ADB="%ANDROID_HOME%\platform-tools\adb.exe"
if not defined ADB for %%A in (adb.exe) do (set ADB=%%~$PATH:A)
if not defined ADB (
  echo [X] adb.exe tidak ditemukan. Install Android platform-tools atau pastikan LDPlayer terpasang.
  pause
  exit /b 1
)

:: -------- Try connect to LDPlayer --------
%ADB% start-server >nul 2>&1
echo Mencari perangkat...
%ADB% devices > "%temp%\_adb_list.txt"
findstr /R "device$" "%temp%\_adb_list.txt" >nul
if errorlevel 1 (
  echo Tidak ada device aktif, mencoba connect ke LDPlayer...
  %ADB% connect 127.0.0.1:5555 >nul 2>&1
  %ADB% connect 127.0.0.1:62001 >nul 2>&1
  %ADB% devices > "%temp%\_adb_list.txt"
  findstr /R "device$" "%temp%\_adb_list.txt" >nul
  if errorlevel 1 (
    echo [X] Emulator belum terdeteksi. Pastikan LDPlayer sedang berjalan dan USB debugging aktif.
    echo Cara cepat: Settings ^> About ^> tap "Build number" 7x ^> Developer options ^> aktifkan "USB debugging".
    pause
    exit /b 1
  )
)

:: -------- Start server & try connect --------
%ADB% start-server >nul 2>&1
echo Mencari perangkat...
set "DEVICE="
set "DEVCOUNT=0"

:: fungsi untuk hitung device "device" (bukan offline/unauthorized)
for /f "skip=1 tokens=1,2" %%i in ('%ADB% devices') do (
  if "%%j"=="device" (
    if not defined DEVICE set "DEVICE=%%i"
    set /a DEVCOUNT+=1
  )
)

if %DEVCOUNT% EQU 0 (
  echo Tidak ada device aktif, mencoba connect ke LDPlayer...
  %ADB% connect 127.0.0.1:5555  >nul 2>&1
  %ADB% connect 127.0.0.1:62001 >nul 2>&1

  set "DEVICE="
  set "DEVCOUNT=0"
  for /f "skip=1 tokens=1,2" %%i in ('%ADB% devices') do (
    if "%%j"=="device" (
      if not defined DEVICE set "DEVICE=%%i"
      set /a DEVCOUNT+=1
    )
  )
)
echo Terhubung ke: %DEVICE%
echo.

:MENU
echo ===============================
echo  LDPlayer RAM/CPU Monitor
echo ===============================
echo  [1] Snapshot TOP proses (CPU/RAM tertinggi)
echo  [2] Ringkasan RAM (Total/Free/Used)
echo  [3] Detail RAM aplikasi (butuh nama paket)
echo  [4] Simpan laporan lengkap ke file
echo  [Q] Keluar
echo.
set /p CH=Pilihan Anda [1/2/3/4/Q]: 
if /I "%CH%"=="1" goto TOPSNAP
if /I "%CH%"=="2" goto RAMSUM
if /I "%CH%"=="3" goto APPMEM
if /I "%CH%"=="4" goto SAVEREPORT
if /I "%CH%"=="Q" goto END
echo Pilihan tidak dikenal.
echo.
goto MENU

:TOPSNAP
echo.
echo ==== TOP proses (CPU/RAM) ====
:: -n 1 = sekali ambil; -m 20 = tampilkan 20 proses teratas (opsi -m didukung di banyak build Android)
%ADB% -s %DEVICE% shell top -n 1 -m 20
echo.
pause
goto MENU

:RAMSUM
echo.
echo ==== Ringkasan RAM ====
:: Ambil baris penting dari dumpsys meminfo
%ADB% -s %DEVICE% shell dumpsys meminfo | findstr /C:"Total RAM" /C:"Free RAM" /C:"Used RAM" /C:"Lost RAM"
echo.
pause
goto MENU

:APPMEM
echo.
set /p PKG=Masukkan nama paket (cth: com.example.app): 
if "%PKG%"=="" (
  echo Paket kosong.
  echo.
  goto MENU
)
echo.
echo ==== Detail RAM untuk %PKG% ====
%ADB% -s %DEVICE% shell dumpsys meminfo %PKG%
echo.
pause
goto MENU

:SAVEREPORT
set TS=%date:~6,4%-%date:~3,2%-%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TS=%TS: =0%
set OUT=ldp_report_%TS%.txt
echo Membuat laporan: %OUT%
echo ====== LDPlayer Report (%TS%) ====== > "%OUT%"
echo DEVICE: %DEVICE%>> "%OUT%"
echo.>> "%OUT%"
echo --- TOP Proses (20 teratas) --- >> "%OUT%"
%ADB% -s %DEVICE% shell top -n 1 -m 20 >> "%OUT%"
echo.>> "%OUT%"
echo --- Ringkasan RAM --- >> "%OUT%"
%ADB% -s %DEVICE% shell dumpsys meminfo | findstr /C:"Total RAM" /C:"Free RAM" /C:"Used RAM" /C:"Lost RAM" >> "%OUT%"
echo.>> "%OUT%"
echo --- (Opsional) Detail paket spesifik --- >> "%OUT%"
echo Masukkan paket (atau kosong untuk skip):
set /p PKG2= Paket: 
if not "%PKG2%"=="" (
  %ADB% -s %DEVICE% shell dumpsys meminfo %PKG2% >> "%OUT%"
) else (
  echo (dilewati) >> "%OUT%"
)
echo Selesai. Laporan tersimpan: %OUT%
echo.
pause
goto MENU

:END
echo Bye!
endlocal
