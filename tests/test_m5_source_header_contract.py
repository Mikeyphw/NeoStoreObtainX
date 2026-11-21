"""Offline structural regression for M5 source preference and anchored UI."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
J=ROOT/'app/src/main/java/io/github/mikeyphw/neostoreobtainx'
class M5Contract(unittest.TestCase):
    def test_source_preference_and_identity(self):
        mapper=(J/'SourceMapper.kt').read_text()
        probe=(J/'NeoStorePageProbe.kt').read_text()
        snap=(J/'ProbePolicy.kt').read_text()
        seed=(J/'ObtainXSeedPolicy.kt').read_text()
        for s in ('preferred(', 'supportedSourceSite(', '"github.com"', '"gitlab.com"',
                  '"codeberg.org"', '"sourceforge.net"', '"git.sr.ht"'):
            self.assertIn(s,mapper)
        self.assertIn('SourceMapper.preferred(activePackage, repositoryAddress, productSourceSite)',snap)
        self.assertIn('releaseField(product, "source")',probe)
        self.assertIn('releaseField(product, "web")',probe)
        self.assertIn('productSourceSite',snap)
        self.assertIn('snapshot.productSourceSite',seed)
        self.assertIn('canonical.url != snapshot.sourceUrl',seed)
    def test_izzy_uses_dedicated_adapter(self):
        mapper=(J/'SourceMapper.kt').read_text()
        seed=(J/'ObtainXSeedPolicy.kt').read_text()
        self.assertIn('overrideSource = "IzzyOnDroid"',mapper)
        self.assertIn('canonical.overrideSource == "IzzyOnDroid"',seed)
        self.assertIn('appIdOrName',seed)
    def test_icon_is_header_anchored_and_fail_closed(self):
        ui=(J/'ObtainXPageAction.kt').read_text()
        self.assertIn('getIdentifier("source_code"',ui)
        self.assertIn('AccessibilityNodeInfo',ui)
        self.assertIn('source.left - gap - size',ui)
        self.assertIn('ImageButton(activity)',ui)
        self.assertIn('minimumHeight = dp(activity, 48)',ui)
        self.assertIn('importantForAccessibility',ui)
        self.assertIn('if (!position(activity,root,view,source)) {',ui)
        self.assertNotIn('Gravity.BOTTOM',ui)
        self.assertNotIn('android.widget.Button',ui)
        self.assertNotIn('removeAllViews',ui)
    def test_no_side_effect_or_secret_logs(self):
        ui=(J/'ObtainXPageAction.kt').read_text()
        probe=(J/'NeoStorePageProbe.kt').read_text()
        self.assertNotIn('startActivity(',ui+probe)
        self.assertEqual(probe.count('ObtainXIntentBridge.launchFromUserTap('),1)
        self.assertIn('ActionPolicy.allowsTap(displayed, fresh, action.visible(activity))',probe)
        self.assertNotIn('XposedBridge.log(',ui)
    def test_version_and_pending_acceptance(self):
        gradle=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('versionCode = 10',gradle)
        self.assertIn('0.8.1-m9-droidify-scope',gradle)
        doc=(ROOT/'docs/M5_SOURCE_HEADER_ACCEPTANCE.md').read_text()
        self.assertIn('DEVICE_ACCEPTANCE=NOT_PERFORMED',doc)
        self.assertIn('RELEASE=NOT_PERFORMED',doc)
