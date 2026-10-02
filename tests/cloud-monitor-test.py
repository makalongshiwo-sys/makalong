"""Regression for provider corruption, first-run replay and repeated cron executions."""
import importlib.util,json,pathlib,tempfile,unittest
from unittest.mock import patch
spec=importlib.util.spec_from_file_location('monitor',pathlib.Path('cloud/monitor.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
AT=1800000000000
class FeedTest(unittest.TestCase):
 def minutes(self,opening=100):return [[AT-300000+i*60000,str(opening),str(max(opening,102)),str(min(opening,99)),str(opening),10,AT-300000+i*60000+59999] for i in range(6)]
 def quote(self,price=102):return {'lastPrice':str(price),'priceChangePercent':'2','closeTime':AT}
 def test_fresh_volatility(self):
  events=m.volatility('BTC',self.quote(),self.minutes());self.assertEqual(len(events),1);self.assertAlmostEqual(events[0]['changePercent'],2);self.assertEqual(events[0]['at'],AT)
 def test_nan_zero_bad_range_and_gap(self):
  for bad in ['NaN','Infinity',0,-1]:
   with self.assertRaises(ValueError):m.volatility('BTC',self.quote(bad),self.minutes())
  with self.assertRaises(ValueError):m.volatility('BTC',self.quote(),self.minutes(0))
  for field,value in [(2,90),(3,110),(6,AT),(0,AT-299999)]:
   rows=self.minutes();rows[0][field]=value
   with self.assertRaises(ValueError):m.volatility('BTC',self.quote(),rows)
  rows=self.minutes();rows.pop(2)
  with self.assertRaises(ValueError):m.volatility('BTC',self.quote(),rows)
 def provider(self,url):
  if url.endswith('time'):return {'serverTime':AT}
  if 'ticker/' in url:return self.quote(100)
  if 'interval=1m' in url:return self.minutes()
  return [[0,100,101,99,100,1,3599999]]*500
 def test_first_run_repeat_new_close_and_stale_feed(self):
  with tempfile.TemporaryDirectory() as d,patch.object(m,'read',self.provider),patch.object(m.time,'time',return_value=AT/1000),patch.object(m.subprocess,'check_output') as analyze:
   p=pathlib.Path(d)/'state.json';old={'id':'BTC:old:close','title':'closed','body':'fixture','at':AT-3600000,'type':'close'};analyze.return_value=json.dumps([old]).encode()
   self.assertFalse(m.build(p));self.assertEqual(json.loads(p.read_text())['events'],[])
   new=dict(old,id='BTC:new:close',at=AT);analyze.return_value=json.dumps([new]).encode()
   self.assertTrue(m.build(p));self.assertFalse(m.build(p));state=json.loads(p.read_text());self.assertEqual(state['validCoverage'],6);self.assertEqual(len(state['events']),1)
   state['feed']='another-feed';p.write_text(json.dumps(state))
   with self.assertRaises(ValueError):m.build(p)
 def test_stale_clock_rejects_entire_run(self):
  with tempfile.TemporaryDirectory() as d,patch.object(m,'read',return_value={'serverTime':AT-61000}),patch.object(m.time,'time',return_value=AT/1000):
   p=pathlib.Path(d)/'state.json'
   with self.assertRaises(ValueError):m.build(p)
   self.assertFalse(p.exists())
 def test_quote_failures_do_not_look_complete(self):
  def broken(url):
   if 'ticker/' in url:return self.quote('NaN')
   return self.provider(url)
  with tempfile.TemporaryDirectory() as d,patch.object(m,'read',broken),patch.object(m.time,'time',return_value=AT/1000),patch.object(m.subprocess,'check_output',return_value=b'[]'):
   p=pathlib.Path(d)/'state.json';self.assertFalse(m.build(p));state=json.loads(p.read_text());self.assertEqual(state['validCoverage'],0);self.assertEqual(state['quotes'],{});self.assertEqual(len(state['failures']),9)
unittest.main()
