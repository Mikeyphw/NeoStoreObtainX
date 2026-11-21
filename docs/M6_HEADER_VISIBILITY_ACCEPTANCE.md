# M6 — Neo Store header icon visibility correction

Verified baseline: merged M5 main `76a42d293937b4c65eecefb9f6679083fee5b41e`.
Observed M5 user screenshot: `NSOX_M4_HOOKS=INSTALLED_R8_FIELDS`,
`NSOX_M1_PAGE_STATE=READY`, but no ObtainX icon beside the source action.
The M4 marker is also emitted by M5, so it **does not identify installed
module version**. Check `dumpsys package` before accepting a device result.

## Mechanism

Android `AccessibilityNodeInfo.getChild()` relies on accessibility service
connections and does not reliably traverse Compose's virtual descendants from
an app-local `root.createAccessibilityNodeInfo()` snapshot. Keep the semantic
anchor when available, but use a guarded pinned Neo Store 1.3.3 header layout
fallback only for a sufficiently wide window (>=365dp). Respect status bar
insets, root offsets and RTL, and retain 48dp accessibility target, IME/layout
checks, route binding, same-snapshot tap revalidation, and user-only launch.

Static LSPosed status markers (no package IDs or source URLs):
- `NSOX_M6_UI_ANCHOR=SEMANTIC`
- `NSOX_M6_UI_ANCHOR=PINNED_HEADER_FALLBACK`
- `NSOX_M6_UI_ANCHOR=NO_SAFE_HEADER_POSITION`
- `NSOX_M6_UI_ANCHOR=RENDER_CONDITIONS` / `NO_ROOM`

The fallback is an approximation, not an actual Compose child. It cannot
reflow the app title. Verify that it does not overlap source, Close, or long
app names. If overlap occurs, do not call this UI accepted; a separate Compose
integration or measured semantic geometry is required.

## Gates still pending
- [ ] Hosted compile, real Kotlin unit tests and lint for exact PR head
- [ ] Signed main debug/release APKs for exact merged M6 head
- [ ] Android installed versionCode=7 and pinned certificate
- [ ] LSPosed logs record `NSOX_M6_UI_ANCHOR=SEMANTIC` or `PINNED_HEADER_FALLBACK`
- [ ] New icon appears next to source control on portrait screenshots
- [ ] Taps invoke ObtainX preview only, with explicit Izzy adapter/source preference
- [ ] No source/Close control obstruction, no title overlap, long titles, rotation
- [ ] 360dp and below, Android font scaling, cutouts, RTL, TalkBack and themes

DEVICE_ACCEPTANCE=NOT_PERFORMED
RELEASE=NOT_PERFORMED
