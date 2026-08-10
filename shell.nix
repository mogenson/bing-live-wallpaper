{
  pkgs ? import <nixpkgs> {
    config = {
      allowUnfree = true;
      android_sdk.accept_license = true;
    };
  },
}:

let
  cmdlineTools = pkgs.androidenv.composeAndroidPackages {
    platformVersions = [ ];
    includeEmulator = false;
    includeNDK = false;
    includeSystemImages = false;
  };
in
pkgs.mkShell {
  packages = [
    cmdlineTools.androidsdk # provides sdkmanager
    pkgs.jdk17
    pkgs.gradle
    pkgs.android-tools
  ];
  shellHook = ''
    export ANDROID_HOME="$HOME/Library/Android/sdk"
    export ANDROID_SDK_ROOT="$ANDROID_HOME"

    if [ ! -d "$ANDROID_HOME/platforms/android-35" ]; then
      echo "Installing Android SDK 35 into $ANDROID_HOME ..."
      mkdir -p "$ANDROID_HOME"
      yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses > /dev/null
      sdkmanager --sdk_root="$ANDROID_HOME" \
        "platforms;android-35" "build-tools;35.0.0" "platform-tools"
    fi
  '';
}
