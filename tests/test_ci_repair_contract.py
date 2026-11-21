"""Regression checks for M0 CI repair. Does not claim an Android build."""
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / '.github/workflows/ci.yml'
POLICY = ROOT / 'config/delivery-required-ci.json'


class CiRepairContractTests(unittest.TestCase):
    def test_setup_android_does_not_request_retired_tools(self):
        text = WORKFLOW.read_text(encoding='utf-8')
        self.assertIn('android-actions/setup-android@v4', text)
        self.assertNotIn('android-actions/setup-android@v3', text)
        # A deprecated package must not appear in the installer declaration.
        match = re.search(r'(?m)^\s+packages:\s*(.+)$', text)
        self.assertIsNotNone(match)
        self.assertEqual(match.group(1).strip().strip('\"\''), 'platform-tools')

    def test_android_tasks_are_not_skipped_or_replaced(self):
        text = WORKFLOW.read_text(encoding='utf-8')
        self.assertIn("sdkmanager --install 'platforms;android-35' 'build-tools;35.0.0'", text)
        self.assertIn('gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug', text)
        self.assertIn('CI_DEBUG_APK_ASSEMBLED_NO_PUBLICATION', text)
        self.assertIn(':app:assembleRelease', text)
        self.assertNotIn('actions/upload-artifact', text)

    def test_python_structural_checks_still_run_on_pr(self):
        text = WORKFLOW.read_text(encoding='utf-8')
        self.assertIn('pull_request:', text)
        self.assertIn('push:', text)
        self.assertIn("python-version: '3.13'", text)
        self.assertIn('python3 -B -m unittest discover -s tests', text)

    def test_policy_is_exact_known_github_workflow(self):
        policy = json.loads(POLICY.read_text(encoding='utf-8'))
        self.assertEqual(set(policy), {
            'schema_version', 'repository', 'base_branch', 'event',
            'required_workflows', 'release_qualification',
        })
        self.assertEqual(policy['schema_version'], 1)
        self.assertEqual(policy['repository'], 'Mikeyphw/NeoStoreObtainX')
        self.assertEqual(policy['base_branch'], 'main')
        self.assertEqual(policy['event'], 'pull_request')
        self.assertEqual(policy['release_qualification'], 'separate')
        self.assertEqual(policy['required_workflows'], [{
            'name': 'NeoStoreObtainX CI', 'workflow_id': 425663365,
        }])
        self.assertIn('name: NeoStoreObtainX CI', WORKFLOW.read_text(encoding='utf-8'))

    def test_policy_does_not_claim_first_pr_is_ci_qualified(self):
        doc = (ROOT / 'docs/CI_REPAIR_AND_QUALIFICATION.md').read_text(encoding='utf-8')
        self.assertIn('NOT QUALIFIED', doc)
        self.assertIn('base branch', doc)
        self.assertIn('device acceptance', doc)


if __name__ == '__main__':
    unittest.main()
