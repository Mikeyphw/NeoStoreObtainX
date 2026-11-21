# M3 — Add to ObtainX action (candidate)

Base: signing-enabled main `39d5ff87eced9c715bde9a438d0662428c5e0626` (merge PR #4).
Both baseline workflows passed; M3 must preserve signed-only publication.
M3 PR CI builds both variants without production credentials and publishes no APK.
After merge, use `bash "$HOME/.local/bin/nsox-apk" debug` or `release`
to obtain the new verified signed APK; check its printed HEAD against merged M3.
Pinned host: Neo Store 1.3.3 (1304), `com.machiav3lli.fdroid`, main process.
Pinned receiving contract: ObtainX 2.21.0, `dev.bikram.obtainx`; the bridge's
package and exported-handler validation is unchanged. The runtime package identity
is not a signing-certificate attestation; exact installed APK acceptance remains separate.

## UI and lifecycle

A small high-contrast rounded native Button is attached to the activity's existing
`android.R.id.content` FrameLayout, bottom-end, with system bar/display cutout
insets. It uses the host configuration's font scale, RTL end alignment, 48dp
minimum touch height, and EN / pt-BR text. It is intentionally not a Compose
header insertion; real-device placement, theme and overlap checks are mandatory.
Nothing replaces the existing install/update widgets or mutates their listeners.
It consumes touch only in its own bounds. No extra permissions or app dependency.

Long-press opens a native settings dialog. Choose "Show settings icon only" to
remove the action label; the small gear remains on eligible pages to re-enable
it. The sole persisted setting is a boolean in a module-namespaced private
SharedPreferences file inside the host's own sandbox. No credentials or app
history are persisted. Disable the module in LSPosed to remove all UI.

Every poll checks the current route and metadata. A new route/source removes the
old button before inserting another. Pause, destruction, no focus, IME, unsupported
source or missing root remove the button. Settings dialog closes on route change
or lifecycle stop. Polling remains 600ms while resumed; the click handler re-reads
route and metadata synchronously, verifies route object identity and full snapshot,
checks current generation, attachment, focus, enabled setting and debounce, then
calls the original bridge exactly once. No automatic launch, no chooser, no fallback
handler. An Intent dispatch is not an import/save claim. Refusals show nonsensitive
localized messages. Runtime logs use only symbolic phase/error/outcome markers.
The repository authentication field must be empty (even if its URL looks safe).
Official and Izzy site adapters reject archive/alternate paths and nonstandard ports
instead of silently switching to the main repository.
Host `kotlin.Pair` is read via host-classloader reflection, not module casts.

## Device acceptance — not performed

Use the exact CI artifact on LSPosed scoped only to Neo Store. Confirm versionName
and versionCode, APK provenance, exact scope and ObtainX installed identity.

| Scenario | Expected |
| --- | --- |
| List/search/settings/back | No action on non-app routes |
| Official F-Droid, Izzy and custom HTTPS repo | Ready page action; ObtainX preview retains chosen source |
| Credentialed repository | No action; no credentials in logs or Intent |
| Tap then cancel import | No saved-app claim; Neo Store unaffected |
| Existing ObtainX listing | ObtainX's own duplicate behavior |
| ObtainX absent/disabled | Friendly failure, no fallback or crash |
| Rapid app A/B navigation and stale tap | Refusal or fresh current listing only |
| Rapid double tap | At most one dispatch in debounce interval |
| Background/resume/rotate, multiwindow | No leaked or duplicated buttons/dialogs |
| Light/dark/Neo Store AMOLED | Contrast and theme fit; inspect real screen |
| Small portrait screen, large font, RTL, cutouts, gesture navigation | Button readable; no overlap with primary controls, dialogs or IME |
| TalkBack | Label, activation, long-press settings accessible |
| Hide/re-enable | Gear only, then action returns; setting persists across restart |
| Download/install/update | Original Neo Store actions unchanged |
| Neo Store version mismatch | Fail closed, no enabled action |

If overlap or accessibility issues occur, do not qualify the UI; adjust position
or pursue a separately audited source-native Compose integration. Capture only
redacted symbolic NSOX markers and non-sensitive screenshots. No root/device
commands, install, merge or release are performed by this overlay operator.

DEVICE_ACCEPTANCE=NOT_PERFORMED
RELEASE=NOT_PERFORMED
