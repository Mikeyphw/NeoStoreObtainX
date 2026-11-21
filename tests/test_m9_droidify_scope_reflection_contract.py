from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
SRC=(ROOT/'app/src/main/java/io/github/mikeyphw/neostoreobtainx/DroidifyPageHook.kt').read_text()
ANDROID='{http://schemas.android.com/apk/res/android}'

class M9DroidifyFixContracts(unittest.TestCase):
 def test_recommended_legacy_lsposed_scope_array(self):
  manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml')
  scope=[x for x in manifest.iter('meta-data') if x.attrib.get(ANDROID+'name')=='xposedscope']
  self.assertEqual(len(scope),1)
  self.assertEqual(scope[0].attrib.get(ANDROID+'resource'),'@array/xposed_scope')
  self.assertNotIn(ANDROID+'value',scope[0].attrib)
  declared=ET.parse(ROOT/'app/src/main/res/values/xposed_scope.xml')
  self.assertEqual([e.text for e in declared.findall("./string-array[@name='xposed_scope']/item")],
                   ['com.machiav3lli.fdroid','com.looker.droidify'])
 def test_no_fragile_xposed_attach_getters(self):
  self.assertIn('private fun hostActivity(root: View): Activity?',SRC)
  self.assertIn('private fun inheritedGetter(instance: Any, name: String): Any?',SRC)
  self.assertIn('private fun fragmentArguments(fragment: Any): Bundle?',SRC)
  self.assertIn('ATTACH_REFUSED_${stage}_${error.javaClass.simpleName}',SRC)
  for pattern in ('XposedHelpers.callMethod(fragment, "getActivity")',
                  'XposedHelpers.callMethod(tb, "getMenu")',
                  'XposedHelpers.callMethod(fragment, "getArguments")'):
   self.assertNotIn(pattern,SRC)
 def test_existing_source_policies_unchanged(self):
  self.assertIn('ObtainXIntentBridge.launchFromUserTap(',SRC)
  self.assertIn('ProbePolicy.resolve(',SRC)
  self.assertIn('TARGET_CODE = 780L',SRC)
  self.assertIn('TARGET_VERSION = "0.7.8"',SRC)
  self.assertIn('routePackage(fragment) != boundPackage',SRC)
 def test_version(self):
  gradle=(ROOT/'app/build.gradle.kts').read_text()
  self.assertIn('versionCode = 10',gradle)
  self.assertIn('versionName = "0.8.1-m9-droidify-scope"',gradle)
