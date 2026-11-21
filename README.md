# NeoStoreObtainX

A Neo Store–scoped LSPosed module to transfer selected app listings to ObtainX while keeping repository identity intact.

> **M2 bridge stage**: the LSPosed entrypoint still only observes Neo Store 1.3.3 read-only. A source-validated ObtainX deep-link bridge has been added for future explicit user interaction. It is **not yet wired to any button or lifecycle callback** and does not automatically open ObtainX, download, install or save anything. Hosted and device acceptance remain separate.

## Scope

- Host: Neo Store `com.machiav3lli.fdroid`.
- Destination: ObtainX `dev.bikram.obtainx` (external Android Intent only; **no** database writes).
- Workflow: source-bound Devtool overlay → native validation/commit → durable session adoption → approved push → draft PR → exact-head CI.
- Default behavior after future implementation: retain Neo Store's Install/Update action and add a distinct **Add to ObtainX** button.

See [audit](docs/SOURCE_AUDIT.md), [implementation plan](docs/IMPLEMENTATION_PLAN.md), and [Devtool operations](docs/DEVTOOL_DELIVERY.md).

## Build / verification

The M2 native overlay does **not** require Gradle on Termux; hosted CI runs the real Android build. Hosted CI uses JDK 17, Android SDK 35, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20.

```bash
python3 -B -m unittest discover -s tests -p 'test_*.py' -v
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Gradle is expected from a trusted local installation or the pinned GitHub Actions setup; a wrapper binary is not invented or downloaded by the handoff.

No release or Android-device acceptance is implied by a passing CI build.

## M1 read-only probe and M2 intent bridge

Read [the M1 acceptance plan](docs/M1_READONLY_ACCEPTANCE.md) for exact version gating, lifecycle behavior, and privacy. M1 remains read-only.

M2 implements a pure ObtainX JSON-seed policy and a strictly package-scoped Android VIEW bridge to ObtainX `dev.bikram.obtainx`. Its handler is resolved before launch; **launchFromUserTap** is exposed only to a future M3 explicit click action, never invoked by M1 polling. ObtainX owns the import preview, duplicate handling and final save. See [M2 acceptance](docs/M2_INTENT_BRIDGE_ACCEPTANCE.md). M3 will add the button and UI lifecycle validation; M4 remains production acceptance.

## Signed APK downloads

Only the trusted `main` signing workflow publishes APKs. Debug and release use the same persistent project certificate. PR CI still builds and tests both variants, but never publishes APKs signed with the project key. See [Signing and downloads](docs/APK_SIGNING.md).
