# Persistent APK signing and download contract

Both distributed variants use **one dedicated, persistent RSA-3072 PKCS#12 signing key**. Debug and release share the application ID and certificate; switching variants can replace the installed app only when Android's version and downgrade rules permit it. Debug remains debuggable; release is not a debug build. A pre-existing runner-debug-signed installation cannot be updated using this new signing identity; do not uninstall it automatically. Back up user state and decide manually if a one-time migration is needed.

## GitHub configuration

Use the separately distributed `configure-signing.sh --configure` handoff, authenticated as `Mikeyphw`. It generates a fresh local key only for a previously unconfigured environment and repository ID 1466142942, retains its private PKCS#12 and password under `~/.local/share/neostore-obtainx/signing`, and creates the `apk-signing` GitHub Actions environment. Its sole selected deployment rule must be **branch `main`**, not a tag. No repository-level signing secrets, no environment broadening, no key overwrite. The operator refuses an existing environment or interrupted setup; reconcile those states manually rather than generating another key.

- Environment secret `NSOX_KEYSTORE_B64`: base64 of PKCS#12 (base64 is not encryption; GitHub encrypts the secret at rest).
- Environment secret `NSOX_STORE_PASSWORD`: PKCS#12 store/key password.
- Environment variable `NSOX_SIGNER_SHA256`: lowercase SHA-256 of the DER signing certificate; public, not a secret.

Keep secure offline backups of the encrypted keystore and password separately. The certificate pin is public; keep an independent trusted copy for other devices. GitHub does not expose existing secret values for backup. Do not paste keys, passwords, base64 or full shell environment into chat. Review every change to main and environment protection; repository administrators and trusted main workflow code can access the signing authority. Consider adding a required deployment reviewer if desired; this will require approval before builds sign.

## CI and publication

The existing required PR workflow retains its registered workflow ID and real Android build, unit-test and lint gates, also compiling release. It no longer uploads runner-default-signed or unsigned APKs. The separate `signed-apks.yml` accepts only trusted main push or main manual dispatch; no pull_request_target, fork builds, PR artifacts, default signing fallback, or automatic GitHub Release. Gradle runs without passing signing secrets; only the signing step receives them. Both variants must pass alignment, signing, full APK verification and exact single-certificate pin comparison before either artifact upload runs. Missing/mismatched secrets fail closed. Artifacts carry APK hash, variant, exact commit, run ID, attempt and signer digest; downloader uses only a successful run and its matching attempt. The initial merged signing workflow must pass hosted acceptance before these APKs are available.

## Termux downloader

Install dependencies (`gh`, `python`, `apksigner`; apksigner includes a Java dependency). Run `bash scripts/download-apk.sh debug` or `bash scripts/download-apk.sh release`, with an optional existing output directory. Default is `~/storage/downloads`. The local trusted pin at `~/.local/share/neostore-obtainx/signing/certificate.sha256` is mandatory; on another device copy only that public pin through a trusted channel. Uses latest successful main workflow run; fails rather than falling back to an old artifact, unsigned artifact or other certificate. Does not install, uninstall or rotate keys. It never overwrites a prior download. `release` refers to the build variant, not a GitHub Release or production qualification.

## Acceptance

Confirm environment rule and secret-name inventory, then after merge independently verify both artifacts on the same exact successful main run. Download both through the script and compare fingerprints. Test Android signature continuity/update on the actual device separately. Native Devtool delivery of source is distinct from hosted signing and device/release qualification.

## Certificate report compatibility

The verifier accepts both `Signer #1 certificate SHA-256 digest:` and scheme-labelled reports such as `V3.0 Signer: certificate SHA-256 digest:`. Exactly one signer, at least one recognized certificate digest, matching certificate pins in every digest, successful APK verification, and verified v2 signing remain mandatory. Unknown, missing, conflicting, or multi-signer reports fail closed. Reinstall the downloader from the reviewed repair; no certificate pin or signing-key rotation is required.
