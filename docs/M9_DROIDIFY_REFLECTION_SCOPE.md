# M9 — Droid-ify native hook resilience and LSPosed default scope

## Regression under test
The installed M8 hook emitted `NSOX_M8_DROIDIFY=HOOK_REFUSED_NoSuchMethodError` while attaching on Droid-ify 0.7.8/780. M8 used `XposedHelpers.callMethod(fragment, "getActivity")`, `getArguments` and `getMenu`; the broad exception log did not name the failed method. The precise missing method is **not yet proven** without device diagnostics.

## Mitigation
- Activity is resolved through the fragment root View ContextWrapper chain; refuse absent/ambiguous Activity.
- The MaterialToolbar is located in the host's View hierarchy. Public inherited no-argument `getMenu` is invoked using explicit Java reflection, not LSPosed's best-match helper.
- Fragment arguments prefer inherited `getArguments`, with a narrow `mArguments` fallback. Route still validates strict Android package ID.
- Every tap independently resolves fragment visibility, lifecycle and route, and rebuilds the source snapshot before dispatch.
- Structured log failures `NSOX_M8_DROIDIFY=ATTACH_REFUSED_<STAGE>_<TYPE>` identify the failing stage without logging sensitive IDs, URLs or metadata.
- The scope is now `android:resource="@array/xposed_scope"` with exactly two package entries, the recommended **legacy** LSPosed format. The manifest comma separator used in M8 was invalid for a value-based scope.

## Runtime and scope acceptance — REQUIRED
- [ ] Verify installed M9 package versionCode 10; check APK resources/manifest scope resolves to Neo Store (`com.machiav3lli.fdroid`) and Droid-ify (`com.looker.droidify`).
- [ ] In LSPosed Vector, open module scope and select both packages. Recommended scope metadata **does not forcibly change existing user selection**.
- [ ] Force-stop and reopen Droid-ify and Neo Store after scoping; verify LSPosed hooks loaded.
- [ ] Observe `NSOX_M8_DROIDIFY_HOOK=INSTALLED` and `NSOX_M8_DROIDIFY=NATIVE_TOOLBAR_ACTION_ADDED` on Droid-ify app page, or a new precise `ATTACH_REFUSED_*` marker.
- [ ] Check toolbar/overflow action; test known F-Droid, Izzy, GitHub upstream, unsupported/empty source, authenticated repository; no credentials may be exported.
- [ ] Verify no duplicate items across rotation, navigation and lifecycle. Check font scale, light/dark, TalkBack, landscape and small screens.
- [ ] Verify Neo Store M7 action remains functional. No ObtainX fork changes or tracking status feature.
- [ ] Separate hosted compile/unit/lint, signing workflow and on-device acceptance.

DEVICE_ACCEPTANCE=NOT_PERFORMED
RELEASE=NOT_PERFORMED
