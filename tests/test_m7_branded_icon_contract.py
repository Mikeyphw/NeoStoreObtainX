"""M7 module-only visual/contract regression: no ObtainX status integration."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
J=ROOT/'app/src/main/java/io/github/mikeyphw/neostoreobtainx'
class M7BrandedIconContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui=(J/'ObtainXPageAction.kt').read_text()
        cls.probe=(J/'NeoStorePageProbe.kt').read_text()

    def test_app_brand_and_drawable_fallback(self):
        for token in ('getApplicationIcon(TargetPackages.OBTAIN_X)',
                      'ObtainXBrandIcon(', 'drawLine(cx, cy - 8f * unit',
                      'InsetDrawable(', 'dp(activity, 4)', 'dp(activity, 48)'):
            self.assertIn(token,self.ui)

    def test_no_status_provider_or_tracking_in_module(self):
        self.assertFalse((J/'ObtainXTrackedStatus.kt').exists())
        for s in ('contentResolver.query(', 'ObtainXTrackedState', 'trackedStatus',
                  'NSOX_M7_TRACKED_STATE=', 'TrackedBrandIcon', 'trackedState'):
            self.assertNotIn(s,self.ui+self.probe)
        self.assertNotIn('provider', (ROOT/'app/src/main/AndroidManifest.xml').read_text().lower())

    def test_neutral_text_and_explicit_user_action(self):
        for token in ('"Open in ObtainX. Long press for settings."',
                      '"Abrir no ObtainX. Toque e segure para configurações."',
                      'onTap(currentPage)', 'setOnLongClickListener',
                      'onAnchor(anchorKind)'):
            self.assertIn(token,self.ui)
        self.assertNotIn('Tracked in ObtainX',self.ui)
        self.assertNotIn('if (tracked',self.ui)
        self.assertIn('ActionPolicy.allowsTap(displayed, fresh, action.visible(activity))',self.probe)

    def test_old_bridge_and_izzy_selection_untouched(self):
        source=(J/'SourceMapper.kt').read_text()
        self.assertIn('overrideSource = "IzzyOnDroid"',source)
        bridge=(J/'ObtainXIntentBridge.kt').read_text()
        self.assertIn('launchFromUserTap',bridge)
        self.assertIn('setPackage(TargetPackages.OBTAIN_X)',bridge)

    def test_version_docs_and_later_fork_roadmap(self):
        gradle=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('versionCode = 10',gradle)
        self.assertIn('versionName = "0.8.1-m9-droidify-scope"',gradle)
        doc=(ROOT/'docs/M7_BRANDED_ICON_ACCEPTANCE.md').read_text()
        roadmap=(ROOT/'docs/FUTURE_OBTAINX_FORK_STATUS.md').read_text()
        self.assertIn('DEVICE_ACCEPTANCE=NOT_PERFORMED',doc)
        self.assertIn('DOCUMENTATION ONLY',roadmap)
        self.assertIn('Mikeyphw/ObtainX',roadmap)
        self.assertIn('do not install it over',roadmap)

if __name__=='__main__': unittest.main()
