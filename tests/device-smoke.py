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
 adb('install','-r','releases/guanchao-native-preview.apk');adb('logcat','-c');adb('shell','am','start','-W','-n','com.tide.journal/.MainActivity');time.sleep(4)
 assert b'com.tide.journal' in adb('shell','pidof','com.tide.journal') or adb('shell','pidof','com.tide.journal').strip()
 capture('01-market');click('ETH');click('日线',scrolls=1);capture('02-eth-daily')
 click('课堂');capture('03-classroom');click('进入实验  →',scrolls=2);capture('04-rates-scene')
 # Find the actual native SeekBar, change it, then open a matching numeric explanation.
 for _ in range(3):
  seek=next((n for n in tree().iter('node') if n.get('class')=='android.widget.SeekBar'),None)
  if seek is not None:break
  adb('shell','input','swipe',500,1300,500,700,350)
 assert seek is not None,'Native slider missing'
 x1,y1,x2,y2=map(int,re.findall(r'\d+',seek.get('bounds')));adb('shell','input','tap',x2-25,(y1+y2)//2);time.sleep(1);capture('05-slider-changed')
 click('新年利息',partial=True,scrolls=2);capture('06-formula-dialog');click('明白了');click('下一步',scrolls=3);capture('07-lesson-step')
 click('资金');time.sleep(3);capture('08-etf');click('研报');time.sleep(6);capture('09-reports');click('打开这一期',partial=True,scrolls=3);capture('10-report-detail')
 click('提醒');capture('11-alerts');adb('shell','pm','grant','com.tide.journal','android.permission.POST_NOTIFICATIONS');click('发送本机测试通知',scrolls=4);time.sleep(1)
 notice=adb('shell','dumpsys','notification','--noredact').decode(errors='replace');(out/'notifications.txt').write_text(notice);assert '观潮测试通知' in notice,'No submitted Android notification'
 adb('shell','input','keyevent','3');time.sleep(.5);adb('shell','am','start','-W','-n','com.tide.journal/.MainActivity');time.sleep(1);capture('12-resumed')
 adb('install','-r','releases/native-instrumentation.apk')
 instrument=adb('shell','am','instrument','-w','com.tide.journal.test/com.tide.journal.test.NativeCheck').decode(errors='replace');(out/'instrumentation-output.txt').write_text(instrument);assert 'INSTRUMENTATION_CODE: -1' in instrument and '"status":"passed"' in instrument,instrument
 adb('pull','/sdcard/Android/data/com.tide.journal/files/verification',str(out/'instrumentation'))
 logs=adb('logcat','-d','-v','brief').decode(errors='replace');(out/'logcat.txt').write_text(logs);assert 'FATAL EXCEPTION' not in logs,'Runtime crash'
 result={'status':'passed','apkSha256':hashlib.sha256(Path('releases/guanchao-native-preview.apk').read_bytes()).hexdigest(),'device':adb('shell','getprop','ro.build.version.release').decode().strip(),'steps':steps,'physicalDevice':False,'cloudPushVerified':False}
 (out/'device-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2));print(json.dumps(result,ensure_ascii=False))
finally:
 (out/'logcat-final.txt').write_bytes(adb('logcat','-d','-v','brief',check=False));capture('final-screen')
