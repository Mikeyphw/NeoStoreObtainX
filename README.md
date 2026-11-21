# NeoStoreObtainX

A Neo Store–scoped LSPosed module to transfer selected app listings to ObtainX while keeping repository identity intact.

> **Bootstrap stage only**: The packaged Xposed entrypoint verifies Neo Store process scoping and logs a diagnostic. It does **not yet** inject a button, intercept downloads, start ObtainX, or install anything. Those behaviors require separately reviewed and device-validated milestones.

## Scope

- Host: Neo Store `com.machiav3lli.fdroid`.
- Destination: ObtainX `dev.bikram.obtainx` (external Android Intent only; **no** database writes).
- Workflow: source-bound Devtool overlay → native validation/commit → durable session adoption → approved push → draft PR → exact-head CI.
- Default behavior after future implementation: retain Neo Store's Install/Update action and add a distinct **Add to ObtainX** button.

See [audit](docs/SOURCE_AUDIT.md), [implementation plan](docs/IMPLEMENTATION_PLAN.md), and [Devtool operations](docs/DEVTOOL_DELIVERY.md).

## Build / verification

The bootstrap does **not** require Gradle on Termux. Hosted CI uses JDK 17, Android SDK 35, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20.

```bash
python3 -B -m unittest discover -s tests -p 'test_*.py' -v
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Gradle is expected from a trusted local installation or the pinned GitHub Actions setup; a wrapper binary is not invented or downloaded by the handoff.

No release or Android-device acceptance is implied by a passing CI build.
