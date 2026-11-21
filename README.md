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
