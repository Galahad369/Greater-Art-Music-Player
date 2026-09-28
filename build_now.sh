#!/bin/bash
cd "$(dirname "$0")/greater-art" || exit 1
./gradlew :app:assembleDebug --no-daemon --console=plain
echo "EXIT_CODE: $?"
ls -la app/build/outputs/apk/debug/ 2>/dev/null || echo "NO APK"
