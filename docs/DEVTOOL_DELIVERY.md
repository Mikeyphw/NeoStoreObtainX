# Devtool-native bootstrap / delivery contract

This project is provisioned using `gh repo create` for the **empty remote only**. All subsequent source-file mutations are delivered as one source-bound Devtool overlay. No manual `git add/commit/push`, forced branch rewrite or ad-hoc GitHub file patch is allowed.

1. Provision `Mikeyphw/NeoStoreObtainX` with GitHub's initial README, clone and switch to the clean named bootstrap feature branch.
2. Build a manifest-v2 `exact-tree` overlay in user-private storage. Bind the exact origin, branch, HEAD and tree, every new path and SHA-256.
3. Native `devtool validate-overlay` and `devtool overlay check` must pass.
4. Native `devtool apply-overlay --validate --keep-artifact` must run required Python contract tests, create the commit or refuse safely.
5. Adopt the **native history receipt** through `python3 -B -m devtool.native_overlay_delivery adopt-committed`. No reapply after commit.
6. Use `devtool delivery session ...` to perform approved non-force push and draft PR. Exact PR head hosted CI is a separate gate.

**New-repository CI bootstrap exception:** `config/delivery-required-ci.json` must contain a *real* GitHub Actions workflow numeric ID; its source must be committed on the base branch for subsequent strict qualification. The ID is not known before GitHub registers the first workflow, so the initial bootstrap cannot claim Devtool's formal required-CI gate. Do not invent an ID or interpret absent policy as success. Once the first workflow is registered and the baseline merged, introduce the verified policy via a separate native overlay. The bootstrap CI still runs and must be inspected at the exact PR HEAD independently.

24-shard `devtool-full-repository-pytest.yml` is Devtool's **own** regression scope, not this Android module's CI. Android device and release qualification remain separate.

On an uncertain push/PR response, stop and reconcile the durable journal; never blindly retry. On an interrupted committed overlay, use status/report/resume and adoption, never re-apply the artifact.
