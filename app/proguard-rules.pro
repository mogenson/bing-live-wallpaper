# Project-specific ProGuard rules
-dontwarn clojure.**
-keep class clojure.** { *; }
-keep class com.binglivewallpaper.** { *; }
-dontwarn java.lang.invoke.**

# Clojure loads these AndroidX classes reflectively (via (:import ...) and
# gen-class :extends), which R8 cannot trace. Keep them so the release build
# doesn't strip WorkManager / core classes that the wallpaper worker needs.
-keep class androidx.work.** { *; }
-keep class androidx.core.** { *; }
-dontwarn androidx.work.**
-dontwarn androidx.core.**

