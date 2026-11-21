# M7 — Branded header icon, no tracking-status integration

Base: NeoStoreObtainX merged M6 main `fac837e5fceb4dd310704bb6d321c69dbf535b2e`.
Module candidate version 0.7.0-m7-branded-icon, versionCode 8.

## Scope
- Only modify NeoStoreObtainX: UI drawable, app version, tests and docs.
- The installed ObtainX remains unchanged. No provider, no content query, no
  polling, no tracked/untracked badge or read of ObtainX's app library.
- Try the Android package manager's app launcher icon for `dev.bikram.obtainx`.
  When unavailable through package visibility or not installed, draw a neutral
  download-arrow-to-tray fallback. Do not claim the icon was found until device
  testing confirms the drawable.
- Make the background 40dp within a 48dp tappable target with ripple and a
  neutral accessible label: `Open in ObtainX` (Portuguese translation included).
- Preserve the M6 pinned Neo Store 1.3.3 header fallback and visual safety
  checks, text/settings preference, long-press, stale-route/current-source
  verification, IzzyOnDroid adapter and source-site preference.
- A successful tap only dispatches an intent. ObtainX manages duplicate
  detection and already-tracked app navigation; there is no instant status
  badge or tracking-state promise in Neo Store.

## Independent future roadmap
The unimplemented ObtainX fork integration is in
`docs/FUTURE_OBTAINX_FORK_STATUS.md`. It MUST NOT be included in this PR.

## Outstanding acceptance gates
- [ ] Exact-head hosted Android compilation, Kotlin unit tests, lint and checks.
- [ ] Merged-main signed build and signer-certificate continuity; install code 8.
- [ ] Installed ObtainX launcher icon appears when Android permits it, otherwise
      fallback glyph appears without a misleading plus or checkmark.
- [ ] M6 header anchor does not obscure the original source-code or close action.
- [ ] Dark/light/pure-black, Portuguese/English, RTL, scaling, landscape,
      rotation, large font and TalkBack.
- [ ] Explicit tap opens ObtainX; existing tracked app is handled by ObtainX.
- [ ] No new host mutations, hidden reads, duplicate imports or data leakage.

DEVICE_ACCEPTANCE=NOT_PERFORMED
RELEASE_QUALIFICATION=NOT_PERFORMED
