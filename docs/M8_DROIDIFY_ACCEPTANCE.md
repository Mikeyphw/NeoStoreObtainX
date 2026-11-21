# M8 — Droid-ify 0.7.8 / ObtainX native toolbar integration

## Source and identity
- NeoStoreObtainX baseline exact main `b1abcc4fe20393bea61e9f272fa6c8918e82aa34` (tree `b5280ab39384e6b59183533186a363e9ce59ee05`).
- Public Droid-ify source: `Droid-ify/client` source commit `ff1453ed957f3b2382abce9a7eb9a9db8def3a3a` (tree `5ecefe5bfbc77e68d3cff486b162e2d7ee156983`), version 0.7.8, appId `com.looker.droidify`, code 780. Its MainActivity launches `ui.appDetail.AppDetailFragment` and its release proguard config has `-dontobfuscate` (though optimization can still affect fields).
- Fragment `onViewCreated` populates MaterialToolbar native Menu and product/repository pairs. Its source/share actions select products[0], matching M8's choice. `ARG_PACKAGE_NAME = package_name`, `ARG_REPO_ADDRESS = repo_address`.
- ObtainX bridge package `dev.bikram.obtainx` remains unchanged. No fork tracking status feature.

## Behavior
- Hook only the Droid-ify main process and verified 0.7.8/780 app-detail Fragment.
- One native MaterialToolbar `MenuItem` (if room), not an overlay. Android/host controls layout, so the item may move to the overflow menu on compact screens.
- Use ObtainX launcher icon when Android package visibility allows; fallback is a download glyph.
- On **user tap only** check activity focus, fragment lifecycle, exact package route, live selected product ID and repository ID, repository URL, and absence of authentication. Reject missing or changed identity.
- Prefer validated upstream supported source (GitHub/GitLab/etc.). Fall back to exact F-Droid/Izzy/custom repo mapping. ObtainX alone decides import versus already tracked.
- No automatic install, tracking query, repo credential export, direct host repository reads/writes, or ObtainX process hooks.

## Separate acceptance gates
- [ ] M8 native source-pinned build, validate, apply, durable session and required hosted CI.
- [ ] Validate installed Droid-ify package version `0.7.8`/780 and actual release DEX names/fields.
- [ ] Verify LSPosed scope for both Neo Store and Droid-ify. Restart host.
- [ ] Open Droid-ify app page and confirm native menu action appears or is in overflow.
- [ ] Test F-Droid and IzzyOnDroid pages, source-first GitHub case and custom repository fallback.
- [ ] Tap missing-source page, changed route, backgrounded host and authenticated repo; must refuse without launching.
- [ ] Check device logs: `NSOX_M8_DROIDIFY_HOOK=INSTALLED`, `NSOX_M8_DROIDIFY=NATIVE_TOOLBAR_ACTION_ADDED` and `IMPORT_PREVIEW_DISPATCHED` on explicit tap. No IDs/URLs logged.
- [ ] Recheck Neo Store M7 action unchanged and ObtainX duplicate routing unchanged.
- [ ] Check narrow portrait, landscape, font scaling, RTL, TalkBack, dark/light themes and long app names.
- [ ] If the host instead uses Compose app details, mark unsupported until separately pinned, do not infer route from text.

DEVICE_ACCEPTANCE=NOT_PERFORMED
RELEASE=NOT_PERFORMED
