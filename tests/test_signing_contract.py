"""Signing policy regression; actual signed APKs require GitHub/SDK and real key."""
from pathlib import Path
import unittest
import subprocess
import tempfile
import os
import json
import hashlib

ROOT=Path(__file__).resolve().parents[1]
WORKFLOW=ROOT/'.github/workflows/signed-apks.yml'
CI=ROOT/'.github/workflows/ci.yml'
SIGN=ROOT/'scripts/sign-apks.sh'
DOWNLOAD=ROOT/'scripts/download-apk.sh'
PIN='a'*64
SHA='b'*40

class SigningContract(unittest.TestCase):
    def test_only_trusted_main_workflow_can_publish(self):
        text=WORKFLOW.read_text(); ci=CI.read_text()
        self.assertIn('environment: apk-signing',text)
        self.assertIn("github.ref == 'refs/heads/main'",text)
        self.assertIn("github.repository == 'Mikeyphw/NeoStoreObtainX'",text)
        self.assertNotIn('pull_request',text)
        self.assertNotIn('secrets.',ci)
        self.assertNotIn('upload-artifact',ci)
        self.assertIn(':app:assembleRelease',ci)
        self.assertIn(':app:assembleDebug :app:testDebugUnitTest :app:lintDebug',ci)
    def test_both_variants_use_same_pin_and_secret_scope(self):
        t=WORKFLOW.read_text(); s=SIGN.read_text()
        self.assertIn('for variant in debug release',s)
        self.assertIn('--ks-key-alias neostoreobtainx',s)
        self.assertIn('--ks-pass env:NSOX_STORE_PASSWORD',s)
        self.assertIn('--key-pass env:NSOX_STORE_PASSWORD',s)
        self.assertIn('verify --verbose --print-certs --Werr',s)
        self.assertIn('MISSING_SIGNING_SECRETS',s)
        self.assertIn('nsox-signed-debug-${{ github.run_attempt }}',t)
        self.assertIn('nsox-signed-release-${{ github.run_attempt }}',t)
        self.assertEqual(t.count('secrets.NSOX_'),2)
        self.assertGreater(t.index('Publish verified debug APK'),t.index('run: bash scripts/sign-apks.sh'))
    def test_scripts_bash_syntax(self):
        for p in (SIGN,DOWNLOAD): subprocess.run(['bash','-n',str(p)],check=True)
    def test_signing_refuses_wrong_source(self):
        r=subprocess.run(['bash',str(SIGN)],env={**os.environ,'GITHUB_REPOSITORY':'someone/fork'},capture_output=True,text=True)
        self.assertNotEqual(r.returncode,0); self.assertIn('WRONG_SOURCE',r.stderr)
    def test_signing_refuses_missing_secrets(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo=Path(tmp); env={**os.environ,'GITHUB_REPOSITORY':'Mikeyphw/NeoStoreObtainX','GITHUB_REF':'refs/heads/main','GITHUB_EVENT_NAME':'push','GITHUB_RUN_ID':'1','GITHUB_RUN_ATTEMPT':'1','NSOX_STORE_PASSWORD':'','NSOX_KEYSTORE_B64':''}
            subprocess.run(['git','init',str(repo)],check=True,capture_output=True)
            subprocess.run(['git','-C',str(repo),'-c','user.name=Test','-c','user.email=t@example.invalid','commit','--allow-empty','-m','test'],check=True,capture_output=True)
            env['GITHUB_SHA']=subprocess.check_output(['git','-C',str(repo),'rev-parse','HEAD'],text=True).strip()
            r=subprocess.run(['bash',str(SIGN)],cwd=repo,env=env,capture_output=True,text=True)
            self.assertNotEqual(r.returncode,0); self.assertIn('MISSING_SIGNING_SECRETS',r.stderr); self.assertFalse((repo/'signed').exists())

class DownloadFixture(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(); self.addCleanup(self.tmp.cleanup); self.root=Path(self.tmp.name)
        self.home=self.root/'home'; self.dest=self.root/'dest'; self.bin=self.root/'bin'; self.tmpdir=self.root/'tmp'
        for p in [self.home,self.dest,self.bin,self.tmpdir]: p.mkdir()
        key=self.home/'.local/share/neostore-obtainx/signing'; key.mkdir(parents=True); (key/'certificate.sha256').write_text(PIN+'\n')
        self.env={**os.environ,'HOME':str(self.home),'TMPDIR':str(self.tmpdir),'PATH':str(self.bin)+os.pathsep+os.environ['PATH'],'MOCK_SIGNER':PIN,'MOCK_VARIANT':'debug','MOCK_SHA':SHA,'MOCK_PIN':PIN,'MOCK_MODE':'ok'}
        gh=self.bin/'gh'; gh.write_text('''#!/usr/bin/env python3
import json,os,sys,hashlib
from pathlib import Path
args=sys.argv[1:]; sha=os.environ['MOCK_SHA']; pin=os.environ['MOCK_PIN']; variant=os.environ['MOCK_VARIANT']; mode=os.environ['MOCK_MODE']; repo={'full_name':'Mikeyphw/NeoStoreObtainX'}
if args[0]=='api':
    endpoint=next(x for x in args if x.startswith('repos/'))
    if endpoint.endswith('signed-apks.yml'): data={'path':'.github/workflows/signed-apks.yml','state':'active','id':123}
    elif endpoint.endswith('signed-apks.yml/runs'): data={'workflow_runs':[{'id':99,'run_attempt':2,'head_sha':sha,'head_branch':'main','workflow_id':123,'repository':repo,'head_repository':repo,'event':'push','status':'completed','conclusion':'success','created_at':'2026-11-21T00:00:00Z'}]}
    elif '/runs/99/artifacts' in endpoint: data={'artifacts':[] if mode=='missing' else [{'id':88,'name':'nsox-signed-'+variant+'-2','expired':False,'workflow_run':{'id':99,'head_sha':sha,'head_branch':'main'}}]}
    else: sys.exit(9)
    print(json.dumps(data))
elif args[:2]==['run','download']:
    p=Path(args[args.index('--dir')+1]); name='NeoStoreObtainX-'+variant+'.apk'; raw=b'mock-apk'; (p/name).write_bytes(raw)
    (p/'build.json').write_text(json.dumps(dict(schema=1,repository=repo['full_name'],head_sha=sha,run_id=99,run_attempt=2,variant=variant,apk=name,sha256='0'*64 if mode=='tampered' else hashlib.sha256(raw).hexdigest(),signer_sha256=pin)))
else: sys.exit(9)
'''); gh.chmod(0o700)
        apk=self.bin/'apksigner'; apk.write_text('''#!/usr/bin/env bash
set -Eeuo pipefail
printf 'Verified using v2 scheme (APK Signature Scheme v2): true\\nSigner #1 certificate SHA-256 digest: %s\\n' "$MOCK_SIGNER"
'''); apk.chmod(0o700)
    def call(self,variant='debug'):
        self.env['MOCK_VARIANT']=variant
        return subprocess.run(['bash',str(DOWNLOAD),variant,str(self.dest)],env=self.env,capture_output=True,text=True)
    def test_both_variants_download_only_after_verification(self):
        for v in ['debug','release']:
            r=self.call(v); self.assertEqual(r.returncode,0,r.stdout+r.stderr); self.assertIn('VERIFIED_NOT_INSTALLED',r.stdout)
        self.assertEqual(len(list(self.dest.glob('*.apk'))),2)
    def test_missing_artifact_no_fallback(self):
        self.env['MOCK_MODE']='missing'; r=self.call(); self.assertNotEqual(r.returncode,0); self.assertFalse(list(self.dest.iterdir()))
    def test_wrong_certificate_never_published(self):
        self.env['MOCK_SIGNER']='c'*64; r=self.call(); self.assertNotEqual(r.returncode,0); self.assertFalse(list(self.dest.iterdir()))
    def test_digest_mismatch_never_published(self):
        self.env['MOCK_MODE']='tampered'; r=self.call(); self.assertNotEqual(r.returncode,0); self.assertFalse(list(self.dest.iterdir()))
    def test_never_overwrites(self):
        p=self.dest/('NeoStoreObtainX-debug-'+SHA[:12]+'-r99a2.apk'); p.write_bytes(b'existing'); r=self.call(); self.assertNotEqual(r.returncode,0); self.assertEqual(p.read_bytes(),b'existing')

if __name__=='__main__': unittest.main()
