"""Read-only source probe. Records upstream access/format failure without inventing history."""
from pathlib import Path
import json, subprocess, urllib.request, datetime, re
out=Path('evidence/ci');out.mkdir(parents=True,exist_ok=True)
subprocess.run(['javac','-encoding','UTF-8','-d','out/domain','app/src/main/java/com/tide/journal/domain/EtfHistory.java','tests/EtfSourceProbe.java'],check=True)
results=[]
for asset,path in [('BTC','bitcoin-etf-flow-all-data/'),('ETH','eth/'),('SOL','sol/')]:
 url='https://farside.co.uk/'+path
 result={'asset':asset,'url':url,'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat()}
 try:
  request=urllib.request.Request(url,headers={'User-Agent':'Guanchao ETF history verification/0.7'})
  with urllib.request.urlopen(request,timeout=15) as response:
   if response.geturl()!=url:raise ValueError('Unexpected redirect: '+response.geturl())
   html=response.read(2500001)
  file=out/('etf-'+asset+'.html');file.write_bytes(html)
  probe=subprocess.run(['java','-cp','out/domain','EtfSourceProbe',str(file)],capture_output=True,text=True)
  result.update(status='passed' if probe.returncode==0 else 'format-unverified',detail=(probe.stdout+probe.stderr)[:2000])
  if probe.returncode:
   text=html.decode('utf-8',errors='replace')
   result['tables']=len(re.findall(r'<table\b',text,re.I))
   result['tableStart']=[re.sub(r'<[^>]*>',' ',table)[:1200] for table in re.findall(r'(?is)<table\b.*?</table>',text)[:2]]
   result['relatedLinks']=sorted(set(re.findall(r'href=["\x27]([^"\x27]*(?:all-data|ethereum|solana|/eth/|/sol/)[^"\x27]*)',text,re.I)))[:30]
   result['pageTitle']=re.findall(r'(?is)<title>(.*?)</title>',text)[:1]
 except Exception as error:
  result.update(status='source-unavailable',detail=str(error))
 results.append(result)
(out/'etf-source-probe.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
print(json.dumps(results,ensure_ascii=False,indent=2))
