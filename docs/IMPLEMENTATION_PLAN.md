# Implementation milestones

## M0 — Bootstrap (merged in PR #1)
- [x] LSPosed Neo Store-only scope / no-op diagnostic entrypoint.
- [x] Source mapper with official/Izzy/third-party distinction and negative tests.
- [x] Hosted structural, Kotlin unit, Android assembly and lint jobs.
- [x] Hosted jobs verified at exact PR HEAD (`992663658dffc4f93b66d72f9fdd93f913d83bc7`); main merge `261e2cfeed184cdbc68c759ed0493acb7416a656`.
- [ ] Device check on real installed Neo Store/ObtainX versions (not performed).

## M1 — Read-only page detection (merged in PR #2)
- [x] Pin source contract to Neo Store 1.3.3 / versionCode 1304; fail closed for other installed versions.
- [x] Observe `NeoActivity.onResume` + `AppPageVM.setApp` without hooking Compose compiler-generated code.
- [x] Read activity navigation stack and suggested product/repo, accepting metadata only when IDs match.
- [x] In-memory only; no UI, Intent, downloads, DB writes or sensitive diagnostic logs.
- [x] Add pure Kotlin policy and structure regression tests.
- [x] Hosted Android compile, unit tests and lint at exact M1 PR HEAD `f46b855aae9e2ffe66e43ac9d3bbf5a28c94143c`, merged `2340a0cc85bb77dd2012e768c6080569dc10a1a6`.
- [ ] On-device LSPosed test on supported Neo Store APK: navigation, back, background, rotation and stale metadata.
- [ ] UI acceptance on portrait/multiwindow/large-font/dark/AMOLED is deferred to M3 (no M1 view).

## M2 — Intent bridge (merged in PR #3)
- [x] Use `obtainium://app/<Uri-encoded JSON>` with exact ObtainX `App.fromJson` required fields.
- [x] Revalidate M1 READY snapshot against **current route** and freshly computed source mapping before encoding.
- [x] Encode `additionalSettings` as a JSON string, including `appIdOrName` for third-party F-Droid repos and `overrideSource=FDroidRepo`.
- [x] Build package-scoped `ACTION_VIEW` for `dev.bikram.obtainx`, verify exported matching handler and catch Android launch errors.
- [x] No automatic launch, database writes, network calls, download queue or Install/Update interception.
- [x] Kotlin unit tests and Python static/contract regression tests.
- [x] Exact-head hosted M2 CI passed, PR HEAD `3dc57d82d69bb615d3a4e51c1554e08b84263a26`, merge `dcd2cfcbdfcf29fc188e40f58dcb2e33588e3a1c`.
- [ ] Real-device ObtainX 2.21.0 import-preview verification for official, Izzy and third-party repositories; no saved-app claim from an Intent dispatch.
- [ ] UI invocation deferred to M3; M2 API alone cannot open ObtainX.

## Persistent signing — merged in PR #4
- [x] Main-only signing workflow, one persistent project certificate for debug and release.
- [x] Exact main `39d5ff87eced9c715bde9a438d0662428c5e0626` passed Android CI run 44869397124 and signed-build run 44869397197; both signed artifacts published.
- [x] Preserve signing workflow, scripts, certificate pin, environment secrets and downloader in M3.
- [ ] Device signature continuity and M3 post-merge signed artifact acceptance.

## M3 — UX & lifecycle (this candidate)
- [x] Native, high-contrast bottom-end Add to ObtainX button, only on a validated app route; no Compose internals or Install/Update interception.
- [x] Source and route identity re-read on explicit tap; debounce, handler errors, separate credentialed-repo refusal.
- [x] Accessible text, English/Portuguese labels, long-press settings, private namespaced action preference; disabled action leaves a small settings icon.
- [x] Cleanup on pause/destroy, route change, focus loss, IME, unsupported source; pure Kotlin tap-policy tests and structural regression.
- [ ] Hosted exact-head Android assembly, unit tests and lint.
- [ ] Real-device visual and lifecycle acceptance: light/dark/AMOLED, portrait, font scale, cutouts/insets, TalkBack, Android 15/16, duplicate taps, original controls and ObtainX preview.

## M4 — Production acceptance
- Pin exact upstream versions, test installed APKs, verify CI workflows and native Devtool receipts.
- Security review of URI encoding, escaping, redaction and cross-package launch.
- Release/signing/device qualification separately. Do not infer from local or hosted builds.

## Non-goals
No ObtainX DB writes, no system_server hooks, no silent installs, no root shell actions, no 24-shard Python Devtool regression attached to this separate Android repo.
