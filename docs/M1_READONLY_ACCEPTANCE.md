# M1 — Read-only app-page/source detection

## Source contract and scope

Repository `Mikeyphw/NeoStoreObtainX`, source-bound to merged M0 main
`261e2cfeed184cdbc68c759ed0493acb7416a656`.

Pinned host `NeoApplications/Neo-Store` commit
`37f8ee314be6d1def7af11b0af5b85a10680645c`, Android application ID
`com.machiav3lli.fdroid`, versionName **1.3.3**, versionCode **1304**.
Observe only the host's primary process. Debug or `.neo` package variants
and unknown versions are deliberately unsupported for this milestone.

Source-level probes:
- `NeoActivity.onResume()` is a real override, not a Compose compiler bridge.
- Private `NeoActivity.navStack: NavBackStack<NavRoute>` is read, never mutated;
  top item must be `NavRoute.App` with `packageName` matching the current VM.
- `AppPageVM` constructor after-hook captures a bounded weak VM reference; release R8 removed `setApp(String)` as an invokable method.
- `coreAppState` field exposes StateFlow; `suggestedProductRepo` field yields
  `Pair<EmbeddedProduct, Repository>`.
- `EmbeddedProduct.product.packageName`, `Product.repositoryId`,
  `Repository.id`, and `Repository.address` are checked before source mapping.
- On mismatch, keep state `WAITING`; do not show stale repository information.
- On unsupported/malformed source, state `UNSUPPORTED_SOURCE` without raw URLs.

Polling (600 ms while NeoActivity resumed) is bounded and cancels on pause;
no background service, no persistence, no permissions, no new dependencies.
The main-thread probe is intentionally temporary; M2/M3 should replace this
with a narrowly verified event mechanism if device performance warrants it.

## Log and security contract

Expected LSPosed logs contain only the symbolic `NSOX_M1_*` markers:
`NSOX_M1_HOOKS=INSTALLED_READ_ONLY`, transitions
`NSOX_M1_PAGE_STATE=IDLE|WAITING|READY|UNSUPPORTED_SOURCE`, and explicit
`NSOX_M1_REFUSED=...` on unsupported source/runtime versions. No package name,
repo URL, authentication, fingerprint or user secrets are emitted.

The module **never** calls ObtainX, changes Neo Store UI, invokes
`DownloadWorker`, performs a network fetch, or reads/writes ObtainX storage.
`READY` means only that *in-memory route/source metadata agrees*; it does not
mean ObtainX import or Android runtime validation passed.

## Real-device acceptance (separate, pending)

Required test environment: LSPosed enabled, module scoped to the exact
`com.machiav3lli.fdroid` package, official Neo Store 1.3.3 (1304) installed.
Use LSPosed module diagnostics / scoped logcat to confirm:
1. Open an official F-Droid app page and observe `WAITING → READY`.
2. Open an IzzyOnDroid app and a valid custom HTTPS repository app; ensure
   only matched source/repository is observed.
3. Rapidly navigate between two packages; ensure no stale source is accepted.
4. Back out of an app, navigate to Search/Settings, and background/foreground
   the app; observe `IDLE` and resume without duplicate observers.
5. Rotate/recreate the Activity; verify only one lifecycle callback/poll loop.
6. Open a malformed/credentialed source and verify fail-closed behavior.
7. Test unsupported Neo Store version; no observation hooks should activate.
8. Verify Neo Store Install/Update and downloads work unchanged.

Collect a redacted `NSOX_M1_*` diagnostic summary and the actual APK identity.
Sensitive source URLs or full LSPosed logcat must not be copied to clipboard.

## Native/hosted acceptance boundaries

1. Pinned clean named feature worktree and exact-tree overlay inventory.
2. `devtool validate-overlay` and `devtool overlay check` on trusted Devtool.
3. `devtool apply-overlay --validate` with real Python checks and native receipt.
4. `adopt-committed` verifies the unique source-bound native commit.
5. Approved non-force native push and exact PR reconciliation.
6. `delivery session ci` verifies the committed base policy and real workflow ID.
7. Android-device acceptance is separate. **No merge or release qualification**
   is performed by the operator automatically.

M4 corrective note: Neo Store 1.3.3 release optimizes away getters and `setApp`.
The module must use audited declared fields and route-matched StateFlow values;
missing fields refuse injection. The installed release DEX audit is documented in `M4_R8_ACCEPTANCE.md`.
