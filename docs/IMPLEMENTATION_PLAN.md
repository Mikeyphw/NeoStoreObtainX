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

## M2 — Intent bridge (this candidate)
- [x] Use `obtainium://app/<Uri-encoded JSON>` with exact ObtainX `App.fromJson` required fields.
- [x] Revalidate M1 READY snapshot against **current route** and freshly computed source mapping before encoding.
- [x] Encode `additionalSettings` as a JSON string, including `appIdOrName` for third-party F-Droid repos and `overrideSource=FDroidRepo`.
- [x] Build package-scoped `ACTION_VIEW` for `dev.bikram.obtainx`, verify exported matching handler and catch Android launch errors.
- [x] No automatic launch, database writes, network calls, download queue or Install/Update interception.
- [x] Kotlin unit tests and Python static/contract regression tests.
- [ ] Exact-head hosted Gradle assembly, unit tests and lint for M2.
- [ ] Real-device ObtainX 2.21.0 import-preview verification for official, Izzy and third-party repositories; no saved-app claim from an Intent dispatch.
- [ ] UI invocation deferred to M3; M2 API alone cannot open ObtainX.

## M3 — UX & lifecycle
- Add Material-aligned chip, settings toggle and failure states.
- Verify no overlay leaks, no duplicate chips, no stale app identity or unexpected taps.
- Test light/dark/AMOLED, small-screen portrait, font scales, gesture navigation, Android 15/16.

## M4 — Production acceptance
- Pin exact upstream versions, test installed APKs, verify CI workflows and native Devtool receipts.
- Security review of URI encoding, escaping, redaction and cross-package launch.
- Release/signing/device qualification separately. Do not infer from local or hosted builds.

## Non-goals
No ObtainX DB writes, no system_server hooks, no silent installs, no root shell actions, no 24-shard Python Devtool regression attached to this separate Android repo.
