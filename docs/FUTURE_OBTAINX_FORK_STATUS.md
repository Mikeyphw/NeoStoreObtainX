# Deferred proposal — ObtainX fork read-only tracking status

**Status: DOCUMENTATION ONLY — NOT IMPLEMENTED OR AUTHORIZED FOR DEPLOYMENT.**

This is a future option for `Mikeyphw/ObtainX`, separate from the M7
NeoStoreObtainX UI change. The user's installed ObtainX is **not** this fork.
Do not build, merge, install, sign or modify ObtainX as part of M7.

## Goal and semantics
Provide an accurate, low-data, read-only answer to: "Is Android package
`<packageId>` currently represented by at least one tracked ObtainX listing?"
Distinguish (1) tracked, (2) definitely not tracked, (3) unknown/unavailable.
A package may have multiple store-specific listings. The answer MUST NOT claim
that the precise URL or selected source is already tracked merely because its
Android package ID is tracked. A matching source/listing may require an
additional, separately reviewed query contract.

## Candidate design for review
1. Audit the then-current installed ObtainX package/version and `Mikeyphw/ObtainX`
   fork at an exact pinned commit. Resolve signing continuity and upgrade
   compatibility **before** choosing deployment. Never replace an unrelated
   installed ObtainX silently or require data deletion.
2. Prefer an explicitly exported read-only interface owned by ObtainX (for
   example a minimal ContentProvider), gated by Android caller identity and a
   narrow package-level query. Confirm the real UID/package signature and
   export policy, rather than relying on the caller's supplied app ID. A
   non-exported provider cannot serve Neo Store; an overly broad exported
   provider would expose the user's app library.
3. Query from ObtainX's own consistent library state. Do not parse uncertain
   or half-written records as "not tracked." If initial load or storage access
   is incomplete, return UNKNOWN. No app names, version histories, repository
   URLs, credentials or entire library export.
4. Define a versioned response (`schema`, tri-state `tracked`) and bound query
   latency/size. Add negative tests for invalid IDs, wrong callers, race
   conditions, multiple sources, malformed records and provider failures.
5. Add a separate NeoStoreObtainX consumer only after companion support is
   released, installed and independently verified. Use asynchronous, bounded
   reads, route-generation invalidation, lifecycle cleanup, accessibility
   labels and conservative UNKNOWN on any failure.
6. At that future stage, choose badges only when ObtainX positively confirms
   state: check for TRACKED; optional plus for NOT_TRACKED; neutral for UNKNOWN.
   Never infer tracking from a launched deep link, an installed APK or a
   previously tapped button.

## Delivery order and acceptance
- Native Devtool source-pinned **ObtainX fork** PR, standalone hosted tests,
  Android build and signing-review gate. If certificate/update compatibility
  is not established, **do not install it over the existing application**.
- Verify fork's exported contract and security on a device first.
- Separate, source-pinned **NeoStoreObtainX** PR for the client; no dependency
  until the provider actually exists. Test absence/mismatch failures and
  device lifecycle/RTL/theme/fast route switches.
- Make live tracking status opt-in during its own acceptance. No automatic
  imports, no data mutation or background exfiltration.

For now, Neo Store's neutral branded icon always invokes ObtainX's existing
user-reviewed import/deep-link flow. Any tracked app handling occurs there.
