@echo off
title MiLauncher Auto Installer
color 0A

echo ===================================================
echo             MILAUNCHER AUTO INSTALLER
echo ===================================================
echo.
echo Checking for connected devices...
adb devices
echo.

echo ===================================================
echo [1/3] INSTALLING MILAUNCHER...
echo ===================================================
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    adb install -r "app\build\outputs\apk\debug\app-debug.apk"
    if %errorlevel% equ 0 (
        echo [OK] MiLauncher installed successfully.
    ) else (
        echo [ERROR] Failed to install MiLauncher.
        exit /b
    )
) else (
    echo [ERROR] Cannot find app-debug.apk. Please build first.
    exit /b
)

echo.
echo ===================================================
echo [2/3] INSTALLING SUPPLEMENTARY APPS...
echo ===================================================
if exist "app-installer\*.apk" (
    for %%f in (app-installer\*.apk) do (
        echo Installing: %%~nxf
        adb install -r "%%f"
    )
    echo [OK] Supplementary apps installed successfully.
) else (
    echo [INFO] No supplementary apps found in app-installer folder.
)

echo.
echo ===================================================
echo [3/3] LAUNCHING MILAUNCHER...
echo ===================================================
adb shell am start -n com.milauncher/com.milauncher.MainActivity
echo [OK] Launch command sent.

echo.
echo ===================================================
echo [4/4] DISABLING XIAOMI DEFAULT LAUNCHER & BLOATWARE...
echo ===================================================
echo This will deeply intervene in the system to disable unnecessary services.
adb shell pm disable-user --user 0 com.mitv.tvhome
adb shell pm disable-user --user 0 com.mitv.tvhome.atv
adb shell pm disable-user --user 0 com.xiaomi.mitv.tvhome
adb shell pm disable-user --user 0 com.xiaomi.mitv.advertise
adb shell pm disable-user --user 0 com.xiaomi.mitv.update
adb shell pm disable-user --user 0 com.xiaomi.account
adb shell pm disable-user --user 0 com.xiaomi.tv.appstore
adb shell pm disable-user --user 0 com.xiaomi.screenimage
adb shell pm disable-user --user 0 com.xiaomi.mitv.tvpush.tvpushservice
adb shell pm disable-user --user 0 com.mitv.videoplayer
echo [OK] Xiaomi system bloatware disabled.

echo.
echo ===================================================
echo INSTALLATION COMPLETE!
echo ===================================================
echo Please press the HOME button on your remote. MiLauncher should now be the default.
pause
