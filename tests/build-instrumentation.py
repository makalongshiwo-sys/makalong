from pathlib import Path
import subprocess,sys,shutil,zipfile
root=Path(__file__).resolve().parents[1];sdk=Path(sys.argv[1]).resolve();key=Path(sys.argv[2]).resolve();bt=sdk/'android-16';jar=sdk/'android-36/android.jar';out=root/'out/instrumentation';out.mkdir(parents=True,exist_ok=True)
def run(*args):subprocess.run([str(x) for x in args],check=True,cwd=root)
for name in ['classes','dex']:(out/name).mkdir(exist_ok=True)
run(bt/'aapt2','link','-o',out/'base.apk','--manifest',root/'tests/android/AndroidManifest.xml','-I',jar)
run('java','com.sun.tools.javac.Main','-encoding','UTF-8','-source','8','-target','8','-cp',str(jar)+':'+str(root/'out/classes'),'-d',out/'classes',root/'tests/android/NativeCheck.java')
run('java','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',jar,'--classpath',root/'out/classes','--min-api','26','--output',out/'dex',*list((out/'classes').rglob('*.class')))
shutil.copyfile(out/'base.apk',out/'unsigned.apk')
with zipfile.ZipFile(out/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for p in (out/'dex').glob('*.dex'):z.write(p,p.name)
run(bt/'zipalign','-f','4',out/'unsigned.apk',out/'aligned.apk')
run('java','-jar',bt/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',root/'releases/native-instrumentation.apk',out/'aligned.apk')
