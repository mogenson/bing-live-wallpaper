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
update both screens.

The solution is two independent Live Wallpapers (one for the cover screen, one
for the inner screen). A Live Wallpaper is just an Android app that implements the
[WallpaperService](https://developer.android.com/reference/android/service/wallpaper/WallpaperService)
class. Android asks the app to draw to the screen when the wallpaper needs to be displayed.

This Bing Live Wallpaper app is very lightweight: it just converts the Bing
Image of the Day into a bitmap and renders the bitmap based on the display
dimensions when asked. There are no animations or background usage (besides the
daily fetch). There are no user-configurable options, and thus, no app UI or home
screen icon. If you'd like a different wallpaper source or more complicated
fetch or wallpaper update logic, feel free to fork this project and build on it.

## Build

This project is built with the [clj-android](https://github.com/clj-android)
toolchain: a Gradle plugin AOT-compiles the Clojure sources and wires them into
the Android build. On the first build, `settings.gradle.kts` automatically
clones the toolchain's dependencies
(`android-clojure-plugin`, `neko`, `runtime-core`, `runtime-repl`, and
`clojure-patched`) into `build/deps/` and includes them as Gradle composite
builds — no manual setup is required. In debug builds the plugin substitutes
stock Clojure with `clojure-patched` (Clojure 1.12.0 with an Android-aware
classloader) so the on-device nREPL works.

Activate the Nix dev shell:
```sh
nix develop
```

Build:
```sh
./gradlew assembleDebug
```

Install:
```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

Run (open the live wallpaper chooser and select Bing Live Wallpaper):
```sh
adb shell am start -a android.service.wallpaper.LIVE_WALLPAPER_CHOOSER
```

## REPL-driven development

Debug builds start an nREPL server on the device (port 7888) via the
`ClojureApp` application class. Forward the port and connect from any nREPL
client (CIDER, Calva, `lein repl :connect`, ...):

```sh
adb forward tcp:7888 tcp:7888
```

Then connect to `localhost:7888` and evaluate against the running wallpaper:

```clojure
(require '[com.binglivewallpaper.refresh-worker :as w])
(import 'com.goodanser.clj_android.runtime.ClojureApp)

;; Trigger an immediate fetch of today's image
(w/run-once-now (ClojureApp/getInstance))
```

Watch device logs with `adb logcat -s ClojureApp BingWallpaperService BingRefreshWorker`.

## F-Droid

This repository includes F-Droid & Fastlane metadata for publishing:

* **Fastlane metadata**: Store listing, descriptions, and app icons are located under `fastlane/metadata/android/en-US/`.
* **F-Droid recipe**: The build metadata recipe for `fdroiddata` is located in `metadata/com.binglivewallpaper.yml`.

To release a new version on F-Droid:
1. Update `versionCode` and `versionName` in [`app/build.gradle.kts`](file:///Users/mike/Code/bing-live-wallpaper/app/build.gradle.kts).
2. Create and push a git tag (e.g. `git tag -a v1.0 -m "Release v1.0" && git push origin v1.0`).
3. Submit a Merge Request to [fdroiddata](https://gitlab.com/fdroid/fdroiddata) using `metadata/com.binglivewallpaper.yml`.


