#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
# AndroMITM — build-rootwizard.sh
# Compile l'APK RootWizard SANS Gradle : aapt2 + javac + d8 +
# zipalign + apksigner. Réproductible depuis RESTART.md §3bis.
#
# Prérequis (cf. RESTART.md §3bis) :
#   - JDK 17 complet   : /home/z/android-build/jdk-17.0.20.1+1
#   - Android SDK      : /home/z/android-build/android-sdk
#     (platform-34 + build-tools 34.0.0)
#   - keystore         : tokens/andromitm.keystore (+ keystore_pass.txt)
#
# Sortie : releases/RootWizard-v0.2.0.apk (multi-profils : Blackview/AOSP + Xiaomi/Redmi HyperOS)
# ════════════════════════════════════════════════════════════════
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$REPO_ROOT/apk-rootwizard"
BUILD="$SRC/build"
RELEASES="$REPO_ROOT/releases"

JAVA_HOME="${JAVA_HOME:-/home/z/android-build/jdk-17.0.20.1+1}"
SDK="${ANDROID_HOME:-/home/z/android-build/android-sdk}"
BT="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34/android.jar"

KS="${ANDROMITM_KS:-$REPO_ROOT/../tokens/andromitm.keystore}"
KSPASS_FILE="${ANDROMITM_KS_PASS_FILE:-$REPO_ROOT/../tokens/keystore_pass.txt}"

VERSION_NAME="${ANDROMITM_RW_VERSION:-0.2.0}"
VERSION_CODE="${ANDROMITM_RW_CODE:-2}"
OUT="$RELEASES/RootWizard-v${VERSION_NAME}.apk"

[ -x "$BT/aapt2" ] || { echo "aapt2 introuvable : $BT (cf. RESTART.md §3bis)"; exit 1; }
[ -f "$PLATFORM" ] || { echo "android.jar introuvable : $PLATFORM"; exit 1; }
[ -f "$KS" ] || { echo "keystore introuvable : $KS"; exit 1; }
[ -f "$KSPASS_FILE" ] || { echo "fichier mot de passe introuvable : $KSPASS_FILE"; exit 1; }

export PATH="$JAVA_HOME/bin:$PATH"
rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$RELEASES"

echo "[rootwizard] 1/6 aapt2 compile (ressources)…"
"$BT/aapt2" compile --dir "$SRC/res" -o "$BUILD/res.zip"

echo "[rootwizard] 2/6 aapt2 link (manifeste + R.java)…"
"$BT/aapt2" link -o "$BUILD/base.apk" \
  -I "$PLATFORM" \
  --manifest "$SRC/AndroidManifest.xml" \
  --java "$BUILD/gen" \
  --min-sdk-version 26 --target-sdk-version 34 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
  --auto-add-overlay "$BUILD/res.zip"

echo "[rootwizard] 3/6 javac…"
find "$BUILD/gen" "$SRC/src" -name '*.java' > "$BUILD/sources.txt"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -nowarn \
  -bootclasspath "$PLATFORM" -classpath "$PLATFORM" \
  -d "$BUILD/classes" @"$BUILD/sources.txt"

echo "[rootwizard] 4/6 d8 (dex)…"
find "$BUILD/classes" -name '*.class' > "$BUILD/classes.txt"
"$BT/d8" --release --lib "$PLATFORM" --min-api 26 \
  --output "$BUILD/dex" $(cat "$BUILD/classes.txt")

echo "[rootwizard] 5/6 package + zipalign…"
(cd "$BUILD/dex" && zip -q "$BUILD/base.apk" classes.dex)
"$BT/zipalign" -f 4 "$BUILD/base.apk" "$BUILD/aligned.apk"

echo "[rootwizard] 6/6 signature (apksigner, keystore andromitm)…"
KSPASS="$(tr -d ' \n\r\t' < "$KSPASS_FILE")"
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$KSPASS" \
  --out "$OUT" "$BUILD/aligned.apk"

"$BT/apksigner" verify --print-certs "$OUT" | sed -n '1,4p'
sha256sum "$OUT"
ls -lh "$OUT"
echo "[rootwizard] OK → $OUT"
