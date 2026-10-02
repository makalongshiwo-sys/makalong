package com.tide.journal.data;
import android.content.*;import android.os.SystemClock;import com.tide.journal.domain.Market;import org.json.*;import java.net.*;import java.io.*;import java.util.*;import java.util.concurrent.*;
public final class Repository {
 public static final String CLOUD="https://raw.githubusercontent.com/makalongshiwo-sys/makalong/main/content/";
 private static volatile Repository instance;public static Repository get(Context c){if(instance==null)synchronized(Repository.class){if(instance==null)instance=new Repository(c.getApplicationContext());}return instance;}
 private final Context context;public final Store store;public final ExecutorService io=Executors.newFixedThreadPool(4);public final ExecutorService quotes=Executors.newSingleThreadExecutor();private long clock,clockMono,clockRetryAt;private String clockIssue="";private Repository(Context c){context=c;store=new Store(c);}
 public static final class Packet {public JSONObject data;public String state,issue="";public long fetched;}
 public static final class Bars {public List<Market.Bar> bars;public List<Market.Point> points;public Market.Analysis analysis;public long asOf;public boolean cached;public String issue="";}
 public static String read(InputStream in,int cap)throws IOException{return HttpRequests.read(in,cap);}
 public static String http(String url)throws IOException{return HttpRequests.get(url);}
 public String asset(String name)throws Exception{try(InputStream in=context.getAssets().open(name)){return read(in,2500000);}}
 private synchronized long now()throws Exception{HttpRequests.checkCancelled();long mono=SystemClock.elapsedRealtime();if(clock>0&&mono-clockMono<10000)return clock+mono-clockMono;if(mono<clockRetryAt)throw new IOException(clockIssue);try{long t=new JSONObject(http("https://data-api.binance.vision/api/v3/time")).getLong("serverTime");if(Math.abs(t-System.currentTimeMillis())>60000)throw new IOException("来源与手机时钟偏差超60秒");clock=t;clockMono=SystemClock.elapsedRealtime();clockRetryAt=0;return t;}catch(Exception e){HttpRequests.checkCancelled();clockIssue=e.getMessage()==null?"来源校时暂不可用":e.getMessage();clockRetryAt=SystemClock.elapsedRealtime()+15000;throw e;}}
 public static List<Market.Bar> parseBars(JSONArray rows,String period,long at)throws Exception{if(rows.length()==0)throw new IOException("来源K线为空");List<Market.Bar>b=new ArrayList<>();for(int i=0;i<rows.length();i++){JSONArray r=rows.getJSONArray(i);if(r.length()<7)throw new IOException("K线字段缺失");long end=r.getLong(6);b.add(new Market.Bar(r.getLong(0),end,r.getDouble(1),r.getDouble(2),r.getDouble(3),r.getDouble(4),r.getDouble(5),end<at));}Market.analyze(b,period,at);return b;}
 public Bars bars(String coin,String period)throws Exception{if(!Arrays.asList("BTC","ETH","SOL").contains(coin))throw new IOException("币种不支持");Market.duration(period);String key=coin+period;Bars b=new Bars();JSONArray rows;try{long at=now();rows=new JSONArray(http("https://data-api.binance.vision/api/v3/klines?symbol="+coin+"USDT&interval="+period+"&limit=500"));b.asOf=at;b.bars=parseBars(rows,period,b.asOf);store.put(key,new JSONObject().put("asOf",b.asOf).put("rows",rows).toString(),System.currentTimeMillis());}catch(Exception e){HttpRequests.checkCancelled();Store.Cached cache=store.get(key);if(cache==null)throw e;JSONObject j=new JSONObject(cache.payload);b.asOf=j.getLong("asOf");b.bars=parseBars(j.getJSONArray("rows"),period,b.asOf);b.cached=true;b.issue=e.getMessage();}b.points=Market.indicators(b.bars);b.analysis=Market.analyze(b.bars,period,System.currentTimeMillis());if(b.cached||Math.abs(b.asOf-System.currentTimeMillis())>90000){b.analysis.quality="stale";b.analysis.reason="离线或旧快照，仅回看，暂停信号";b.analysis.events.clear();}return b;}
 public Packet document(String kind)throws Exception{if(!Arrays.asList("reports","etf-snapshot").contains(kind))throw new IOException();Packet p=new Packet();try{String raw=http(CLOUD+kind+".json");p.data=new JSONObject(raw);validate(kind,p.data);p.fetched=System.currentTimeMillis();p.state="云端已读取";store.put(kind,raw,p.fetched);}catch(Exception e){HttpRequests.checkCancelled();p=localDocument(kind);p.issue=e.getMessage()==null?"来源暂不可用":e.getMessage();}return p;}
 public Packet localDocument(String kind)throws Exception{if(!Arrays.asList("reports","etf-snapshot").contains(kind))throw new IOException("资料类型不支持");Store.Cached cache=store.get(kind);Packet p=new Packet();try{if(cache==null)throw new IOException("尚无缓存");p.data=new JSONObject(cache.payload);validate(kind,p.data);p.state="本机缓存";p.fetched=cache.fetched;}catch(Exception e){p.data=new JSONObject(asset(kind+".json"));validate(kind,p.data);p.state="随包历史资料";p.fetched=0;}return p;}
 public static void validate(String kind,JSONObject j)throws Exception{
  if(kind.equals("reports")){
   if(j.getInt("schemaVersion")!=1)throw new IOException("报告版本不支持");
   if(j.getString("revision").trim().isEmpty())throw new IOException("报告修订号缺失");
   Set<String> ids=new HashSet<>();JSONArray rs=j.getJSONArray("reports");
   for(int i=0;i<rs.length();i++){JSONObject r=rs.getJSONObject(i);String id=r.getString("id");if(id.isEmpty()||!ids.add(id))throw new IOException("重复或空报告编号");r.getString("title");r.getString("summary");if(r.getString("kind").equals("market")&&java.time.Instant.parse(r.getString("publishedAt")).toEpochMilli()>System.currentTimeMillis())throw new IOException("未来发布时间");}
  }else if(kind.equals("etf-snapshot")){
   if(!j.getString("unit").equals("USD million"))throw new IOException("ETF单位不匹配");
   JSONObject assets=j.getJSONObject("assets");for(String c:new String[]{"BTC","ETH","SOL"})validateEtfRows(assets.getJSONObject(c).getJSONArray("rows"));
  }else throw new IOException("资料类型不支持");
 }
 public static void validateEtfRows(JSONArray rows)throws Exception{
  Set<String> dates=new HashSet<>();java.time.LocalDate now=java.time.LocalDate.now(java.time.ZoneOffset.UTC);
  for(int i=0;i<rows.length();i++){
   JSONObject row=rows.getJSONObject(i);String date=row.getString("date");java.time.LocalDate day=java.time.LocalDate.parse(date);
   if(!day.toString().equals(date)||day.isAfter(now)||!dates.add(date))throw new IOException("ETF 日期无效或重复");
   boolean complete=row.getString("status").equals("complete");
   if(!row.has("total")||(complete&&row.isNull("total")))throw new IOException("ETF 总额字段缺失");
   if(!row.isNull("total")&&!Double.isFinite(row.getDouble("total")))throw new IOException("ETF 总额无效");
   JSONArray funds=row.getJSONArray("funds");if(complete&&funds.length()==0)throw new IOException("完整披露缺基金数字");
   Set<String> tickers=new HashSet<>();double sum=0;
   for(int k=0;k<funds.length();k++){JSONObject fund=funds.getJSONObject(k);String ticker=fund.getString("ticker");if(!ticker.matches("[A-Z][A-Z0-9]{1,7}")||!tickers.add(ticker))throw new IOException("ETF 基金列无效或重复");if(!fund.has("flow")||(complete&&fund.isNull("flow")))throw new IOException("完整披露缺基金数字");if(!fund.isNull("flow")){double flow=fund.getDouble("flow");if(!Double.isFinite(flow))throw new IOException("ETF 基金数字无效");sum+=flow;}}
   if(complete&&Math.abs(sum-row.getDouble("total"))>0.15+funds.length()*0.06)throw new IOException("ETF 基金与总额不一致");
  }
 }
 public JSONObject derivatives(String coin)throws Exception{if(!Arrays.asList("BTC","ETH","SOL").contains(coin))throw new IOException();JSONObject oi=new JSONObject(http("https://fapi.binance.com/fapi/v1/openInterest?symbol="+coin+"USDT")),f=new JSONObject(http("https://fapi.binance.com/fapi/v1/premiumIndex?symbol="+coin+"USDT"));long now=System.currentTimeMillis();if(Math.abs(oi.getLong("time")-now)>300000||Math.abs(f.getLong("time")-now)>300000)throw new IOException("衍生品陈旧");return new JSONObject().put("oi",oi).put("funding",f);}
 public JSONArray predictions()throws Exception{JSONArray all=new JSONArray(http("https://gamma-api.polymarket.com/markets?active=true&closed=false&limit=100&order=volume24hr&ascending=false")),selected=new JSONArray();for(int i=0;i<all.length();i++){JSONObject m=all.getJSONObject(i);if(m.optString("question").toLowerCase(Locale.ROOT).matches(".*(bitcoin|ethereum|solana|federal reserve|fed rate|interest rate).*"))selected.put(m);}return selected;}

 public static final class Quote {public double price,change;public long asOf;}
 public Quote quote(String coin)throws Exception{
  if(!Arrays.asList("BTC","ETH","SOL").contains(coin))throw new IOException("币种不支持");
  JSONObject j=new JSONObject(http("https://data-api.binance.vision/api/v3/ticker/24hr?symbol="+coin+"USDT"));
  Quote q=new Quote();q.price=j.getDouble("lastPrice");q.change=j.getDouble("priceChangePercent");q.asOf=j.getLong("closeTime");
  if(!Double.isFinite(q.price)||q.price<=0||!Double.isFinite(q.change)||Math.abs(q.asOf-System.currentTimeMillis())>90000)throw new IOException("报价陈旧或无效");
  return q;
 }
 public Packet cachedEtf(String coin)throws Exception{
  Store.Cached c=store.get("etf-history-"+coin);if(c==null)return null;
  Packet p=new Packet();p.data=new JSONObject(c.payload);if(!p.data.getString("unit").equals("USD million"))throw new IOException("ETF单位不匹配");validateEtfRows(p.data.getJSONArray("rows"));p.fetched=c.fetched;p.state="本机历史缓存";return p;
 }
 public Packet localEtf(String coin)throws Exception{
  if(!Arrays.asList("BTC","ETH","SOL").contains(coin))throw new IOException("币种不支持");
  Packet p;try{p=cachedEtf(coin);if(p!=null)return p;}catch(Exception ignored){HttpRequests.checkCancelled();}
  Packet snapshot=localDocument("etf-snapshot");p=new Packet();p.data=new JSONObject(snapshot.data.getJSONObject("assets").getJSONObject(coin).toString()).put("capturedAt",snapshot.data.optString("capturedAt")).put("unit","USD million");p.state=snapshot.state;p.fetched=snapshot.fetched;return p;
 }
 public Packet etfHistory(String coin)throws Exception{
  if(!Arrays.asList("BTC","ETH","SOL").contains(coin))throw new IOException("币种不支持");
  String source="https://farside.co.uk/"+(coin.equals("BTC")?"bitcoin-etf-flow-all-data/":coin.equals("ETH")?"ethereum-etf-flow-all-data/":"sol/");
  try{
   List<com.tide.journal.domain.EtfHistory.Row> history=com.tide.journal.domain.EtfHistory.parse(http(source));
   TreeMap<String,JSONObject> merged=new TreeMap<>();Packet previous;try{previous=cachedEtf(coin);}catch(Exception ignored){HttpRequests.checkCancelled();previous=null;}
   if(previous!=null){JSONArray rows=previous.data.getJSONArray("rows");for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);merged.put(r.getString("date"),r);}}
   for(com.tide.journal.domain.EtfHistory.Row r:history){
    JSONArray funds=new JSONArray();for(Map.Entry<String,Double> entry:r.funds.entrySet())funds.put(new JSONObject().put("ticker",entry.getKey()).put("flow",entry.getValue()==null?JSONObject.NULL:entry.getValue()));
    merged.put(r.date,new JSONObject().put("date",r.date).put("total",r.total==null?JSONObject.NULL:r.total).put("funds",funds).put("status",r.complete()?"complete":"partial").put("source",source));
   }
   JSONArray rows=new JSONArray();for(JSONObject row:merged.values())rows.put(row);
   Packet p=new Packet();p.fetched=System.currentTimeMillis();p.state="公开披露表已读取";
   p.data=new JSONObject().put("rows",rows).put("capturedAt",java.time.Instant.ofEpochMilli(p.fetched).toString()).put("source",source)
    .put("unit","USD million").put("compare","https://www.coinglass.com/etf/"+(coin.equals("BTC")?"bitcoin":coin.equals("ETH")?"ethereum":"solana"));
   validateEtfRows(rows);store.put("etf-history-"+coin,p.data.toString(),p.fetched);return p;
  }catch(Exception e){
   HttpRequests.checkCancelled();Packet p=localEtf(coin);
   p.issue=e.getMessage()==null?"公开来源暂不可用":e.getMessage();return p;
  }
 }
}
