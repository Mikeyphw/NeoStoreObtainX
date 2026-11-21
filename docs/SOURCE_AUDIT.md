# Source audit (2026-11-21)

Pinned upstream references read during the bootstrap:

- Neo Store `NeoApplications/Neo-Store`, `master`, SHA `37f8ee314be6d1def7af11b0af5b85a10680645c` (app version 1.3.3).
- ObtainX `bikram-agarwal/ObtainX`, `main`, SHA `aea18592413de992e813bd063cb9ad453259582b` (release 2.21.0).

| Component | Verified path | Integration consequence |
| --- | --- | --- |
| Neo Store page | `src/main/kotlin/com/machiav3lli/fdroid/ui/pages/AppPage.kt` | AppPage receives packageName, reads coreAppState |
| Neo Store header | `src/main/kotlin/com/machiav3lli/fdroid/ui/components/appsheet/Header.kt` | AppInfoHeader is Compose, not a ViewGroup |
| Neo Store state | `src/main/kotlin/com/machiav3lli/fdroid/viewmodels/AppPageVM.kt` | CoreAppState includes suggestedProductRepo and productRepos |
| ObtainX incoming links | `lib/pages/home.dart` | `obtainium://add` opens an add form; `obtainium://app` opens import preview |
| ObtainX link manifest | `android/app/src/main/AndroidManifest.xml` | VIEW of scheme `obtainium` registered |
| ObtainX source | `lib/app_sources/fdroidrepo.dart` | Custom repos require appId and FDroidRepo override |
| ObtainX applicationId | `android/app/build.gradle.kts` | `dev.bikram.obtainx` (not Flutter namespace `dev.imranr.obtainium`) |

No direct ObtainX private-store mutation and no fabricated accepted-import claim.
Source pins are for planning; installed APKs must be independently matched before runtime hooks are enabled.
