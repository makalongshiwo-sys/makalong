package com.tide.journal.test;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.Bitmap;import android.widget.*;
import com.tide.journal.MainActivity;import com.tide.journal.domain.*;import com.tide.journal.ui.*;import com.tide.journal.data.*;
import java.lang.reflect.*;import java.util.*;import java.io.*;

/** Separate signed instrumentation; fixtures never enter the production APK. */
public final class NativeCheck extends Instrumentation {
 private MainActivity activity;private File out;private int checks;
 public void onCreate(Bundle args){super.onCreate(args);start();}
 private Object get(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
 private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
 private void invoke(String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}
 private void check(boolean condition,String name){if(!condition)throw new AssertionError(name);checks++;}
 private interface Step{void run()throws Exception;}
 private void ui(Step step)throws Exception{Throwable[]error={null};runOnMainSync(()->{try{step.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new Exception(error[0]);waitForIdleSync();}
 private boolean containsText(android.view.View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return true;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsText(g.getChildAt(i),text))return true;}return false;}
 private boolean containsClass(android.view.View v,String name){if(v.getClass().getName().equals(name))return true;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsClass(g.getChildAt(i),name))return true;}return false;}
 private void shot(String name)throws Exception{SystemClock.sleep(700);Bitmap b=getUiAutomation().takeScreenshot();check(b!=null,"screenshot available");try(FileOutputStream f=new FileOutputStream(new File(out,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,f);}b.recycle();}
 public void onStart(){Bundle result=new Bundle();try{out=new File(getTargetContext().getExternalFilesDir(null),"verification");out.mkdirs();activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  ui(()->{set("tab",0);invoke("render");set("generation",((Integer)get("generation"))+1);set("active",false);((Handler)get("main")).removeCallbacksAndMessages(null);
   List<Market.Bar>bars=new ArrayList<>();long size=Market.duration("4h"),base=Market.boundary("4h",System.currentTimeMillis())-90*size;for(int i=0;i<90;i++){double close=100+Math.sin(i*.4)*3+i*.05;bars.add(new Market.Bar(base+i*size,base+(i+1)*size-1,close-.4,close+1,close-1,close,100+i,true));}
   CandleChart c=(CandleChart)get("chart");c.data(bars,Market.indicators(bars));((TextView)get("quote")).setText("测试夹具");((TextView)get("quoteMeta")).setText("TEST FIXTURE · 用于图表验证，不是真实行情");check(((TextView)get("reading")).getText().toString().contains("RSI14"),"native chart callback");c.zoom(.75f);
  });shot("instrumented-chart-fixture");

  for(String id:Lessons.IDS){final String current=id;ui(()->{
   set("tab",2);set("lessonId",current);invoke("render");
   check(!containsClass((android.view.View)get("body"),"android.widget.SeekBar"),"no toy slider "+current);
   check(containsText((android.view.View)get("body"),"一个例子"),"example explanation "+current);
   check(containsText((android.view.View)get("body"),"对市场的影响"),"market explanation "+current);
  });if(id.equals("etf")||id.equals("rates"))shot("instrumented-explanation-"+id);}
  ui(()->{
   set("tab",3);set("lessonId","");invoke("render");set("generation",((Integer)get("generation"))+1);
   org.json.JSONArray rows=new org.json.JSONArray();java.time.LocalDate start=java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(69);
   for(int i=0;i<70;i++)rows.put(new org.json.JSONObject().put("date",start.plusDays(i).toString()).put("total",i==69?org.json.JSONObject.NULL:i==68?1:10).put("status",i>=68?"partial":"complete").put("funds",new org.json.JSONArray()));
   set("etfData",new org.json.JSONObject().put("rows",rows));set("etfRange","全部");set("etfLimit",30);invoke("drawEtf");
   ((TextView)get("etfState")).setText("TEST FIXTURE · 70 个合成披露日，不是真实 ETF 数据");
   check(containsText((android.view.View)get("etfRows"),"加载更早记录"),"history pagination");
   check(((TextView)get("etfSummary")).getText().toString().contains("+680.0"),"partial days excluded from cumulative");
   set("etfLimit",90);invoke("drawEtf");
   check(!containsText((android.view.View)get("etfRows"),"加载更早记录"),"all 70 rows reachable");
   check(containsText((android.view.View)get("etfRows"),start.toString()),"oldest history reachable");
   set("etfRange","1个月");invoke("drawEtf");check(!containsText((android.view.View)get("etfRows"),start.toString()),"range filters oldest rows");
  });shot("instrumented-etf-history-fixture");
  ui(()->{Store s=Repository.get(activity).store;s.add("device-test-dedupe","test","test",1);s.add("device-test-dedupe","test","test",1);long count=s.notices().stream().filter(x->x[0].equals("device-test-dedupe")).count();check(count==1,"SQLite duplicate suppression");s.mark("device-test-dedupe");check(s.sent("device-test-dedupe"),"SQLite delivery persistence");s.getWritableDatabase().delete("inbox","id=?",new String[]{"device-test-dedupe"});});
  String receipt="{\"status\":\"passed\",\"checks\":"+checks+",\"fixtureScreenshotsLabeled\":true,\"physicalDevice\":false}";try(FileOutputStream f=new FileOutputStream(new File(out,"instrumentation.json"))){f.write(receipt.getBytes("UTF-8"));}result.putString("stream",receipt);finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
