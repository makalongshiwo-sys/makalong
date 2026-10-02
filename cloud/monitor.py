"""Public source-backed feed; no accounts, positions, device tokens or research publication."""
import argparse,json,math,pathlib,subprocess,time,urllib.request
COINS=['BTC','ETH','SOL'];PERIODS=['1h','4h'];API='https://data-api.binance.vision/api/v3/'
class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,*args,**kwargs):raise ValueError('Unexpected redirect')
OPENER=urllib.request.build_opener(NoRedirect)
def read(url):
 if not url.startswith(API):raise ValueError('Unexpected source')
 req=urllib.request.Request(url,headers={'User-Agent':'Guanchao public signals/0.9','Cache-Control':'no-cache'})
 with OPENER.open(req,timeout=12) as response:
  raw=response.read(2500001)
  if len(raw)>2500000:raise ValueError('Response limit')
  return json.loads(raw)
def number(value,positive=False):
 value=float(value)
 if not math.isfinite(value) or (positive and value<=0):raise ValueError('Invalid price')
 return value
def clock():
 value=int(read(API+'time')['serverTime'])
 if abs(value-int(time.time()*1000))>60000:raise ValueError('Source clock differs')
 return value
def volatility(coin,quote,minute):
 at=int(quote['closeTime']);price=number(quote['lastPrice'],True);bases=[];previous=0
 if not isinstance(minute,list) or len(minute)>6:raise ValueError('Minute candle limit')
 for row in minute:
  if not isinstance(row,list) or len(row)<7:raise ValueError('Invalid minute candle')
  start=int(row[0]);end=int(row[6]);opening,high,low,close=[number(x,True) for x in row[1:5]];number(row[5])
  if start%60000 or end!=start+59999 or start<=previous or high<max(opening,close) or low>min(opening,close) or high<low or number(row[5])<0:raise ValueError('Invalid minute range')
  if previous and start-previous!=60000:raise ValueError('Minute gap')
  previous=start
  if at-300000<=start<at-15000:bases.append(row)
 if not bases:return []
 base=bases[0];before=number(base[1],True);change=(price/before-1)*100
 if abs(change)<.5:return []
 return [{'id':'volatility:'+coin+':'+str(at//300000),'title':coin+' 短时波动 '+format(change,'+.2f')+'%','body':'最近 '+str((at-int(base[0]))//1000)+' 秒，'+str(before)+' → '+str(price)+' USDT；来自新鲜报价。','at':at,'type':'volatility','coin':coin,'changePercent':change}]
def build(path):
 clock();old=json.loads(path.read_text()) if path.exists() else None
 if old and (old.get('schemaVersion')!=1 or old.get('feed')!='guanchao-public-signals'):raise ValueError('Unexpected previous feed')
 latest=[];valid=0;failures=[];quotes={}
 for coin in COINS:
  try:
   quote=read(API+'ticker/24hr?symbol='+coin+'USDT');number(quote['lastPrice'],True);number(quote['priceChangePercent'])
   if abs(int(quote['closeTime'])-int(time.time()*1000))>90000:raise ValueError('Stale quote')
   quotes[coin]={k:quote[k] for k in ['lastPrice','priceChangePercent','closeTime']}
   latest.extend(volatility(coin,quote,read(API+'klines?symbol='+coin+'USDT&interval=1m&limit=6')))
  except Exception as e:failures.append(coin+' quote: '+type(e).__name__)
  for period in PERIODS:
   try:
    at=clock();rows=read(API+'klines?symbol='+coin+'USDT&interval='+period+'&limit=500')
    if not isinstance(rows,list) or not 55<=len(rows)<=500:raise ValueError('Candle count')
    raw='\n'.join('\t'.join(str(x) for x in row[:7]) for row in rows)
    events=json.loads(subprocess.check_output(['java','-cp','out/cloud','CloudAnalysis',coin,period,str(at)],input=raw.encode(),timeout=8))
    if not events:raise ValueError('No valid closed candle')
    latest.extend(events);valid+=1
   except Exception as e:failures.append(coin+period+': '+type(e).__name__)
 generated=int(time.time()*1000);started=old['startedAt'] if old else generated
 if not 0<started<=generated:raise ValueError('Invalid baseline')
 # First deployment establishes a baseline; no replay of previously closed hours.
 existing={x['id']:x for x in (old or {}).get('events',[]) if 0<=generated-x['at']<=86400000};before=set(existing)
 for event in latest:
  if started<=event['at']<=generated and generated-event['at']<=86400000:existing[event['id']]=event
 output={'schemaVersion':1,'feed':'guanchao-public-signals','generatedAt':generated,'startedAt':started,'source':'Binance spot / USDT / UTC','validCoverage':valid,'failures':failures,'quotes':quotes,'events':sorted(existing.values(),key=lambda x:x['at'])[-1000:]}
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(output,ensure_ascii=False,separators=(',',':'),allow_nan=False)+'\n')
 return bool(set(existing)-before)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--state',default='signals/state.json');a=p.parse_args();changed=build(pathlib.Path(a.state));print('NEW_SIGNALS='+('true' if changed else 'false'))
