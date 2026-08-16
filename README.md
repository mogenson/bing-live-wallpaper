# Bing Live Wallpaper

Fetches the Bing image of the day at 9 UTC every day. Scales the image to the
largest of the two screen dimensions and centers the image. Redraws the image on
screen layout change (e.g. rotation).

The fetch routine checks the datestring of the image metadata, it will try
three times in an exponential backoff (30 minutes, 1 hour, 2 hours) to fetch the
current day's image. There sometimes seems to be some HTTP caching, or
Microsoft's servers are slow to update, so this is more reliable than a single
fetch attempt.

This project is useful for folding phones because the
[Android wallpaper API](https://developer.android.com/reference/android/app/WallpaperManager#setBitmap(android.graphics.Bitmap,%20android.graphics.Rect,%20boolean,%20int))
will only update the currently active screen (e.g. cover screen or inner
screen). This makes it impossible for traditional wallpaper apps (like Muzei) to
update both screens..

The solution is two independent Live Wallpapers (one for the cover screen, one
for the inner screen). A Live Wallpaper is just and Android app that implements the
[WallpaperService](https://developer.android.com/reference/android/service/wallpaper/WallpaperService)
class. Android asks the app to draw to the screen when the wallpaper needs to be displayed.

This Bing Live Wallpaper app is very lightweight, it just converts the Bing
Image of the Day into a bitmap and renders the bitmap based on the display
dimensions when asked. There are no animations or background usage (besides the
daily fetch). There no user-configurable options, and this, no app UI or home
screen icon. If you'd like a different wallpaper source or more complicated
fetch or wallpaper update logic, feel free to fork this project and build on it.

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

