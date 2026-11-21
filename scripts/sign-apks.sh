#!/usr/bin/env bash
set -Eeuo pipefail
# Do not run with xtrace; secret values must never reach logs or arguments.
set +x
umask 077
fail() { printf 'NSOX_SIGN_REFUSED=%s\n' "$1" >&2; exit 3; }
[[ ${GITHUB_REPOSITORY:-} == Mikeyphw/NeoStoreObtainX && ${GITHUB_REF:-} == refs/heads/main ]] || fail WRONG_SOURCE
[[ ${GITHUB_EVENT_NAME:-} == push || ${GITHUB_EVENT_NAME:-} == workflow_dispatch ]] || fail WRONG_EVENT
[[ ${GITHUB_SHA:-} =~ ^[a-f0-9]{40}$ && ${GITHUB_RUN_ID:-} =~ ^[0-9]+$ && ${GITHUB_RUN_ATTEMPT:-} =~ ^[0-9]+$ ]] || fail MISSING_PROVENANCE
[[ $(git rev-parse HEAD) == "$GITHUB_SHA" ]] || fail HEAD_MISMATCH
[[ -n ${NSOX_KEYSTORE_B64:-} && -n ${NSOX_STORE_PASSWORD:-} ]] || fail MISSING_SIGNING_SECRETS
pin=${NSOX_SIGNER_SHA256:-}; pin=${pin,,}
[[ $pin =~ ^[a-f0-9]{64}$ ]] || fail MISSING_OR_INVALID_CERTIFICATE_PIN
SDK=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
[[ -n $SDK ]] || fail ANDROID_SDK_UNAVAILABLE
signer="$SDK/build-tools/35.0.0/apksigner"
aligner="$SDK/build-tools/35.0.0/zipalign"
[[ -x $signer && -x $aligner ]] || fail SDK_TOOLS_UNAVAILABLE
[[ ! -e signed && ! -L signed ]] || fail OUTPUT_ALREADY_EXISTS
scratch=$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/nsox-sign.XXXXXX")
trap 'rm -rf -- "$scratch"' EXIT
printf '%s' "$NSOX_KEYSTORE_B64" | base64 -d > "$scratch/key.p12"
unset NSOX_KEYSTORE_B64
[[ -s $scratch/key.p12 ]] || fail EMPTY_KEYSTORE
mkdir -p "$scratch/output"
shopt -s nullglob
for variant in debug release; do
    printf '\n=== Sign %s APK ===\n' "$variant"
    apks=(app/build/outputs/apk/"$variant"/*.apk)
    [[ ${#apks[@]} -eq 1 && -s ${apks[0]} ]] || fail "EXPECTED_ONE_${variant^^}_APK"
    "$aligner" -c -P 16 4 "${apks[0]}"
    dir="$scratch/output/$variant"; mkdir "$dir"
    out="$dir/NeoStoreObtainX-$variant.apk"
    "$signer" sign --ks "$scratch/key.p12" --ks-type PKCS12 --ks-key-alias neostoreobtainx \
        --ks-pass env:NSOX_STORE_PASSWORD --key-pass env:NSOX_STORE_PASSWORD \
        --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
        --v4-signing-enabled false --out "$out" "${apks[0]}"
    "$signer" verify --verbose --print-certs --Werr "$out" | tee "$scratch/cert.txt"
    python3 -B - "$scratch/cert.txt" "$pin" "$variant" "$out" <<'PY'
import hashlib,json,os,re,sys
from pathlib import Path
cert,pin,variant,raw=sys.argv[1:]; p=Path(raw)
text=Path(cert).read_text()
assert [x.lower() for x in re.findall(r'(?m)^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)\s*$',text)]==[pin], 'Signer digest/count mismatch'
assert re.search(r'(?m)^Verified using v2 scheme .*: true$', text), 'v2 signature required'
metadata=dict(schema=1, repository=os.environ['GITHUB_REPOSITORY'], head_sha=os.environ['GITHUB_SHA'],run_id=int(os.environ['GITHUB_RUN_ID']), run_attempt=int(os.environ['GITHUB_RUN_ATTEMPT']),variant=variant,apk=p.name,sha256=hashlib.sha256(p.read_bytes()).hexdigest(),signer_sha256=pin)
(p.parent/'build.json').write_text(json.dumps(metadata,indent=2,sort_keys=True)+'\n')
print('NSOX_SIGN_VERIFIED='+variant+' '+pin)
PY
done
unset NSOX_STORE_PASSWORD
# Publish neither variant unless both have passed verification.
mv "$scratch/output" signed
printf 'NSOX_SIGN_BOTH_VARIANTS=PASS\n'
