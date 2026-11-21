"""M6 structural regression: source-pinned bounded header recovery."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/io/github/mikeyphw/neostoreobtainx'
class M6HeaderRecovery(unittest.TestCase):
    def test_accessibility_is_optional_and_safe_fallback_exists(self):
        ui=(JAVA/'ObtainXPageAction.kt').read_text()
        for marker in ['val semanticSource = try { anchor(activity, root) } catch (_: RuntimeException) { null }',
                       'val source = semanticSource ?: pinnedHeaderSource(activity, root)',
                       'root.width < dp(activity, 365)',
                       'WindowInsets.Type.statusBars()',
                       'root.getLocationOnScreen(rootScreen)',
                       'root.layoutDirection == View.LAYOUT_DIRECTION_RTL',
                       'PINNED_HEADER_FALLBACK',
                       'NO_SAFE_HEADER_POSITION',
                       'if (!position(activity,root,view,source))',
                       'minimumHeight = dp(activity, 48)',
                       'setOnLongClickListener']:
            self.assertIn(marker,ui)
        self.assertNotIn('Gravity.BOTTOM',ui)
        self.assertNotIn('removeAllViews',ui)

    def test_only_static_anchor_marker_and_no_user_data_in_logs(self):
        probe=(JAVA/'NeoStorePageProbe.kt').read_text()
        ui=(JAVA/'ObtainXPageAction.kt').read_text()
        self.assertIn('NSOX_M6_UI_ANCHOR=',probe)
        self.assertIn('if (lastUiAnchor != mode)',probe)
        self.assertIn('lastUiAnchor = null',probe)
        self.assertNotIn('XposedBridge.log(',ui)
        self.assertNotIn('XposedBridge.log("NSOX_M6_UI_ANCHOR=" + page',probe)

    def test_protected_source_preference_unchanged(self):
        s=(JAVA/'SourceMapper.kt').read_text()
        self.assertIn('overrideSource = "IzzyOnDroid"',s)
        self.assertIn('fun preferred(',s)
        bridge=(JAVA/'ObtainXIntentBridge.kt').read_text()
        self.assertIn('setPackage(TargetPackages.OBTAIN_X)',bridge)

    def test_release_pinned_and_device_pending(self):
        gradle=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('versionCode = 10',gradle)
        self.assertIn('0.8.1-m9-droidify-scope',gradle)
        d=(ROOT/'docs/M6_HEADER_VISIBILITY_ACCEPTANCE.md').read_text()
        self.assertIn('DEVICE_ACCEPTANCE=NOT_PERFORMED',d)
        self.assertIn('RELEASE=NOT_PERFORMED',d)
if __name__=='__main__': unittest.main()
