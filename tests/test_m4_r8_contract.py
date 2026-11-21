"""Fail-closed contracts derived from observed installed Neo Store 1.3.3 DEX."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java/io/github/mikeyphw/neostoreobtainx'


class ReleaseR8ProbeContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.probe = (SRC / 'NeoStorePageProbe.kt').read_text()
        cls.ui = (SRC / 'ObtainXPageAction.kt').read_text()
        cls.policy = (SRC / 'ActionPolicy.kt').read_text()

    def test_only_dex_retained_source_pinned_fields(self):
        self.assertIn('getDeclaredField(name)', self.probe)
        for owner,field in [
            ('activityClass','navStack'),('appRoute','packageName'),
            ('viewModel','packageName'),('viewModel','coreAppState'),
            ('core','suggestedProductRepo'),('embedded','product'),
            ('product','packageName'),('product','repositoryId'),('product','source'),('product','web'),
            ('repository','id'),('repository','address'),
            ('repository','authentication'),('hostPair','first'),('hostPair','second')]:
            self.assertIn(f'releaseField({owner}, "{field}")', self.probe)
        self.assertIn('stateFlow.getMethod("getValue")', self.probe)

    def test_no_removed_getters_or_set_app_hook(self):
        for old in ['publicMethod(', '"setApp"', '"getCoreAppState"',
                    '"getSuggestedProductRepo"', '"getFirst"', '"getSecond"',
                    'symbols.repositoryAddress.invoke', 'symbols.routePackage.invoke']:
            self.assertNotIn(old, self.probe)
        self.assertIn('XposedBridge.hookAllConstructors(viewModel,', self.probe)
        self.assertIn('getDeclaredMethod("onResume")', self.probe)
        self.assertNotIn('hookAllMethods(', self.probe)

    def test_live_route_matched_vm_and_weak_bounded_retention(self):
        for s in [
            'private val viewModels = mutableListOf<WeakReference<Any>>()',
            'while (viewModels.size > 24) viewModels.removeAt(0)',
            'val packageFlow = symbols.vmPackageFlow.get(vm)',
            '(symbols.stateValue.invoke(packageFlow) as? String) == active',
            'return ProbePolicy.resolve(active, null, null, null, null, null)',
            'return ProbePolicy.resolve(active, active, productPkg, productId, repoId, repoUrl, productSource)',
            'val fresh = currentBinding(activity)',
            'ActionPolicy.allowsTap(displayed, fresh, action.visible(activity))',
        ]: self.assertIn(s,self.probe)

    def test_authentication_remains_fail_closed(self):
        self.assertIn('(symbols.repositoryAuthentication.get(repo) as? String) != ""', self.probe)
        self.assertNotIn('getAuthenticationPair()', self.probe)
        self.assertIn('displayed.route === fresh.route', self.policy)
        self.assertIn('displayed.snapshot == fresh.snapshot', self.policy)

    def test_no_automatic_side_effect_or_private_data_logs(self):
        combined=self.probe+self.ui
        self.assertEqual(combined.count('ObtainXIntentBridge.launchFromUserTap('),1)
        self.assertIn('NSOX_M4_HOOKS=INSTALLED_R8_FIELDS',self.probe)
        self.assertNotIn('startActivity(',combined)
        for word in ['ContentResolver','SQLiteDatabase','DownloadWorker','enqueue(', 'PackageInstaller']:
            self.assertNotIn(word,combined)
        self.assertNotIn('XposedBridge.log(',self.ui)
        self.assertNotIn('XposedBridge.log("NSOX_M4_HOOKS=" +',self.probe)

    def test_version_upgrade_and_signing_not_reworked(self):
        gradle=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('versionCode = 9',gradle)
        self.assertIn('versionName = "0.8.0-m8-droidify"',gradle)
        doc=(ROOT/'docs/M4_R8_ACCEPTANCE.md').read_text()
        self.assertIn('DEVICE_ACCEPTANCE=NOT_PERFORMED',doc)
        self.assertIn('RELEASE=NOT_PERFORMED',doc)

if __name__=='__main__': unittest.main()
