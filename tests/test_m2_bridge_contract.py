"""M2 source-bound Android bridge and privacy checks; Android execution is CI/device-only."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/io/github/mikeyphw/neostoreobtainx'
TESTS = ROOT / 'app/src/test/java/io/github/mikeyphw/neostoreobtainx'

class M2BridgeContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.entry = (JAVA/'NeoStoreEntry.kt').read_text()
        cls.probe = (JAVA/'NeoStorePageProbe.kt').read_text()
        cls.policy = (JAVA/'ObtainXSeedPolicy.kt').read_text()
        cls.bridge = (JAVA/'ObtainXIntentBridge.kt').read_text()
        cls.tests = (TESTS/'ObtainXSeedPolicyTest.kt').read_text()

    def test_entrypoint_does_not_launch_and_probe_only_uses_tap_bridge(self):
        self.assertNotIn('ObtainXIntentBridge', self.entry)
        self.assertEqual(self.probe.count('ObtainXIntentBridge.launchFromUserTap('), 1)
        self.assertIn('if (!ActionPolicy.allowsTap(displayed, fresh, action.visible(activity))', self.probe)
        self.assertNotIn('startActivity(', self.entry + self.probe)
        self.assertNotIn('DownloadWorker', self.entry + self.probe)

    def test_source_revalidated_not_trusted_from_cached_snapshot(self):
        self.assertIn('currentRoutePackage', self.policy)
        self.assertIn('pkg != currentRoutePackage', self.policy)
        self.assertIn('SourceMapper.map(pkg, address)', self.policy)
        self.assertIn('canonical.url != snapshot.sourceUrl', self.policy)
        self.assertIn('canonical.overrideSource != snapshot.overrideSource', self.policy)

    def test_obtainx_app_json_shape_is_not_raw_url_or_auto_add(self):
        self.assertIn('"{\\"appIdOrName\\":${quote(pkg)}}"', self.policy)
        self.assertIn('append(",\\"additionalSettings\\":")', self.policy)
        self.assertIn('"FDroidRepo"', self.policy)
        self.assertIn('preferredApkIndex', self.policy)
        self.assertIn('ObtainXSeed.Prepared', self.policy)
        self.assertIn('Uri.encode(seed.json)', self.bridge)
        self.assertIn('obtainium://app/', self.bridge)
        self.assertNotIn('obtainium://add/', self.bridge)

    def test_bridge_is_package_locked_and_explicit_user_action_only(self):
        self.assertIn('setPackage(TargetPackages.OBTAIN_X)', self.bridge)
        self.assertIn('Intent.ACTION_VIEW', self.bridge)
        self.assertIn('CATEGORY_BROWSABLE', self.bridge)
        self.assertIn('resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)', self.bridge)
        self.assertIn('target.packageName != TargetPackages.OBTAIN_X', self.bridge)
        self.assertIn('!target.exported', self.bridge)
        self.assertIn('fun launchFromUserTap(', self.bridge)
        self.assertEqual(self.bridge.count('activity.startActivity(intent)'), 1)
        self.assertNotIn('createChooser', self.bridge)

    def test_no_unsafe_automatic_side_effects_or_payload_logs(self):
        combined = self.policy + self.bridge
        for token in ['DownloadWorker', 'enqueue(', 'PackageInstaller', 'rootShell',
                      'XposedBridge.log(', 'Intent.ACTION_SEND', 'ContentResolver', 'SQLiteDatabase']:
            self.assertNotIn(token, combined)
        self.assertIn('ActivityNotFoundException', self.bridge)
        self.assertIn('SecurityException', self.bridge)
        self.assertIn('SeedRefusal.SOURCE_TOO_LONG', self.policy)

    def test_jvm_tests_cover_official_custom_izzy_stale_and_escape(self):
        for text in ['officialFdroidBuildsMinimalImportSeed', 'customRepoHasNestedJsonStringAndForcedAdapter',
                     'izzyDoesNotUseGenericRepoAdapter', 'routeChangeRefusesStaleSnapshot',
                     'forgedSourceOrChangedRepositoryIsRejected', 'jsonQuoteEscapesSpecialAndControlCharacters']:
            self.assertIn(text, self.tests)

    def test_version_bump_and_milestone_remains_pending(self):
        gradle = (ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('versionCode = 5', gradle)
        self.assertIn('0.5.0-m4-r8-fields', gradle)
        self.assertIn('this candidate', (ROOT/'docs/IMPLEMENTATION_PLAN.md').read_text())
        self.assertIn('**does not yet call the bridge**', (ROOT/'docs/M2_INTENT_BRIDGE_ACCEPTANCE.md').read_text())

if __name__=='__main__':
    unittest.main()
