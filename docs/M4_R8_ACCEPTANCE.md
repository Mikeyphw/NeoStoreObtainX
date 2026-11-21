# M4 — Neo Store 1.3.3 release R8 probe compatibility

Observed real-device failure on signed M3 v0.4.0: `NSOX_M1_PROCESS=NEO_STORE_ONLY`
followed by `NSOX_M1_REFUSED=SOURCE_SYMBOLS_N` (truncated diagnostic).
Application versions: Neo Store `com.machiav3lli.fdroid` 1.3.3 (1304),
ObtainX `dev.bikram.obtainx` 2.21.0 and module 0.4.0-m3-action (4).

The user's read-only installed-Dex audit (one DEX) confirmed:

- `NeoActivity.navStack` and `NeoActivity.onResume` present.
- `NavRoute$App.packageName` present, getter missing.
- `AppPageVM` retained constructor (arity 6), `packageName` and
  `coreAppState` fields present; `setApp` and getter missing.
- `CoreAppState.suggestedProductRepo`, `EmbeddedProduct.product`,
  `Product.packageName`/`repositoryId` and
  `Repository.id`/`address`/`authentication` fields present, getters missing.
- `kotlin.Pair.first`/`second` fields present, getters missing.
- `StateFlow.getValue` present.

M4 observes VM construction and reads the VM package StateFlow at sample time.
It chooses only a weakly held VM whose package matches the current NavRoute
and reads core state and selected repository from the fields. It retains
route identity and full snapshot revalidation on every tap, strict auth
refusal, host version gating, explicit user-only launches, and all M3 UI
cleanup/visibility rules. It makes no attempt to patch host R8 or to
rename source/signer identities.

## Acceptance status

- [x] Static probe design matches the provided installed DEX field inventory.
- [x] Repository Python contract suite (offline structural checks).
- [ ] Hosted Android compile, Kotlin unit tests and lint on exact new PR head.
- [ ] New merged main signing run succeeds for both variants.
- [ ] Same signing certificate as M3; downloaded APK v5 verified.
- [ ] LSPosed logs contain `NSOX_M4_HOOKS=INSTALLED_R8_FIELDS`, not `SOURCE_SYMBOLS`.
- [ ] Opening a supported app detail yields `NSOX_M1_PAGE_STATE=READY`.
- [ ] Button actually appears without overlapping host controls; clicking
      launches ObtainX preview, not an automatic add.
- [ ] Rapid route switching, authenticated repos, unsupported sources,
      no ObtainX, rotation, portrait, RTL, light/dark, and accessibility.

Do not merge this corrective PR without hosted CI. Do not claim device
acceptance or release qualification from static checks alone. The separate
signing compatibility PR #6 is not part of this patch.

DEVICE_ACCEPTANCE=NOT_PERFORMED
RELEASE=NOT_PERFORMED
