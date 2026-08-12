# Bing live wallpaper

Fetches the Bing image of the day at 9 UTC every day. Scales the image to the
largest of the two screen dimensions and centers the image. Redraws the image on
screen layout change (e.g. rotation).

This project is useful for folding phones because the Android wallpaper API will
only update the currently active screen (e.g. cover screen or inner screen).
This makes it difficult to update both screens with new wallpaper images
simultaneously.

The solution is two independent live wallpapers (one for the cover screen, one
for the inner screen).

## Build

Activate Nix shell:
```sh
nix-shell
```

Do this once:
```sh
gradle wrapper --gradle-version 8.10
```

Build:
```sh
./gradlew assembleDebug
```

Install:
```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

Run:
```sh
adb shell am start -a android.service.wallpaper.LIVE_WALLPAPER_CHOOSER
```

