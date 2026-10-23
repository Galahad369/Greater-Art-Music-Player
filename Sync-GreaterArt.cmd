@echo off
setlocal
cd /d "%~dp0"
where python >nul 2>nul
if errorlevel 1 (
  echo Python is missing. Install Python 3.11 or newer, then retry.
  pause
  exit /b 1
)
python -X utf8 scripts\sync-greater-art.py --apply --latest-apk-only --repair-apk-cleanup
set "SYNC_RESULT=%ERRORLEVEL%"
if not "%SYNC_RESULT%"=="0" echo Sync stopped safely. Read the reason above; do not force-push.
pause
exit /b %SYNC_RESULT%
