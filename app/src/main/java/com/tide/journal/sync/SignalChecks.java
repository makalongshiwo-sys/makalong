package com.tide.journal.sync;

import android.content.*;
import com.tide.journal.data.*;
import com.tide.journal.domain.Market;
import org.json.*;
import java.time.Instant;
import java.util.function.BooleanSupplier;

/** Shared background checks; only fresh, closed observations may deliver signals. */
public final class SignalChecks {
 private SignalChecks(){}
 public static void check(Context c,BooleanSupplier enabled){
  if(!enabled.getAsBoolean())return;SharedPreferences p=AlertJob.prefs(c);Repository repo=Repository.get(c);
  p.edit().putLong("lastAttempt",System.currentTimeMillis()).apply();int valid=0;String issue="";
  for(String coin:new String[]{"BTC","ETH","SOL"})for(String period:new String[]{"1h","4h"}){
   if(!enabled.getAsBoolean())return;
   try{Repository.Bars b=repo.bars(coin,period);if(!b.cached)p.edit().putLong("lastResponse",System.currentTimeMillis()).apply();
    Market.Analysis a=b.analysis;if(!a.valid()){issue=a.reason;continue;}valid++;
    for(Market.Event e:a.events)deliver(c,enabled,coin+":"+e.id,coin+" "+period+" · "+e.title,e.detail,e.at,e.type);
    deliver(c,enabled,coin+":"+period+":"+a.closedAt+":close",coin+" "+period+" 已闭合",a.trend+"；"+a.structure,a.closedAt,"close");
   }catch(Exception e){issue=e.getMessage()==null?"行情来源不可用":e.getMessage();}
  }
  if(enabled.getAsBoolean()&&p.getBoolean("type_report",true))try{
   Repository.Packet pack=repo.document("reports");if(!pack.state.equals("云端已读取"))issue="研究云端暂不可用";
   else{JSONArray rows=pack.data.getJSONArray("reports");for(int i=0;i<rows.length();i++){JSONObject report=rows.getJSONObject(i);if(report.optString("kind").equals("market"))deliver(c,enabled,"report:"+report.getString("id"),report.getString("title"),report.getString("summary"),Instant.parse(report.getString("publishedAt")).toEpochMilli(),"report");}}
  }catch(Exception e){issue=e.getMessage()==null?"研究来源不可用":e.getMessage();}
  if(enabled.getAsBoolean()){SharedPreferences.Editor edit=p.edit().putString("lastResult","行情有效覆盖 "+valid+"/6"+(issue.isEmpty()?"":" · "+issue));if(valid==6)edit.putLong("lastSuccess",System.currentTimeMillis());edit.apply();}
 }
 public static boolean deliverVolatility(Context c,BooleanSupplier enabled,String coin,double change,String id,String title,String body,long at){
  if(!java.util.Arrays.asList("BTC","ETH","SOL").contains(coin)||!Double.isFinite(change))return false;
  Store store=Repository.get(c).store;
  synchronized(store){SharedPreferences p=AlertJob.prefs(c);if(Math.abs(change)<p.getFloat("volatilityPct",1f)||at-p.getLong("lastVolatility-"+coin,0)<300000)return false;
   boolean accepted=deliver(c,enabled,id,title,body,at,"volatility");if(accepted)p.edit().putLong("lastVolatility-"+coin,at).apply();return accepted;}
 }
 public static boolean deliver(Context c,BooleanSupplier enabled,String id,String title,String body,long at,String type){
  long now=System.currentTimeMillis();SharedPreferences p=AlertJob.prefs(c);
  if(!enabled.getAsBoolean()||!p.getBoolean("alerts",false)||!p.getBoolean("type_"+type,true)||at<p.getLong("enabledAt",now)||at>now||now-at>86400000)return false;
  Store store=Repository.get(c).store;
  synchronized(store){store.add(id,title,body,at);if(enabled.getAsBoolean()&&p.getBoolean("alerts",false)&&!store.sent(id)&&AlertJob.allowed(c)){AlertJob.notification(c,id,title,body);store.mark(id);}return true;}
 }
}
