{
  description = "Android Development Environment on aarch64-darwin";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachSystem [ "aarch64-darwin" ] (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        # 1. Define exact SDK version specifications matching your build.gradle
        buildToolsVersion = "35.0.0";

        androidComposition = pkgs.androidenv.composeAndroidPackages {
          cmdLineToolsVersion = "13.0";
          platformToolsVersion = "37.0.1";
          buildToolsVersions = [ buildToolsVersion ];
          platformVersions = [ "35" ];
          includeEmulator = false; # Set to true if running native emulator
          includeNDK = false;
        };

        androidSdk = androidComposition.androidsdk;
        jdk = pkgs.jdk17;
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = [
            androidSdk
            jdk
            pkgs.gradle
          ];

          # Export essential Android environment variables
          ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
          ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
          JAVA_HOME = jdk.home;

          shellHook = ''
            export PATH="${androidSdk}/libexec/android-sdk/platform-tools:$PATH"
            export PATH="${androidSdk}/libexec/android-sdk/build-tools/${buildToolsVersion}:$PATH"

            # 2. Force Gradle to use Nix-provided AAPT2 instead of Maven prebuilt binaries
            export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/libexec/android-sdk/build-tools/${buildToolsVersion}/aapt2"

            echo "Android dev environment loaded (aarch64-darwin)."
            echo " - Run Tests: nix develop --command ./gradlew test"
            echo " - Build Debug APK: nix develop --command ./gradlew assembleDebug"
            echo " - Build Release APK: nix develop --command ./gradlew assembleRelease"
            echo " - Lint Clojure: nix develop --command clj-kondo --lint app/src/main/clojure app/src/test/clojure"
            echo " - Install App: adb install -r -g app/build/outputs/apk/debug/app-debug.apk"
            echo " - Open Wallpaper Chooser: adb shell am start -a android.service.wallpaper.LIVE_WALLPAPER_CHOOSER"
          '';
        };
      }
    );
}
