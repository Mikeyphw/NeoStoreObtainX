"""M8 native toolbar and safe source-binding static gate; hosted/device testing separate."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
J=ROOT/'app/src/main/java/io/github/mikeyphw/neostoreobtainx'
class DroidifyM8Contracts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.hook=(J/'DroidifyPageHook.kt').read_text()
        cls.entry=(J/'NeoStoreEntry.kt').read_text()
        cls.manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
    def test_source_pin_and_independent_scopes(self):
        self.assertIn('const val DROIDIFY = "com.looker.droidify"',(J/'TargetPackages.kt').read_text())
        self.assertIn('TargetPackages.NEO_STORE ->', self.entry)
        self.assertIn('TargetPackages.DROIDIFY ->', self.entry)
        self.assertIn('DroidifyPageHook.install(loader)',self.entry)
        self.assertIn('lpparam.processName != lpparam.packageName',self.entry)
        self.assertIn('@array/xposed_scope',self.manifest)
    def test_actual_native_toolbar_and_no_window_overlay(self):
        for token in ('onViewCreated','com.google.android.material.appbar.MaterialToolbar',
                      'getMenu','menu.add(0, ITEM_ID, 0, "Add to ObtainX")',
                      'SHOW_AS_ACTION_IF_ROOM','getApplicationIcon(TargetPackages.OBTAIN_X)',
                      'setOnMenuItemClickListener'):
            self.assertIn(token,self.hook)
        for token in ('addView(', 'WindowManager', 'AccessibilityNodeInfo', 'getWindow()',
                      'ContentResolver', 'SQLiteDatabase', 'PackageInstaller', 'startActivity('):
            self.assertNotIn(token,self.hook)
    def test_host_version_strict_and_current_route(self):
        for token in ('TARGET_VERSION = "0.7.8"','TARGET_CODE = 780L','UNSUPPORTED_HOST_VERSION',
                      'APP_ROUTE_KEY = "package_name"', 'routePackage(fragment) != boundPackage',
                      'isResumed', 'hasWindowFocus', 'view.isAttachedToWindow', 'view.isShown'):
            self.assertIn(token,self.hook)
    def test_read_live_verified_pairs_no_credentials(self):
        for token in ('getObjectField(fragment, "products")','getFirst','getSecond',
                      'getObjectField(product, "repositoryId")', 'getObjectField(repo, "id")',
                      'getObjectField(repo, "authentication")','!auth.isNullOrBlank()',
                      'REPO_ROUTE_KEY', 'selectedAddress != address', 'ProbePolicy.resolve(',
                      'ObtainXIntentBridge.launchFromUserTap(', 'snapshot.phase != ProbePhase.READY'):
            self.assertIn(token,self.hook)
        self.assertEqual(self.hook.count('ObtainXIntentBridge.launchFromUserTap('),1)
        self.assertNotIn('trackedStatus',self.hook)
        self.assertNotIn('ObtainXTrackedStatus',self.hook)
    def test_version_and_acceptance_docs(self):
        gradle=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('versionCode = 10',gradle)
        self.assertIn('versionName = "0.8.1-m9-droidify-scope"',gradle)
        doc=(ROOT/'docs/M8_DROIDIFY_ACCEPTANCE.md').read_text()
        self.assertIn('DEVICE_ACCEPTANCE=NOT_PERFORMED',doc)
        self.assertIn('RELEASE=NOT_PERFORMED',doc)
        self.assertIn('0.7.8',doc)
        self.assertIn('No fork tracking status',doc)
if __name__=='__main__': unittest.main()
