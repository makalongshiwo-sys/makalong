package com.tide.journal.data;
import org.json.*;import java.io.IOException;import java.util.*;
/** Validate an entire source packet before any notification or inbox write. */
public final class PushPacket {
 public final long generated;public final int coverage;public final List<Event> events;
 public static final class Event {public final String id,title,body,type,coin;public final long at;public final double change;Event(JSONObject e)throws Exception{id=e.getString("id");title=e.getString("title");body=e.getString("body");type=e.getString("type");at=e.getLong("at");coin=e.optString("coin","");change=e.optDouble("changePercent",Double.NaN);}}
 private PushPacket(long g,int c,List<Event> e){generated=g;coverage=c;events=Collections.unmodifiableList(e);}
 public static PushPacket parse(JSONObject packet,long now)throws Exception{
  long generated=packet.getLong("generatedAt"),started=packet.getLong("startedAt");int coverage=packet.getInt("validCoverage");
  if(packet.getInt("schemaVersion")!=1||!"guanchao-public-signals".equals(packet.getString("feed"))||generated<=0||generated>now+60000||now-generated>15*60*1000L||started<=0||started>generated||coverage<0||coverage>6)throw new IOException("云端资料过期或字段无效");
  JSONArray rows=packet.getJSONArray("events");if(rows.length()>1000)throw new IOException("事件过多");List<Event> events=new ArrayList<>();Set<String> ids=new HashSet<>();
  for(int i=0;i<rows.length();i++){Event e=new Event(rows.getJSONObject(i));if(e.id.isEmpty()||e.id.length()>180||e.title.isEmpty()||e.title.length()>160||e.body.length()>4000||e.at<started||e.at>generated||generated-e.at>86400000||!ids.add(e.id)||!Arrays.asList("close","structure","indicator","move","volatility").contains(e.type))throw new IOException("事件字段无效");if(e.type.equals("volatility")&&(!Arrays.asList("BTC","ETH","SOL").contains(e.coin)||!Double.isFinite(e.change)))throw new IOException("波动字段无效");events.add(e);}
  return new PushPacket(generated,coverage,events);
 }
}
