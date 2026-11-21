# NeoStoreObtainX

A Neo Store–scoped LSPosed module to transfer selected app listings to ObtainX while keeping repository identity intact.

> **M3 candidate**: a separate native Add to ObtainX action is shown only for a matching supported page/source. It opens ObtainX for user review, never installs or writes its database. Device acceptance is pending.

## Scope

- Host: Neo Store `com.machiav3lli.fdroid`.
- Destination: ObtainX `dev.bikram.obtainx` (external Android Intent only; **no** database writes).
- Workflow: source-bound Devtool overlay → native validation/commit → durable session adoption → approved push → draft PR → exact-head CI.
- M3 candidate behavior: retain Neo Store's Install/Update action and add a distinct **Add to ObtainX** button.

See [audit](docs/SOURCE_AUDIT.md), [implementation plan](docs/IMPLEMENTATION_PLAN.md), and [Devtool operations](docs/DEVTOOL_DELIVERY.md).

## Build / verification

The M3 native overlay does **not** require Gradle on Termux; hosted CI runs the real Android build. Hosted CI uses JDK 17, Android SDK 35, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20.

```bash
python3 -B -m unittest discover -s tests -p 'test_*.py' -v
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Gradle is expected from a trusted local installation or the pinned GitHub Actions setup; a wrapper binary is not invented or downloaded by the handoff.

No release or Android-device acceptance is implied by a passing CI build.

## M1 read-only probe and M2 intent bridge

Read [the M1 acceptance plan](docs/M1_READONLY_ACCEPTANCE.md) for exact version gating, lifecycle behavior, and privacy. M1 describes the historical read-only probe; M3 adds its own action controller.

M2 implements a pure ObtainX JSON-seed policy and a strictly package-scoped Android VIEW bridge to ObtainX `dev.bikram.obtainx`. Its handler is resolved before launch; **launchFromUserTap** is invoked only through M3’s explicit click handler after a fresh route/source check, never directly by the polling loop. ObtainX owns the import preview, duplicate handling and final save. See [M2 acceptance](docs/M2_INTENT_BRIDGE_ACCEPTANCE.md). M3 adds the button and lifecycle guards; device testing and M4 production acceptance remain pending.

## M3 action

See [M3 acceptance](docs/M3_UI_ACCEPTANCE.md) for placement, long-press settings, fresh tap validation and real-device tests.

## Signed APK downloads

Only the trusted `main` signing workflow publishes APKs. Debug and release use the same persistent project certificate. PR CI still builds and tests both variants, but never publishes APKs signed with the project key. See [Signing and downloads](docs/APK_SIGNING.md).

## Neo Store release R8 compatibility (M4 corrective candidate)

The actual Neo Store 1.3.3 release APK removes Kotlin getters and
`AppPageVM.setApp` that the original M3 probe expected. M4 uses retained
DEX fields with `AppPageVM` constructor hooks and matches the route against
`packageName` StateFlow on each poll/tap. Version 0.5.0-m4-r8-fields (5) must
pass device acceptance before any UI/ObtainX success is claimed. See
`docs/M4_R8_ACCEPTANCE.md`.


## M5 — Source-first import and header icon candidate

For Neo Store 1.3.3, prefer the source-code repository when ObtainX has a
known adapter (GitHub, GitLab, Codeberg, SourceHut, SourceForge); otherwise
preserve the active Neo Store listing. IzzyOnDroid explicitly uses its own
adapter and app ID to avoid generic FDroidRepo import migration. A native
48dp icon is anchored beside the Compose source-code accessibility node;
no host Compose mutation or bottom-floating action remains. Header colors
are approximated with system tonal values, not read from private Compose state.
The icon hides when the anchor cannot be located safely, including small
screens; it is not claimed to be a Compose child. See M5 acceptance docs.


## M6 — Guarded header placement recovery

M5's `root.createAccessibilityNodeInfo()` traversal cannot reliably access
Compose virtual descendants from an ordinary in-process View. M6 adds a guarded
fallback based on the pinned Neo Store 1.3.3 header geometry, while retaining
semantic anchoring when it works. It logs **only static anchor states** such as
`NSOX_M6_UI_ANCHOR=PINNED_HEADER_FALLBACK`. The icon is still a separate native
View; it is not a Compose child and must pass actual device visual acceptance.
No floating-bottom fallback or host UI mutation. Version 0.6.1 (code 7).


## M7 — ObtainX-branded header action (module-only candidate)

The Neo Store header action now tries to display ObtainX's installed launcher
icon, falling back to a download-to-tray glyph when package visibility prevents
reading it. Its tonal circle is 40dp within a 48dp touch target, preserving
M6's guarded header position. It is a neutral **Open in ObtainX** action:
there are no tracking-status probes, badges, exported providers, or changes
to ObtainX. ObtainX itself resolves existing listings after user taps.

A future, independent status API for the user's ObtainX fork is documented in
`docs/FUTURE_OBTAINX_FORK_STATUS.md`. It is not implemented or a dependency.
M7 device and release acceptance remain pending.


## Droid-ify host integration (M8)

In addition to the existing Neo Store 1.3.3 hook, the module can attach an
**explicit** native action to Droid-ify 0.7.8 (`com.looker.droidify`, versionCode
780) app-detail toolbars. Enable **both** packages in LSPosed scope and restart
Droid-ify after installing module 0.8.0-m8-droidify (versionCode 9).

The native toolbar action is offered when Droid-ify is showing an app-detail
fragment. On tap, it re-reads that fragment's current package and selected
Product/Repository pair. A matching public HTTPS repository or supported
upstream source is sent through the same ObtainX import-preview-only bridge as
Neo Store. Missing metadata, a changed route, authenticated repositories or
unsupported host versions are rejected. No host data, install queues, or
private app library is modified. ObtainX's existing duplicate detection remains
responsible for previously tracked apps. No live tracked-status query is added.

This initial pin is based on Droid-ify's public v0.7.8 source. Other versions,
including the experimental Compose detail implementation, are *not* claimed
supported. `NSOX_M8_DROIDIFY` markers do not include package IDs or URLs.

Device testing and hosted Android compilation remain required for acceptance.
See `docs/M8_DROIDIFY_ACCEPTANCE.md`.

Droid-ify source pinned for M8: `Droid-ify/client` source commit `ff1453ed957f3b2382abce9a7eb9a9db8def3a3a` (tree `5ecefe5bfbc77e68d3cff486b162e2d7ee156983`).


## M9 — Droid-ify compatibility and recommended LSPosed scope

The Droid-ify 0.7.8/780 app-detail hook uses view ContextWrapper and inherited reflection
instead of fragile Xposed helper best-match attachment calls, with failure-stage logs.
The legacy LSPosed recommended scope is now `@array/xposed_scope` with exactly
Neo Store and Droid-ify. Existing user-selected scope still needs checking in
LSPosed Vector. See `docs/M9_DROIDIFY_REFLECTION_SCOPE.md`.
