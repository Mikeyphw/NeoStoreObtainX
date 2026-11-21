"""Static M1 privacy, lifecycle and native-hook boundary contract tests."""
import re
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/io/github/mikeyphw/neostoreobtainx'


class M1ProbeContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.entry = (JAVA / 'NeoStoreEntry.kt').read_text()
        cls.probe = (JAVA / 'NeoStorePageProbe.kt').read_text()
        cls.policy = (JAVA / 'ProbePolicy.kt').read_text()

    def test_hook_scoped_to_single_application_process(self):
        self.assertIn('lpparam.packageName != TargetPackages.NEO_STORE', self.entry)
        self.assertIn('lpparam.processName != TargetPackages.NEO_STORE', self.entry)

    def test_no_composable_or_compiler_generated_hook(self):
        self.assertIn('getDeclaredMethod("onResume")', self.probe)
        self.assertIn('publicMethod(viewModel, "setApp", String::class.java)', self.probe)
        self.assertNotIn('AppPageKt', self.probe)
        self.assertNotIn('Composer', self.probe)

    def test_installed_host_version_must_match_source_pin(self):
        self.assertIn('TARGET_VERSION = "1.3.3"', self.probe)
        self.assertIn('TARGET_VERSION_CODE = 1304L', self.probe)
        self.assertIn('UNPINNED_NEO_STORE_VERSION', self.probe)

    def test_polling_stops_when_activity_pauses_or_is_destroyed(self):
        self.assertIn('override fun onActivityPaused', self.probe)
        self.assertIn('override fun onActivityDestroyed', self.probe)
        self.assertIn('pending?.let(main::removeCallbacks)', self.probe)
        self.assertIn('WeakReference<Activity>', self.probe)
        self.assertIn('WeakReference<Any>', self.probe)

    def test_m1_never_changes_app_content_or_launches_obtainx(self):
        all_new = self.entry + self.probe + self.policy
        for forbidden in ['DownloadWorker', 'startActivity(', 'Intent(', 'addView(', 'WindowManager',
                          'enqueue(', 'hookAllMethods(', 'XC_MethodReplacement', 'PackageInstaller']:
            self.assertNotIn(forbidden, all_new)

    def test_repository_address_not_written_to_logs(self):
        self.assertIn('NSOX_M1_PAGE_STATE=', self.probe)
        self.assertNotRegex(self.probe, r'XposedBridge\.log\([^\n]*repoUrl')
        self.assertNotRegex(self.probe, r'XposedBridge\.log\([^\n]*packageName')

    def test_identity_validation_precedes_mapping(self):
        self.assertIn('activePackage != viewModelPackage', self.policy)
        self.assertIn('activePackage != productPackage', self.policy)
        self.assertIn('productRepositoryId != repositoryId', self.policy)
        self.assertLess(self.policy.index('productRepositoryId != repositoryId'), self.policy.index('SourceMapper.map('))

    def test_no_extra_android_permissions_or_runtime_dependencies(self):
        manifest = (ROOT / 'app/src/main/AndroidManifest.xml').read_text()
        self.assertNotIn('<uses-permission', manifest)
        gradle = (ROOT / 'app/build.gradle.kts').read_text()
        self.assertIn('compileOnly("de.robv.android.xposed:api:82")', gradle)
        self.assertNotIn('implementation("de.robv.android.xposed:', gradle)


if __name__ == '__main__':
    unittest.main()
