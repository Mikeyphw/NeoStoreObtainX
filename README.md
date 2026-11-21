# NeoStoreObtainX

A Neo Store–scoped LSPosed module to transfer selected app listings to ObtainX while keeping repository identity intact.

> **M1 read-only detection**: the LSPosed entrypoint observes Neo Store 1.3.3 navigation and its matching app/repository metadata, logging only state transitions. It does **not** display a button, intercept downloads, start ObtainX, or install anything. Runtime behavior requires device acceptance.

## Scope

- Host: Neo Store `com.machiav3lli.fdroid`.
- Destination: ObtainX `dev.bikram.obtainx` (external Android Intent only; **no** database writes).
- Workflow: source-bound Devtool overlay → native validation/commit → durable session adoption → approved push → draft PR → exact-head CI.
- Default behavior after future implementation: retain Neo Store's Install/Update action and add a distinct **Add to ObtainX** button.

See [audit](docs/SOURCE_AUDIT.md), [implementation plan](docs/IMPLEMENTATION_PLAN.md), and [Devtool operations](docs/DEVTOOL_DELIVERY.md).

## Build / verification

The M1 native overlay does **not** require Gradle on Termux; hosted CI runs the real Android build. Hosted CI uses JDK 17, Android SDK 35, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20.

```bash
python3 -B -m unittest discover -s tests -p 'test_*.py' -v
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Gradle is expected from a trusted local installation or the pinned GitHub Actions setup; a wrapper binary is not invented or downloaded by the handoff.

No release or Android-device acceptance is implied by a passing CI build.

## M1 read-only probe

Read [the M1 acceptance plan](docs/M1_READONLY_ACCEPTANCE.md) for exact version gating, lifecycle behavior, privacy, and on-device validation. No UI is injected until M2/M3.
