# M2 — Explicit ObtainX Intent bridge (no UI action yet)

## Exact source bindings

- `Mikeyphw/NeoStoreObtainX` M1 merged `main`: `2340a0cc85bb77dd2012e768c6080569dc10a1a6`; tree `7f2f09817e4ac53a947b1c68438d58f31176c46b`.
- Neo Store 1.3.3 (1304) pinned host: `NeoApplications/Neo-Store` commit `37f8ee314be6d1def7af11b0af5b85a10680645c`.
- ObtainX 2.21.0 pinned destination: `bikram-agarwal/ObtainX` commit `aea18592413de992e813bd063cb9ad453259582b`.
- Neo Store has the `QUERY_ALL_PACKAGES` permission in the pinned host manifest; M2 still refuses if Android does not report an exported matching destination handler.

## JSON and Intent contract

ObtainX's `lib/providers/source_provider.dart` `App.fromJson` reads `id`, `url`, `author`, `name`, `latestVersion`, `preferredApkIndex`, and **JSON-string** `additionalSettings`. For nonstandard F-Droid repositories, its `FDroidRepo` source is not auto-selected, so M2 carries `overrideSource: "FDroidRepo"` and `additionalSettings: "{\"appIdOrName\":\"<package>\"}"`. Official F-Droid and Izzy links use their dedicated source URLs.

`lib/pages/home.dart` routes `obtainium://app/<Uri-encoded JSON>` through `urlImportEntriesFromLink`, then opens ObtainX's import preview. A single already-tracked app may open its existing listing instead. Starting an Activity **does not** prove an app was saved; the user owns the final confirmation.

The bridge uses `Intent.ACTION_VIEW` with `CATEGORY_BROWSABLE`, **setPackage(`dev.bikram.obtainx`)**, exported handler resolution, exception handling, and fresh route-to-M1-snapshot source matching. It never uses ACTION_SEND, the Android share chooser, ObtainX internal DB, privileged APIs or network requests. It never changes Neo Store's Install/Update action.

The module **does not yet call the bridge**. The public `launchFromUserTap` entrypoint is reserved for M3's actual user-click handler. The current M1 lifecycle and logs remain unchanged. URI payloads and raw metadata are never sent to LSPosed diagnostics. Failures return an enum without sensitive strings.

## Independent acceptance gates

1. Native Devtool exact-tree overlay validate/check/apply, native commit history, session adoption, approved push, draft PR and source reconciliation.
2. Real GitHub Actions PR workflow (`425663365`), Android assembleDebug, Kotlin unit tests, lint and structural Python tests; exact commit/attempt binding.
3. On-device Android 15/16: pin Neo Store 1.3.3 and ObtainX 2.21.0, confirm matching package and handler, M1 READY, verify URI import preview only through a **separately reviewed manual test harness**, not a lifecycle-triggered launch.
4. Confirm official F-Droid, IzzyOnDroid, third-party repo, duplicate listing and malformed source cases; no auto-add or unexpected download; verify app back-stack return behavior.
5. UI/UX, portrait, multiwindow, large fonts, dark/AMOLED and real button semantics are M3. Signing and release are M4.

A passing Kotlin smoke compile or native Devtool transaction is not an Android-device acceptance receipt. No automatic PR merge, release or 24-shard Devtool repository regression is implied.
