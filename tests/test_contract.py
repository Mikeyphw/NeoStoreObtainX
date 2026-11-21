"""Bootstrap-only structural tests, device/Gradle tests are separate gates."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main"
ANDROID = "{http://schemas.android.com/apk/res/android}"

class BootstrapContractTests(unittest.TestCase):
    def test_lsposed_entrypoint_and_scope(self):
        entry = (BASE / "assets/xposed_init").read_text().strip()
        self.assertEqual(entry, "io.github.mikeyphw.neostoreobtainx.NeoStoreEntry")
        source = (BASE / "java/io/github/mikeyphw/neostoreobtainx/NeoStoreEntry.kt").read_text()
        self.assertIn("IXposedHookLoadPackage", source)
        self.assertIn("NeoStorePageProbe.install(loader)", source)
        manifest = ET.parse(BASE / "AndroidManifest.xml")
        rows = {node.attrib.get(ANDROID + "name"):node for node in manifest.iter("meta-data")}
        self.assertEqual(rows["xposedmodule"].attrib[ANDROID + "value"], "true")
        self.assertEqual(rows["xposedscope"].attrib[ANDROID + "value"], "com.machiav3lli.fdroid,com.looker.droidify")
        self.assertEqual(rows["xposedminversion"].attrib[ANDROID + "value"], "82")

    def test_xposed_api_is_not_packaged(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text()
        self.assertIn('compileOnly("de.robv.android.xposed:api:82")', gradle)
        self.assertNotIn('implementation("de.robv.android.xposed:api:', gradle)

    def test_no_non_neo_store_hook_or_download_mutation(self):
        source = (BASE / "java/io/github/mikeyphw/neostoreobtainx/NeoStoreEntry.kt").read_text()
        self.assertIn('TargetPackages.NEO_STORE ->', source)
        self.assertIn('TargetPackages.DROIDIFY ->', source)
        self.assertNotRegex(source, r"DownloadWorker|enqueue\(|startActivity\(")

    def test_obtainx_identity_source_mapping(self):
        src = (BASE / "java/io/github/mikeyphw/neostoreobtainx/TargetPackages.kt").read_text()
        self.assertIn('dev.bikram.obtainx', src)
        mapper = (BASE / "java/io/github/mikeyphw/neostoreobtainx/SourceMapper.kt").read_text()
        self.assertIn('overrideSource = "FDroidRepo"', mapper)
        self.assertIn('https://f-droid.org/packages/', mapper)

    def test_ci_provides_real_gradle_gate(self):
        ci = (ROOT / ".github/workflows/ci.yml").read_text()
        self.assertIn(':app:assembleDebug', ci)
        self.assertIn(':app:lintDebug', ci)
        self.assertIn(':app:testDebugUnitTest', ci)
        self.assertIn('pull_request:', ci)

if __name__ == "__main__":
    unittest.main()
