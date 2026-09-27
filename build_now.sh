#!/bin/bash
cd "/c/Users/galah/Desktop/Knowledge-Base/Random Coding stuff/App/greater-art"
./gradlew :app:assembleDebug --no-daemon --console=plain
echo "EXIT_CODE: $?"
ls -la app/build/outputs/apk/debug/ 2>/dev/null || echo "NO APK"