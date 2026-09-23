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
 private void shot(String name)throws Exception{SystemClock.sleep(700);Bitmap b=getUiAutomation().takeScreenshot();check(b!=null,"screenshot available");try(FileOutputStream f=new FileOutputStream(new File(out,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,f);}b.recycle();}
 public void onStart(){Bundle result=new Bundle();try{out=new File(getTargetContext().getExternalFilesDir(null),"verification");out.mkdirs();activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  ui(()->{set("tab",0);invoke("render");set("generation",((Integer)get("generation"))+1);set("active",false);((Handler)get("main")).removeCallbacksAndMessages(null);
   List<Market.Bar>bars=new ArrayList<>();long size=Market.duration("4h"),base=Market.boundary("4h",System.currentTimeMillis())-90*size;for(int i=0;i<90;i++){double close=100+Math.sin(i*.4)*3+i*.05;bars.add(new Market.Bar(base+i*size,base+(i+1)*size-1,close-.4,close+1,close-1,close,100+i,true));}
   CandleChart c=(CandleChart)get("chart");c.data(bars,Market.indicators(bars));((TextView)get("quote")).setText("测试夹具");((TextView)get("quoteMeta")).setText("TEST FIXTURE · 用于图表验证，不是真实行情");check(((TextView)get("reading")).getText().toString().contains("RSI14"),"native chart callback");c.zoom(.75f);
  });shot("instrumented-chart-fixture");
  for(String id:Lessons.IDS){final String current=id;ui(()->{set("tab",2);set("lessonId",current);set("stage",0);invoke("render");check(get("scene") instanceof SpatialLesson,"native GLES scene "+current);SpatialLesson s=(SpatialLesson)get("scene");s.resumeScene();double v=current.equals("rates")?10:current.equals("cuts")?1:current.equals("inflation")?50:current.equals("supply")?0:current.equals("jobs")?10:current.equals("etf")?0:current.equals("expectations")?50:current.equals("oi")?-50:5;Method m=MainActivity.class.getDeclaredMethod("updateLesson",double.class);m.setAccessible(true);m.invoke(activity,v);s.expanded(true);check(((List<?>)get("numbers")).size()==3,"accessible model readings");});shot("instrumented-lesson-"+id);}
  ui(()->{Store s=Repository.get(activity).store;s.add("device-test-dedupe","test","test",1);s.add("device-test-dedupe","test","test",1);long count=s.notices().stream().filter(x->x[0].equals("device-test-dedupe")).count();check(count==1,"SQLite duplicate suppression");s.mark("device-test-dedupe");check(s.sent("device-test-dedupe"),"SQLite delivery persistence");s.getWritableDatabase().delete("inbox","id=?",new String[]{"device-test-dedupe"});});
  String receipt="{\"status\":\"passed\",\"checks\":"+checks+",\"fixtureScreenshotsLabeled\":true,\"physicalDevice\":false}";try(FileOutputStream f=new FileOutputStream(new File(out,"instrumentation.json"))){f.write(receipt.getBytes("UTF-8"));}result.putString("stream",receipt);finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
