#!/usr/bin/env bash
#
# Build, sign, and install a release APK of Bing Live Wallpaper using the
# debug keystore. Intended for personal/testing installs on your own device.
#
# The debug key is NOT suitable for distribution. F-Droid builds sign with
# their own key (and use the unsigned APK from `./gradlew assembleRelease`),
# so this script is only for local installs.
#
# Usage:
#   ./install-release.sh            # build, sign, install
#   ./install-release.sh --no-install   # build and sign only
#
set -euo pipefail

cd "$(dirname "$0")"

BUILD_TOOLS_VERSION="35.0.0"
PKG="com.binglivewallpaper"
OUT_DIR="app/build/outputs/apk/release"
UNSIGNED="$OUT_DIR/app-release-unsigned.apk"
ALIGNED="$OUT_DIR/app-release-aligned.apk"
SIGNED="$OUT_DIR/app-release-signed.apk"

DEBUG_KEYSTORE="$HOME/.android/debug.keystore"
DEBUG_STORE_PASS="android"
DEBUG_KEY_ALIAS="androiddebugkey"
DEBUG_KEY_PASS="android"

DO_INSTALL=1
if [[ "${1:-}" == "--no-install" ]]; then
    DO_INSTALL=0
fi

# Prefer the Nix dev shell (provides Java, Android SDK, ANDROID_HOME).
# If already inside it, ANDROID_HOME is set; otherwise re-exec under nix.
if [[ -z "${ANDROID_HOME:-}" ]]; then
    echo "ANDROID_HOME not set — re-running under 'nix develop'..."
    exec nix develop --command "$0" "$@"
fi

BUILD_TOOLS="$ANDROID_HOME/build-tools/$BUILD_TOOLS_VERSION"
ZIPALIGN="$BUILD_TOOLS/zipalign"
APKSIGNER="$BUILD_TOOLS/apksigner"

if [[ ! -f "$DEBUG_KEYSTORE" ]]; then
    echo "error: debug keystore not found at $DEBUG_KEYSTORE" >&2
    exit 1
fi

echo "==> Building release APK (R8-minified)..."
./gradlew assembleRelease --console=plain

if [[ ! -f "$UNSIGNED" ]]; then
    echo "error: expected unsigned APK at $UNSIGNED" >&2
    exit 1
fi

echo "==> Aligning..."
"$ZIPALIGN" -p -f 4 "$UNSIGNED" "$ALIGNED"

echo "==> Signing with debug keystore..."
"$APKSIGNER" sign \
    --ks "$DEBUG_KEYSTORE" \
    --ks-pass "pass:$DEBUG_STORE_PASS" \
    --key-pass "pass:$DEBUG_KEY_PASS" \
    --ks-key-alias "$DEBUG_KEY_ALIAS" \
    --out "$SIGNED" \
    "$ALIGNED"

echo "==> Verifying signature..."
"$APKSIGNER" verify --print-certs "$SIGNED" | grep -E "Signer #1 certificate (DN|SHA-256)"

echo "==> Signed APK: $SIGNED ($(du -h "$SIGNED" | cut -f1))"

if [[ "$DO_INSTALL" -eq 1 ]]; then
    echo "==> Installing to connected device..."
    adb install -r "$SIGNED"
    echo "==> Installed $PKG"
    echo "    Set the wallpaper with:"
    echo "    adb shell am start -a android.service.wallpaper.LIVE_WALLPAPER_CHOOSER"
else
    echo "==> Skipping install (--no-install)."
fi
