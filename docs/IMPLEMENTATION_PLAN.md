# Implementation milestones

## M0 — Bootstrap (merged in PR #1)
- [x] LSPosed Neo Store-only scope / no-op diagnostic entrypoint.
- [x] Source mapper with official/Izzy/third-party distinction and negative tests.
- [x] Hosted structural, Kotlin unit, Android assembly and lint jobs.
- [x] Hosted jobs verified at exact PR HEAD (`992663658dffc4f93b66d72f9fdd93f913d83bc7`); main merge `261e2cfeed184cdbc68c759ed0493acb7416a656`.
- [ ] Device check on real installed Neo Store/ObtainX versions (not performed).

## M1 — Read-only page detection (this candidate)
- [x] Pin source contract to Neo Store 1.3.3 / versionCode 1304; fail closed for other installed versions.
- [x] Observe `NeoActivity.onResume` + `AppPageVM.setApp` without hooking Compose compiler-generated code.
- [x] Read activity navigation stack and suggested product/repo, accepting metadata only when IDs match.
- [x] In-memory only; no UI, Intent, downloads, DB writes or sensitive diagnostic logs.
- [x] Add pure Kotlin policy and structure regression tests.
- [ ] Hosted Android compile, unit tests and lint at exact M1 PR HEAD.
- [ ] On-device LSPosed test on supported Neo Store APK: navigation, back, background, rotation and stale metadata.
- [ ] UI acceptance on portrait/multiwindow/large-font/dark/AMOLED is deferred to M3 (no M1 view).

## M2 — Intent bridge
- Prefer `obtainium://app/<encoded-JSON>` for custom repos, using minimal proven `App.fromJson` seed fields.
- Set Android target package to `dev.bikram.obtainx`; resolve handler before launch; never expose credentials.
- Duplicate handling belongs to ObtainX; verify import preview instead of treating startActivity as saved.
- Keep Install/Update intact; action is additive, never queues `DownloadWorker`.

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
