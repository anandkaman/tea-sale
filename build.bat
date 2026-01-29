@echo off
setlocal

set ANDROID_SDK_ROOT=C:\Android\Sdk
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%ANDROID_SDK_ROOT%\platform-tools;%ANDROID_SDK_ROOT%\build-tools\34.0.0;%PATH%

echo ========================================
echo GOLD Tea Sales - Android Build Script
echo ========================================
echo.

echo Checking Android SDK...
if not exist "%ANDROID_SDK_ROOT%" (
    echo ERROR: Android SDK not found at %ANDROID_SDK_ROOT%
    exit /b 1
)
echo Android SDK: OK

echo.
echo Checking device connection...
adb devices
echo.

echo Setting up Gradle wrapper...
cd /d "%~dp0"

echo.
echo Building APK...
echo This may take several minutes on first build...
echo.

REM Create gradlew if it doesn't exist
if not exist gradlew.bat (
    echo Creating Gradle wrapper...
    gradle wrapper --gradle-version 8.2
)

REM Build the APK
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo APK Location:
    echo %~dp0app\build\outputs\apk\debug\app-debug.apk
    echo.
    
    REM Copy to build_output folder
    if not exist build_output mkdir build_output
    copy /Y app\build\outputs\apk\debug\app-debug.apk build_output\gold-tea-sales.apk
    echo.
    echo APK copied to: build_output\gold-tea-sales.apk
    echo.
    
    echo Installing on device...
    adb install -r build_output\gold-tea-sales.apk
    
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ========================================
        echo INSTALLATION SUCCESSFUL!
        echo ========================================
        echo.
        echo You can now launch the app on your device.
    ) else (
        echo.
        echo Installation failed. Make sure:
        echo 1. Device is connected and authorized
        echo 2. USB debugging is enabled
        echo.
        echo You can manually install: build_output\gold-tea-sales.apk
    )
) else (
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    echo Please check the error messages above.
)

pause
