"""M3 source contracts; real View/layout/LSPosed acceptance is device-only."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
J = ROOT/'app/src/main/java/io/github/mikeyphw/neostoreobtainx'

class M3UiContract(unittest.TestCase):
    def setUp(self):
        self.probe = (J/'NeoStorePageProbe.kt').read_text()
        self.ui = (J/'ObtainXPageAction.kt').read_text()
        self.policy = (J/'ActionPolicy.kt').read_text()
        self.bridge = (J/'ObtainXIntentBridge.kt').read_text()
    def test_only_click_dispatches(self):
        self.assertIn('action.render(activity, page) { displayed -> launchForTap(token, displayed) }', self.probe)
        self.assertIn('val fresh = currentBinding(activity)', self.probe)
        self.assertIn('ActionPolicy.allowsTap(displayed, fresh, action.visible(activity))', self.probe)
        self.assertIn('onTap(currentPage)', self.ui)
        self.assertEqual(self.probe.count('ObtainXIntentBridge.launchFromUserTap('), 1)
        self.assertNotIn('startActivity(', self.ui + self.probe)
        self.assertEqual(self.bridge.count('activity.startActivity(intent)'), 1)
    def test_stale_identity(self):
        self.assertIn('displayed.route === fresh.route', self.policy)
        self.assertIn('displayed.snapshot == fresh.snapshot', self.policy)
        self.assertIn('token != generation', self.probe)
        self.assertIn('button?.get() !== view', self.ui)
        self.assertIn('now - lastTap < 1000L', self.ui)
    def test_own_view_only_and_cleanup(self):
        self.assertEqual(self.ui.count('root.addView(view,'), 1)
        self.assertIn('(own.parent as? ViewGroup)?.removeView(own)', self.ui)
        self.assertIn('existing.parent === root', self.ui)
        self.assertIn('action.clear()', self.probe)
        self.assertIn('settingsDialog?.dismiss()', self.ui)
        self.assertNotIn('removeAllViews', self.ui)
        self.assertNotIn('setContentView(', self.ui)
    def test_read_only_host_and_authenticated_repo_refusal(self):
        self.assertIn('publicMethod(repository, "getAuthentication")', self.probe)
        self.assertIn('(symbols.repositoryAuthentication.invoke(repo) as? String) != ""', self.probe)
        self.assertIn('val hostPair = host("kotlin.Pair")', self.probe)
        self.assertNotIn('as? Pair<', self.probe)
        for t in ['DownloadWorker','PackageInstaller','enqueue(', 'SQLiteDatabase','ContentResolver','ACTION_SEND','setResult(']:
            self.assertNotIn(t, self.ui + self.probe)
    def test_accessibility_insets_settings_and_privacy(self):
        self.assertIn('minimumHeight = dp(activity, 48)', self.ui)
        self.assertIn('IMPORTANT_FOR_ACCESSIBILITY_YES', self.ui)
        self.assertIn('WindowInsets.Type.ime()', self.ui)
        self.assertIn('WindowInsets.Type.displayCutout()', self.ui)
        self.assertIn('setOnLongClickListener', self.ui)
        self.assertIn('"io.github.mikeyphw.neostoreobtainx.ui", Context.MODE_PRIVATE', self.ui)
        self.assertIn('putBoolean("show_action", which == 0)', self.ui)
        self.assertNotIn('XposedBridge.log(', self.ui)
        self.assertIn('XposedBridge.log("NSOX_M3_TAP=" + diagnostic)', self.probe)
    def test_release_gate_not_claimed(self):
        doc = (ROOT/'docs/M3_UI_ACCEPTANCE.md').read_text()
        self.assertIn('DEVICE_ACCEPTANCE=NOT_PERFORMED', doc)
        self.assertIn('RELEASE=NOT_PERFORMED', doc)
        self.assertIn('overlap', doc)

if __name__ == '__main__': unittest.main()
