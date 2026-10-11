#!/usr/bin/env bash
# Sign a staged APK only; no source APK or user's keystore is modified.
set -euo pipefail
if (( $# != 2 )); then
  echo 'Usage: bash scripts/sign-release-apk.sh INPUT_APK NEW_OUTPUT_APK' >&2
  exit 2
fi
for setting in MAVLAB_APK_KEYSTORE_B64 MAVLAB_APK_STORE_PASSWORD MAVLAB_APK_KEY_ALIAS MAVLAB_APK_KEY_PASSWORD MAVLAB_APK_CERT_SHA256; do
  if [[ -z ${!setting:-} ]]; then
    echo "Publication signing input missing: $setting" >&2
    exit 2
  fi
done
[[ $MAVLAB_APK_CERT_SHA256 =~ ^[a-fA-F0-9]{64}$ ]] || { echo 'Expected certificate fingerprint must be SHA-256 hex' >&2; exit 2; }
signer=${MAVLAB_APKSIGNER:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/build-tools/35.0.0/apksigner}
[[ -x $signer && -r $1 ]] || { echo 'APK or apksigner unavailable' >&2; exit 2; }
[[ ! -e $2 ]] || { echo 'Refusing to replace signing output' >&2; exit 2; }
umask 077
key_file=$(mktemp "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/mavlab-apk-signing-XXXXXX.jks")
trap 'rm -f "$key_file"' EXIT
printf '%s' "$MAVLAB_APK_KEYSTORE_B64" | base64 --decode > "$key_file"
"$signer" sign --ks "$key_file" --ks-pass env:MAVLAB_APK_STORE_PASSWORD \
  --ks-key-alias "$MAVLAB_APK_KEY_ALIAS" --key-pass env:MAVLAB_APK_KEY_PASSWORD \
  --out "$2" "$1"
actual=$("$signer" verify --print-certs "$2" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
[[ ${actual,,} == ${MAVLAB_APK_CERT_SHA256,,} ]] || { echo 'APK certificate does not match the approved upgrade identity' >&2; exit 1; }
echo 'APK signature verified against the approved certificate.'
