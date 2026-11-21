# M0 CI repair and admission — 2026-11-21

## Bound source and event

- Target repository: `Mikeyphw/NeoStoreObtainX`
- PR: #1, `feature/lsposed-bootstrap-20261121` to `main` (draft)
- Repaired source HEAD: `3e108e1b2d9512bc14725615db543a8194bfc73a`
- Repaired source tree: `91d4a303a8ea1bf1ca39a80c9369d4e419a6c617`
- Original failing PR workflow: `NeoStoreObtainX CI`, workflow ID `425663365`, run ID `44862291517`.
- Observed: `Contract tests` success; Android job failed in `android-actions/setup-android@v3` during `sdkmanager tools`, before any Gradle work.

## Change

- Replace `android-actions/setup-android@v3` with `@v4` (no retired `tools` installation), explicit `packages: 'platform-tools'`.
- Preserve the Android 35 SDK installation and **real** `assembleDebug`, `testDebugUnitTest`, and `lintDebug` tasks plus APK artifact check.
- Add Python regression checks for workflow and committed required-CI policy.
- Introduce `config/delivery-required-ci.json` with the *observed* numeric GitHub workflow ID `425663365`; do not forge required checks or source identities.

## Initial-repository admission caveat

The first draft PR's base branch does not yet have a committed required-CI policy. The repair adds that policy **to the PR branch**, so after this PR is merged `main` will have a policy for later native delivery qualification. For the first PR, we conservatively treat initial required-CI governance as **NOT QUALIFIED** until the policy is independently approved and committed on the **base branch**. The present same-repository Devtool CI loader reads the committed policy from the feature HEAD and might produce a native CI observation, but that alone does not prove independent base-branch policy approval. Do not overstate this bootstrap exception as a Devtool-enforced check. Do not weaken the guard, cherry-pick straight to main, or fabricate a qualifying receipt. Independently examine real hosted jobs and the exact PR head, GitHub branch/ruleset settings and review decisions before any merge. A new post-merge PR can use the base policy for normal native qualification.

This M0 module still has no app-page UI hook. Android device acceptance, LSPosed on-device loading, signing, and release qualification are separate and **NOT PERFORMED**. The SDK setup correction is source-level until a new hosted run confirms it.

## Native flow

1. Verify the exact original feature SHA/tree, clean worktree, expected GitHub origin, open draft PR #1, and current GitHub workflow ID.
2. Build the exact-tree schema-v2 ZIP using only the declared four payload files. Do not modify the repository to build the package.
3. Run native `validate-overlay`, `overlay check` and `apply-overlay --validate --keep-artifact` using Devtool's native transaction and required real Python tests.
4. Adopt committed native history. Recover an interrupted commit by read-only history/status and adoption; never re-apply blindly.
5. Use native journaled `delivery session push/pr`; independently verify the PR points to the new HEAD.
6. Read the actual hosted checks and wait for all required jobs to pass before deciding whether to merge.

No force push, automatic merge, workflow dispatch, release claim, or GitHub API direct file edits are authorized by this repair.
