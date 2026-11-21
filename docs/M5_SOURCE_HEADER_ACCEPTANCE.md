# M5 — Source-first import and header integration

Baseline main: `73ede553bd65802254ce36109ef80d30c5280546`.
ObtainX source: `bikram-agarwal/ObtainX`, `lib/app_sources/izzyondroid.dart`
and `lib/providers/source_provider.dart`; Neo Store 1.3.3 app-page header uses
`TopBarHeader > Row > SourceCodeButton + RoundButton(Close)`.

## Source selection contract

- App `Product.source` (or `Product.web` when source is blank) is read from the current, route-matched R8-retained field.
- Supported, canonical HTTPS repository roots are preferred for GitHub, GitLab,
  Codeberg, SourceHut and SourceForge. Unrecognized hosting URLs, app pages,
  file URLs, credentials, query strings and fragments fall back to the *exact*
  selected Neo Store repository, without trying a default store.
- The source snapshot, URI, source override and route are revalidated on tap.
- Izzy listing uses `overrideSource=IzzyOnDroid` and `appIdOrName=<package>`.
  ObtainX has this dedicated adapter; a generic FDroidRepo rewrite is incorrect.
- Host-level source support is not proof that an app publishes APKs there.
  ObtainX's own import preview must verify availability, version and duplicates.
- No background retrieval, automatic import, secret transfer or direct DB writes.

## Header UI contract

The host source-code action is a Compose semantic virtual node, not an Android
View. M5 uses its localized accessibility description and on-screen bounds to
anchor a *separate module-owned* Android ImageButton directly beside it. The
48dp target has a plus glyph, circular Material-like tonal fill, ripple,
accessibility label and long-press settings. The old bottom-floating text is
removed. The source and Close buttons keep their original listeners.

**Important limitation:** this does not insert a Compose child or reflow the
app-name label. The icon refuses to appear when there is insufficient space,
or if virtual semantics do not expose the source-code button. On-screen testing
is essential; a separate source-native Compose approach would be required for
perfect theme matching and responsive reflow.

## Acceptance — NOT PERFORMED

- [ ] Required exact-head hosted Android CI, Kotlin unit tests, lint.
- [ ] Signed main build retains known certificate; verified version 6 APK.
- [ ] Izzy listing imports through `IzzyOnDroid`, no generic F-Droid override.
- [ ] GitHub, GitLab, Codeberg actual source-code link is preferred.
- [ ] Unsupported/credentialed website falls back to selected store listing.
- [ ] Linked repository without releases reports ObtainX's own failure, no auto-add.
- [ ] Icon beside source code button; original source button still works.
- [ ] No title overlap, small portrait, large font, landscape, RTL, cutouts.
- [ ] Light/dark/pure-black tonal fit, TalkBack labels and 48dp target.
- [ ] No stale taps, duplicate buttons, credential leakage or unexpected imports.

DEVICE_ACCEPTANCE=NOT_PERFORMED
RELEASE=NOT_PERFORMED
