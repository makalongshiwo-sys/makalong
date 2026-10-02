"""Exercise actual native controls through Android's UI hierarchy. No mock screenshots."""
from pathlib import Path
import subprocess,time,re,json,xml.etree.ElementTree as ET,hashlib
out=Path('evidence/ci');out.mkdir(parents=True,exist_ok=True);steps=[]
def adb(*args,check=True):
 return subprocess.run(['adb',*map(str,args)],capture_output=True,check=check,timeout=50).stdout
def capture(name):
 (out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
def tree():
 adb('shell','uiautomator','dump','/sdcard/tide.xml');raw=adb('shell','cat','/sdcard/tide.xml');(out/'last-ui.xml').write_bytes(raw);return ET.fromstring(raw)
def click(label,partial=False,scrolls=0):
 for attempt in range(scrolls+1):
  nodes=[n for n in tree().iter('node') if (label in n.get('text','') if partial else n.get('text')==label)]
  if nodes:
   n=nodes[0];x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')));adb('shell','input','tap',(x1+x2)//2,(y1+y2)//2);time.sleep(.6);steps.append('tap '+label);return
  if attempt<scrolls:adb('shell','input','swipe',500,1300,500,500,350);time.sleep(.4)
 raise AssertionError('Control not found: '+label)
try:
 adb('shell','wm','size','1080x2400');adb('shell','wm','density','440');adb('shell','settings','put','system','font_scale','1.0');adb('shell','cmd','uimode','night','no')
 if Path('releases/guanchao-0.8-optimized.apk').is_file():adb('install','releases/guanchao-0.8-optimized.apk')
 adb('install','-r','releases/guanchao-native-preview.apk');adb('logcat','-c');adb('shell','am','start','-W','-n','com.guanchao.app/com.tide.journal.MainActivity');time.sleep(4)
 assert b'com.guanchao.app' in adb('shell','pidof','com.guanchao.app') or adb('shell','pidof','com.guanchao.app').strip()
 assert b'package:com.tide.journal' in adb('shell','pm','list','packages','com.tide.journal')
 assert b'package:com.guanchao.app' in adb('shell','pm','list','packages','com.guanchao.app')
 capture('01-market')
 assert any(n.get('text','')=='BTC  /  USDT' for n in tree().iter('node'))
 click('ETH');click('日线',scrolls=1);capture('02-eth-daily')

 click('看懂');capture('03-explanations');click('查看解释',scrolls=2);capture('04-rates-explanation')
 assert not any(n.get('class')=='android.widget.SeekBar' for n in tree().iter('node')),'Toy slider still present'
 click('ETF');time.sleep(3);capture('05-etf-history')
 click('研报');time.sleep(6);capture('09-reports');click('打开这一期',partial=True,scrolls=3);capture('10-report-detail')
 click('提醒');capture('11-alerts');adb('shell','pm','grant','com.guanchao.app','android.permission.POST_NOTIFICATIONS');click('发送本机测试通知',scrolls=4);time.sleep(1)
 notice=adb('shell','dumpsys','notification','--noredact').decode(errors='replace');(out/'notifications.txt').write_text(notice);assert '观潮测试通知' in notice,'No submitted Android notification'
 adb('shell','input','keyevent','3');time.sleep(.5);adb('shell','am','start','-W','-n','com.guanchao.app/com.tide.journal.MainActivity');time.sleep(1);capture('12-resumed')
 # Repeat same-signer installation without deleting preferences or inbox.
 adb('install','-r','releases/guanchao-native-preview.apk')
 adb('shell','am','start','-W','-n','com.guanchao.app/com.tide.journal.MainActivity')
 adb('install','-r','releases/native-instrumentation.apk')

 for scenario,night,font in [('light','no','1.0'),('dark','yes','1.0'),('large-text','no','1.3')]:
  adb('shell','cmd','uimode','night',night);adb('shell','settings','put','system','font_scale',font);adb('shell','am','force-stop','com.guanchao.app');time.sleep(1)
  instrument=adb('shell','am','instrument','-w','-r','-e','scenario',scenario,'com.tide.journal.test/com.tide.journal.test.NativeCheck').decode(errors='replace')
  (out/('instrumentation-'+scenario+'.txt')).write_text(instrument)
  assert 'INSTRUMENTATION_CODE: -1' in instrument and '"status":"passed"' in instrument,instrument
  print('Native layout checks passed: '+scenario)

 adb('pull','/sdcard/Android/data/com.guanchao.app/files/verification',str(out/'instrumentation'))
 logs=adb('logcat','-d','-v','brief').decode(errors='replace');(out/'logcat.txt').write_text(logs);assert 'FATAL EXCEPTION' not in logs,'Runtime crash'
 result={'status':'passed','apkSha256':hashlib.sha256(Path('releases/guanchao-native-preview.apk').read_bytes()).hexdigest(),'device':adb('shell','getprop','ro.build.version.release').decode().strip(),'steps':steps,'sameSignerReinstallVerified':True,'parallelPackageName':'com.guanchao.app','physicalDevice':False,'cloudPushVerified':False,'viewport':[1080,2400],'originOsDeviceTested':False,'appearanceScenarios':['light','dark','large-text']}
 (out/'device-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2));print(json.dumps(result,ensure_ascii=False))
finally:
 adb('shell','settings','put','system','font_scale','1.0',check=False);adb('shell','cmd','uimode','night','no',check=False)
 (out/'logcat-final.txt').write_bytes(adb('logcat','-d','-v','brief',check=False));capture('final-screen')
