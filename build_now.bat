@echo off
setlocal
cd /d "%~dp0greater-art" || exit /b 1
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\openjdk\jdk-21.0.8"
if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
"%JAVA_HOME%\bin\java.exe" -jar gradle\wrapper\gradle-wrapper.jar :app:assembleDebug --offline --no-daemon --console=plain
exit /b %ERRORLEVEL%
