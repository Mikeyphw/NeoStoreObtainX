#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
REPO=Mikeyphw/NeoStoreObtainX
variant=${1:-debug}
[[ $variant == debug || $variant == release ]] || { printf 'Usage: bash download-apk.sh debug|release [destination-directory]\n' >&2; exit 2; }
(($# <= 2)) || exit 2
DEST=${2:-$HOME/storage/downloads}
PINFILE="$HOME/.local/share/neostore-obtainx/signing/certificate.sha256"
fail() { printf 'NSOX_DOWNLOAD_REFUSED=%s\n' "$1" >&2; exit 3; }
for c in gh python3 apksigner; do command -v "$c"; done
[[ -f $PINFILE && ! -L $PINFILE ]] || fail LOCAL_CERTIFICATE_PIN_MISSING
read -r pin < "$PINFILE"
pin=${pin,,}; [[ $pin =~ ^[a-f0-9]{64}$ ]] || fail INVALID_LOCAL_CERTIFICATE_PIN
[[ -d $DEST ]] || fail DESTINATION_NOT_FOUND
mkdir -p "${TMPDIR:-$HOME/.cache}"
scratch=$(mktemp -d "${TMPDIR:-$HOME/.cache}/nsox-download.XXXXXX")
staged=
trap 'if [[ -n $staged ]]; then rm -f -- "$staged"; fi; rm -rf -- "$scratch"' EXIT
printf '\n=== Discover latest successful signed main build ===\n'
gh api "repos/$REPO/actions/workflows/signed-apks.yml" > "$scratch/workflow.json"
gh api --method GET "repos/$REPO/actions/workflows/signed-apks.yml/runs" -f branch=main -f status=success -F per_page=100 > "$scratch/runs.json"
read -r run_id attempt commit < <(python3 -I -B - "$scratch" <<'PY'
import json,sys,re
from pathlib import Path
p=Path(sys.argv[1]); w=json.loads((p/'workflow.json').read_text()); rows=json.loads((p/'runs.json').read_text())['workflow_runs']
assert w['path']=='.github/workflows/signed-apks.yml' and w['state']=='active'
rows=[x for x in rows if x['workflow_id']==w['id'] and x['event'] in ('push','workflow_dispatch') and x['head_branch']=='main' and x['status']=='completed' and x['conclusion']=='success' and x['repository']['full_name']=='Mikeyphw/NeoStoreObtainX' and x['head_repository']['full_name']=='Mikeyphw/NeoStoreObtainX']
assert rows, 'No successful trusted signed build found'
x=max(rows,key=lambda x:(x['created_at'],x['id'])); assert re.fullmatch('[a-f0-9]{40}',x['head_sha'])
assert type(x['id']) is int and type(x['run_attempt']) is int
print(x['id'],x['run_attempt'],x['head_sha'])
PY
) || fail NO_VERIFIED_SIGNED_BUILD
printf 'NSOX_DOWNLOAD_RUN=%s attempt=%s commit=%s variant=%s\n' "$run_id" "$attempt" "$commit" "$variant"
name="nsox-signed-$variant-$attempt"
gh api "repos/$REPO/actions/runs/$run_id/artifacts?per_page=100" > "$scratch/artifacts.json"
artifact_id=$(python3 -I -B - "$scratch/artifacts.json" "$name" "$run_id" "$commit" <<'PY'
import json,sys
p,name,run,commit=sys.argv[1:]; rows=json.load(open(p))['artifacts']; matches=[x for x in rows if x['name']==name and x['expired'] is False and x['workflow_run']['id']==int(run) and x['workflow_run']['head_sha']==commit and x['workflow_run']['head_branch']=='main']
assert len(matches)==1, 'Missing/ambiguous/expired signed artifact; no fallback to old APK'
print(matches[0]['id'])
PY
)
printf 'NSOX_DOWNLOAD_ARTIFACT=%s %s\n' "$artifact_id" "$name"
mkdir "$scratch/artifact"
gh run download "$run_id" --repo "$REPO" --name "$name" --dir "$scratch/artifact"
file="NeoStoreObtainX-$variant.apk"
python3 -I -B - "$scratch/artifact" "$variant" "$pin" "$run_id" "$attempt" "$commit" <<'PY'
from pathlib import Path
import json,hashlib,sys
raw,variant,pin,run,attempt,sha=sys.argv[1:]; p=Path(raw); apk='NeoStoreObtainX-'+variant+'.apk'
assert sorted(x.name for x in p.iterdir())==sorted([apk,'build.json']), 'Unexpected artifact contents'
assert all(x.is_file() and not x.is_symlink() for x in p.iterdir()), 'Unexpected file type'
m=json.loads((p/'build.json').read_text())
expected=dict(schema=1,repository='Mikeyphw/NeoStoreObtainX',head_sha=sha,run_id=int(run),run_attempt=int(attempt),variant=variant,apk=apk,sha256=hashlib.sha256((p/apk).read_bytes()).hexdigest(),signer_sha256=pin)
assert m==expected, 'APK digest or source provenance mismatch'
print('NSOX_DOWNLOAD_METADATA=VERIFIED')
PY
LC_ALL=C apksigner verify --verbose --print-certs --Werr "$scratch/artifact/$file" | tee "$scratch/cert.txt"
python3 -I -B - "$scratch/cert.txt" "$pin" <<'PY'
from pathlib import Path
import sys,re
report=Path(sys.argv[1]).read_text()
pin=sys.argv[2]
# apksigner releases label a single certificate differently. Keep the
# certificate pin independent of the report style and fail on ambiguity.
lines = [line.strip() for line in report.splitlines()]
counts = [line for line in lines if line.startswith('Number of signers:')]
if counts != ['Number of signers: 1']:
    raise SystemExit('Signing certificate verification refused: expected exactly one signer')
pattern = re.compile(r'(?:Signer #1 |V[123](?:\.[0-9]+)? Signer(?: #1)?: )certificate SHA-256 digest: ([0-9a-fA-F]{64})')
digests = []
for line in lines:
    if 'certificate SHA-256 digest:' not in line:
        continue
    match = pattern.fullmatch(line)
    if match is None:
        raise SystemExit('Signing certificate verification refused: unknown signer report')
    digests.append(match.group(1).lower())
if not re.fullmatch(r'[0-9a-f]{64}', pin) or not digests or any(value != pin for value in digests):
    raise SystemExit('Signing certificate mismatch')
if not re.search(r'(?m)^Verified using v2 scheme .*: true\s*$', report):
    raise SystemExit('v2 signature required')
print('NSOX_DOWNLOAD_SIGNATURE=VERIFIED')
PY
OUT="$DEST/NeoStoreObtainX-$variant-${commit:0:12}-r${run_id}a${attempt}.apk"
# Never replace an existing download, even if it has the same basename.
[[ ! -e $OUT && ! -L $OUT ]] || fail OUTPUT_ALREADY_EXISTS
# Copy to a non-APK temporary file on the destination filesystem, then publish.
staged=$(mktemp "$DEST/.nsox-apk.XXXXXX")
cat "$scratch/artifact/$file" > "$staged"
mv -n -- "$staged" "$OUT"
[[ ! -e $staged ]] || fail OUTPUT_ALREADY_EXISTS
staged=
printf 'NSOX_DOWNLOAD_APK=%s\n' "$OUT"
printf 'NSOX_DOWNLOAD_STATUS=VERIFIED_NOT_INSTALLED\n'
