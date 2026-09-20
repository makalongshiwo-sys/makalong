from pathlib import Path
import json,hashlib,zipfile
root=Path(__file__).resolve().parents[1];apk=root/'releases/guanchao-native-preview.apk'
receipt=json.loads((root/'releases/release.json').read_text())
assert hashlib.sha256(apk.read_bytes()).hexdigest()==receipt['sha256']
assert apk.stat().st_size==receipt['bytes']
with zipfile.ZipFile(apk) as z:
 assert not any(n.endswith(('.html','.css','.js','.mjs')) for n in z.namelist())
 assert all(b'Landroid/webkit/WebView;' not in z.read(n) for n in z.namelist() if n.endswith('.dex'))
for path,digest in receipt['sourceSha256'].items():
 assert hashlib.sha256((root/path).read_bytes()).hexdigest()==digest,path+' changed without rebuilding APK'
print('PASS: exact APK hash, current source hashes, no WebView and no web runtime assets')
