#!/bin/sh
#
# do this once
# gradle wrapper --gradle-version 8.10

./gradlew assembleDebug

# adb install app/build/outputs/apk/debug/app-debug.apk
# adb shell am start -a android.service.wallpaper.LIVE_WALLPAPER_CHOOSER
