package com.tide.journal.data;
import android.content.*;import android.os.SystemClock;import com.tide.journal.domain.Market;import org.json.*;import java.net.*;import java.io.*;import java.util.*;import java.util.concurrent.*;
public final class Repository {
 public static final String CLOUD="https://raw.githubusercontent.com/makalongshiwo-sys/makalong/main/content/";
 private static volatile Repository instance;public static Repository get(Context c){if(instance==null)synchronized(Repository.class){if(instance==null)instance=new Repository(c.getApplicationContext());}return instance;}
 private final Context context;public final Store store;public final ExecutorService io=Executors.newFixedThreadPool(4);public final ExecutorService quotes=Executors.newSingleThreadExecutor();private long clock,clockMono;private Repository(Context c){context=c;store=new Store(c);}
 public static final class Packet {public JSONObject data;public String state,issue="";public long fetched;}
 public static final class Bars {public List<Market.Bar> bars;public List<Market.Point> points;public Market.Analysis analysis;public long asOf;public boolean cached;public String issue="";}
 public static String read(InputStream in,int cap)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[]b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();out.write(b,0,n);if(out.size()>cap)throw new IOException("响应过大");}return out.toString("UTF-8");}
 public static String http(String url)throws IOException{URI u=URI.create(url);if(!"https".equals(u.getScheme())||!Arrays.asList("raw.githubusercontent.com","data-api.binance.vision","fapi.binance.com","gamma-api.polymarket.com","www.federalreserve.gov","farside.co.uk").contains(u.getHost()))throw new IOException("来源未获准");HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();try{c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(14000);c.setRequestProperty("Cache-Control","no-cache");c.setRequestProperty("User-Agent","Guanchao ETF history verification/0.7");if(c.getResponseCode()!=200)throw new IOException("来源 HTTP "+c.getResponseCode());try(InputStream in=c.getInputStream()){return read(in,2500000);}}finally{c.disconnect();}}
 public String asset(String name)throws Exception{try(InputStream in=context.getAssets().open(name)){return read(in,2500000);}}
 private synchronized long now()throws Exception{long mono=SystemClock.elapsedRealtime();if(clock>0&&mono-clockMono<10000)return clock+mono-clockMono;long t=new JSONObject(http("https://data-api.binance.vision/api/v3/time")).getLong("serverTime");if(Math.abs(t-System.currentTimeMillis())>60000)throw new IOException("来源与手机时钟偏差超60秒");clock=t;clockMono=SystemClock.elapsedRealtime();return t;}
 public static List<Market.Bar> parseBars(JSONArray rows,String period,long at)throws Exception{if(rows.length()==0)throw new IOException("来源K线为空");List<Market.Bar>b=new ArrayList<>();for(int i=0;i<rows.length();i++){JSONArray r=rows.getJSONArray(i);if(r.length()<7)throw new IOException("K线字段缺失");long end=r.getLong(6);b.add(new Market.Bar(r.getLong(0),end,r.getDouble(1),r.getDouble(2),r.getDouble(3),r.getDouble(4),r.getDouble(5),end<at));}Market.analyze(b,period,at);return b;}
 public Bars bars(String coin,String period)throws Exception{if(!Arrays.asList("BTC","ETH","SOL").contains(coin))throw new IOException("币种不支持");Market.duration(period);String key=coin+period;Bars b=new Bars();JSONArray rows;try{long at=now(),mono=SystemClock.elapsedRealtime();rows=new JSONArray(http("https://data-api.binance.vision/api/v3/klines?symbol="+coin+"USDT&interval="+period+"&limit=500"));b.asOf=at+SystemClock.elapsedRealtime()-mono;b.bars=parseBars(rows,period,b.asOf);store.put(key,new JSONObject().put("asOf",b.asOf).put("rows",rows).toString(),System.currentTimeMillis());}catch(Exception e){Store.Cached cache=store.get(key);if(cache==null)throw e;JSONObject j=new JSONObject(cache.payload);b.asOf=j.getLong("asOf");b.bars=parseBars(j.getJSONArray("rows"),period,b.asOf);b.cached=true;b.issue=e.getMessage();}b.points=Market.indicators(b.bars);b.analysis=Market.analyze(b.bars,period,System.currentTimeMillis());if(b.cached||Math.abs(b.asOf-System.currentTimeMillis())>90000){b.analysis.quality="stale";b.analysis.reason="离线或旧快照，仅回看，暂停信号";b.analysis.events.clear();}return b;}
 public Packet document(String kind)throws Exception{if(!Arrays.asList("reports","etf-snapshot").contains(kind))throw new IOException();Packet p=new Packet();try{String raw=http(CLOUD+kind+".json");p.data=new JSONObject(raw);validate(kind,p.data);p.fetched=System.currentTimeMillis();p.state="云端已读取";store.put(kind,raw,p.fetched);}catch(Exception e){p.issue=e.getMessage();Store.Cached cache=store.get(kind);p.data=new JSONObject(cache==null?asset(kind+".json"):cache.payload);validate(kind,p.data);p.state=cache==null?"随包历史资料":"本机缓存";p.fetched=cache==null?0:cache.fetched;}return p;}
 public static void validate(String kind,JSONObject j)throws Exception{if(kind.equals("reports")){if(j.getInt("schemaVersion")!=1)throw new IOException("报告版本不支持");j.getString("revision");Set<String>ids=new HashSet<>();JSONArray rs=j.getJSONArray("reports");for(int i=0;i<rs.length();i++){JSONObject r=rs.getJSONObject(i);if(!ids.add(r.getString("id")))throw new IOException("重复报告");r.getString("title");r.getString("summary");if(r.getString("kind").equals("market")&&java.time.Instant.parse(r.getString("publishedAt")).toEpochMilli()>System.currentTimeMillis())throw new IOException("未来发布时间");}}else{if(!j.getString("unit").equals("USD million"))throw new IOException("ETF单位不匹配");JSONObject assets=j.getJSONObject("assets");for(String c:new String[]{"BTC","ETH","SOL"}){Set<String>dates=new HashSet<>();JSONArray rows=assets.getJSONObject(c).getJSONArray("rows");for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(!dates.add(row.getString("date")))throw new IOException("重复日期");if(row.optString("status").equals("complete")&&row.isNull("total"))throw new IOException("完整行缺总额");}}}}
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
  Packet p=new Packet();p.data=new JSONObject(c.payload);p.fetched=c.fetched;p.state="本机历史缓存";return p;
 }
 public Packet etfHistory(String coin)throws Exception{
  if(!Arrays.asList("BTC","ETH","SOL").contains(coin))throw new IOException("币种不支持");
  String source="https://farside.co.uk/"+(coin.equals("BTC")?"bitcoin-etf-flow-all-data/":coin.equals("ETH")?"ethereum-etf-flow-all-data/":"sol/");
  try{
   List<com.tide.journal.domain.EtfHistory.Row> history=com.tide.journal.domain.EtfHistory.parse(http(source));
   TreeMap<String,JSONObject> merged=new TreeMap<>();Packet previous=cachedEtf(coin);
   if(previous!=null){JSONArray rows=previous.data.getJSONArray("rows");for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);merged.put(r.getString("date"),r);}}
   for(com.tide.journal.domain.EtfHistory.Row r:history){
    JSONArray funds=new JSONArray();for(Map.Entry<String,Double> entry:r.funds.entrySet())funds.put(new JSONObject().put("ticker",entry.getKey()).put("flow",entry.getValue()==null?JSONObject.NULL:entry.getValue()));
    merged.put(r.date,new JSONObject().put("date",r.date).put("total",r.total==null?JSONObject.NULL:r.total).put("funds",funds).put("status",r.complete()?"complete":"partial").put("source",source));
   }
   JSONArray rows=new JSONArray();for(JSONObject row:merged.values())rows.put(row);
   Packet p=new Packet();p.fetched=System.currentTimeMillis();p.state="公开披露表已读取";
   p.data=new JSONObject().put("rows",rows).put("capturedAt",java.time.Instant.ofEpochMilli(p.fetched).toString()).put("source",source)
    .put("unit","USD million").put("compare","https://www.coinglass.com/etf/"+(coin.equals("BTC")?"bitcoin":coin.equals("ETH")?"ethereum":"solana"));
   store.put("etf-history-"+coin,p.data.toString(),p.fetched);return p;
  }catch(Exception e){
   Packet p=cachedEtf(coin);if(p==null){Packet snapshot=document("etf-snapshot");JSONObject asset=snapshot.data.getJSONObject("assets").getJSONObject(coin);
    p=new Packet();p.data=new JSONObject(asset.toString()).put("capturedAt",snapshot.data.optString("capturedAt")).put("unit","USD million");p.fetched=snapshot.fetched;p.state=snapshot.state;
   }p.issue=e.getMessage()==null?"公开来源暂不可用":e.getMessage();return p;
  }
 }
}
