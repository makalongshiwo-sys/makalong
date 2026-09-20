"""Assistant-side publisher preparation. Run only after user explicitly asks 更新.
No credential is stored here. Commit the resulting content/reports.json using the
connected GitHub tool, then read the public revision back before reporting success.
"""
from pathlib import Path
import argparse,json,datetime,hashlib
root=Path(__file__).resolve().parents[1]
def instant(s):return datetime.datetime.fromisoformat(s.replace('Z','+00:00'))
def validate(report,existing,now):
 for key in ('id','title','summary','period','kind','publishedAt','validUntil','sources','sections','assets','events','qa'):
  if key not in report:raise ValueError('Missing '+key)
 if report['kind']!='market' or report['period'] not in ('daily','weekly'):raise ValueError('Expected a market report')
 if any(r['id']==report['id'] for r in existing['reports']):raise ValueError('Report ID already published; never rewrite history')
 published=instant(report['publishedAt']);until=instant(report['validUntil'])
 if published>now or until<=published:raise ValueError('Publication or review timestamp invalid')
 if not report['sources']:raise ValueError('A current market report needs actual sources')
 for s in report['sources']:
  if not s.get('url','').startswith('https://') or not s.get('observedAt') or not s.get('title'):raise ValueError('Source URL/title/observation time required')
  if instant(s['observedAt'])>now:raise ValueError('Future source observation')
 for ref,key in [('events','events'),('qa','knowledge')]:
  allowed={e['id'] for e in existing[key]}
  if not set(report[ref])<=allowed:raise ValueError('Unknown '+ref+' reference')
 if {a.get('symbol') for a in report['assets']}!={'BTC','ETH','SOL'}:raise ValueError('Assess all three assets independently; use 未判定 when evidence is missing')
 for a in report['assets']:
  for k in ('health','trend','risk','action','evidence','missing','invalidation','nextReview'):
   if k not in a:raise ValueError('Missing asset field '+k)
 # Deliberately not an automatic privacy guarantee. Author must remove private
 # holdings/account/cost/order data before publishing to this public repository.
def main():
 p=argparse.ArgumentParser();p.add_argument('report');p.add_argument('--write',action='store_true');a=p.parse_args()
 dest=root/'content/reports.json';existing=json.loads(dest.read_text());r=json.loads(Path(a.report).read_text());now=datetime.datetime.now(datetime.timezone.utc);validate(r,existing,now)
 existing['reports'].append(r);existing['updatedAt']=now.isoformat().replace('+00:00','Z');existing['revision']='research-'+r['id']+'-'+hashlib.sha256(json.dumps(r,sort_keys=True,ensure_ascii=False).encode()).hexdigest()[:12]
 if a.write:dest.write_text(json.dumps(existing,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({'valid':True,'written':a.write,'revision':existing['revision'],'reports':len(existing['reports'])},ensure_ascii=False))
if __name__=='__main__':main()
