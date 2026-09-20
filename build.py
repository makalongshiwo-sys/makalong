"""Build native Java/Canvas/GLES APK from official SDK. Never packages web assets.
python build.py SDK --key PRIVATE_TEST_JKS
The existing private test key is required; the builder never changes app identity.
"""
from pathlib import Path
import argparse,subprocess,shutil,zipfile,hashlib,json
p=argparse.ArgumentParser();p.add_argument('sdk');p.add_argument('--key',required=True);a=p.parse_args()
root=Path(__file__).resolve().parent;sdk=Path(a.sdk).resolve();key=Path(a.key).resolve()
assert key.is_file(),'Existing signing key required'
bt=next((p for p in [sdk/'build-tools/36.0.0',sdk/'android-16'] if p.exists()),None)
jar=next((p for p in [sdk/'platforms/android-36/android.jar',sdk/'android-36/android.jar'] if p.exists()),None)
assert bt and jar,'Install build-tools 36.0.0 and platform 36'
out=root/'out';out.mkdir(exist_ok=True);app=root/'app/src/main'
def run(*args): subprocess.run([str(x) for x in args],check=True,cwd=root)
for name in ['gen','classes','dex']:
 d=out/name
 if d.exists():shutil.rmtree(d)
 d.mkdir()
assert not any(p.suffix in ['.html','.js','.mjs','.css'] for p in (app/'assets').rglob('*'))
run(bt/'aapt2','compile','--dir',app/'res','-o',out/'res.zip')
run(bt/'aapt2','link','-o',out/'base.apk','--manifest',app/'AndroidManifest.xml','-I',jar,'-A',app/'assets','--java',out/'gen',out/'res.zip')
run('java','com.sun.tools.javac.Main','-encoding','UTF-8','-source','8','-target','8','-classpath',jar,'-d',out/'classes',*list((app/'java').rglob('*.java')),*list((out/'gen').rglob('*.java')))
run('java','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',jar,'--min-api','26','--output',out/'dex',*list((out/'classes').rglob('*.class')))
shutil.copyfile(out/'base.apk',out/'unsigned.apk')
with zipfile.ZipFile(out/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for p in (out/'dex').glob('*.dex'):z.write(p,p.name)
run(bt/'zipalign','-f','4',out/'unsigned.apk',out/'aligned.apk')
dest=root/'releases/guanchao-native-preview.apk';dest.parent.mkdir(exist_ok=True)
run('java','-jar',bt/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',dest,out/'aligned.apk')
run('java','-jar',bt/'lib/apksigner.jar','verify','--verbose','--print-certs',dest)
with zipfile.ZipFile(dest) as z:
 assert not any(n.endswith(('.html','.js','.mjs','.css')) for n in z.namelist())
 assert all(b'Landroid/webkit/WebView;' not in z.read(n) for n in z.namelist() if n.endswith('.dex'))
receipt={'sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),'bytes':dest.stat().st_size,'versionCode':6,'minSdk':26,'targetSdk':36,'webRuntime':False,'physicalDeviceTested':False}
(dest.parent/'release.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt))
