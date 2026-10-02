package com.tide.journal.test;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.Bitmap;import android.widget.*;
import com.tide.journal.MainActivity;import com.tide.journal.domain.*;import com.tide.journal.ui.*;import com.tide.journal.data.*;
import com.tide.journal.sync.*;
import java.lang.reflect.*;import java.util.*;import java.io.*;

/** Separate signed instrumentation; fixtures never enter the production APK. */
public final class NativeCheck extends Instrumentation {
 private MainActivity activity;private File out;private int checks;private String scenario;
 public void onCreate(Bundle args){super.onCreate(args);scenario=args==null?"standard":args.getString("scenario","standard");start();}
 private Object get(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
 private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
 private void invoke(String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}
 private void check(boolean condition,String name){if(!condition)throw new AssertionError(name);checks++;}
 private interface Step{void run()throws Exception;}
 private void ui(Step step)throws Exception{Throwable[]error={null};runOnMainSync(()->{try{step.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new Exception(error[0]);waitForIdleSync();}
 private boolean containsText(android.view.View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return true;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsText(g.getChildAt(i),text))return true;}return false;}
 private boolean containsClass(android.view.View v,String name){if(v.getClass().getName().equals(name))return true;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsClass(g.getChildAt(i),name))return true;}return false;}
 private void checkNav()throws Exception{
  LinearLayout nav=(LinearLayout)get("nav");check(nav.getChildCount()==5,"five navigation targets");
  android.view.View chart=(android.view.View)get("chart"),scroll=(android.view.View)get("scroll");
  int[] chartAt=new int[2],scrollAt=new int[2];chart.getLocationOnScreen(chartAt);scroll.getLocationOnScreen(scrollAt);
  check(chartAt[1]+chart.getHeight()<=scrollAt[1]+scroll.getHeight()+2,"full chart fits initial phone viewport");
  TextView price=(TextView)get("quote");check(price.getPaint().measureText(price.getText().toString())<=price.getWidth()+1,"single-line price fits");
  for(int i=0;i<5;i++){
   android.view.ViewGroup item=(android.view.ViewGroup)nav.getChildAt(i);TextView label=(TextView)item.getChildAt(2);
   check(item.getWidth()>0&&item.getHeight()>=Ui.dp(activity,48),"navigation touch size "+i);
   check(label.getPaint().measureText(label.getText().toString())<=label.getWidth()+1,"navigation label fits "+i);
  }
  double contrast=(android.graphics.Color.luminance(Ui.INK)+.05)/(android.graphics.Color.luminance(Ui.SURFACE)+.05);
  check(Math.max(contrast,1/contrast)>=4.5,"body text contrast");
  contrast=(android.graphics.Color.luminance(Ui.BLUE)+.05)/(android.graphics.Color.luminance(Ui.TINT)+.05);
  check(Math.max(contrast,1/contrast)>=4.5,"selected button contrast");
  boolean night=(activity.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
  check(Ui.isDark()==night,"system appearance followed");
  check(night==scenario.equals("dark"),"requested appearance scenario active");
  float expected=scenario.equals("large-text")?1.3f:1f;
  check(Math.abs(activity.getResources().getConfiguration().fontScale-expected)<.02,"requested font scale active");
 }
 private android.view.MotionEvent touch(long start,int action,float...xs){
  android.view.MotionEvent.PointerProperties[] props=new android.view.MotionEvent.PointerProperties[xs.length];android.view.MotionEvent.PointerCoords[] coords=new android.view.MotionEvent.PointerCoords[xs.length];
  for(int i=0;i<xs.length;i++){props[i]=new android.view.MotionEvent.PointerProperties();props[i].id=i;props[i].toolType=android.view.MotionEvent.TOOL_TYPE_FINGER;coords[i]=new android.view.MotionEvent.PointerCoords();coords[i].x=xs[i];coords[i].y=100;coords[i].pressure=1;coords[i].size=1;}
  return android.view.MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,xs.length,props,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
 }
 private void chartGestures()throws Exception{
  CandleChart chart=(CandleChart)get("chart");int[] picks={0};chart.onPick(()->picks[0]++);long start=SystemClock.uptimeMillis();
  int[] actions={android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_POINTER_DOWN|(1<<8),android.view.MotionEvent.ACTION_POINTER_UP|(1<<8),android.view.MotionEvent.ACTION_UP};
  for(int i=0;i<actions.length;i++){android.view.MotionEvent e=touch(start,actions[i],i==1||i==2?new float[]{100,200}:new float[]{100});chart.onTouchEvent(e);e.recycle();}
  check(picks[0]==0,"multitouch release does not open candle details");
  for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){android.view.MotionEvent e=touch(start,action,100);chart.onTouchEvent(e);e.recycle();}
  check(picks[0]==1,"single tap still opens candle details");chart.onPick(null);
 }
 private void etfScrollRestore()throws Exception{
  final org.json.JSONObject data=(org.json.JSONObject)get("etfData");
  ui(()->{set("etfRange","全部");set("etfLimit",90);invoke("drawEtf");});SystemClock.sleep(250);
  ui(()->{
   ScrollView scroll=(ScrollView)get("scroll");scroll.scrollTo(0,1000);check(scroll.getScrollY()==1000,"ETF long history can scroll deeply");
   set("restoreScrollY",1000);invoke("render");set("generation",((Integer)get("generation"))+1);invoke("restoreScroll");
   check(((Integer)get("restoreScrollY"))==1000,"ETF restore waits for disclosure rows");
   set("etfData",data);set("etfRange","全部");set("etfLimit",90);invoke("drawEtf");
  });SystemClock.sleep(250);
  ui(()->{check(((ScrollView)get("scroll")).getScrollY()==1000,"ETF restores deep position after rows are laid out");set("etfRange","1个月");invoke("drawEtf");((ScrollView)get("scroll")).scrollTo(0,0);((TextView)get("etfState")).setText("TEST FIXTURE · 70 个合成披露日，不是真实 ETF 数据");});
 }
 private void dataGuards()throws Exception{
  Repository repo=Repository.get(activity);Repository.validate("etf-snapshot",new org.json.JSONObject(repo.asset("etf-snapshot.json")));
  check(true,"bundled ETF rows pass strict validation");
  org.json.JSONObject original=new org.json.JSONObject(repo.asset("etf-snapshot.json"));org.json.JSONArray rows=original.getJSONObject("assets").getJSONObject("BTC").getJSONArray("rows");
  org.json.JSONObject valid=null;for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).getString("status").equals("complete")){valid=rows.getJSONObject(i);break;}
  check(valid!=null,"complete validation fixture exists");
  org.json.JSONArray corrupt=new org.json.JSONArray().put(org.json.JSONObject.NULL);
  try{Repository.validateEtfRows(corrupt);throw new AssertionError("null ETF row accepted");}catch(org.json.JSONException expected){checks++;}
  org.json.JSONObject wrong=new org.json.JSONObject(valid.toString()).put("total",1234567);
  try{Repository.validateEtfRows(new org.json.JSONArray().put(wrong));throw new AssertionError("wrong total accepted");}catch(IOException expected){checks++;}
  wrong=new org.json.JSONObject(valid.toString());wrong.getJSONArray("funds").getJSONObject(0).put("flow",org.json.JSONObject.NULL);
  try{Repository.validateEtfRows(new org.json.JSONArray().put(wrong));throw new AssertionError("incomplete complete row accepted");}catch(IOException expected){checks++;}
  String key="etf-history-BTC";Store.Cached old=repo.store.get(key);
  try{repo.store.put(key,"{\"unit\":\"USD million\",\"rows\":[null]}",System.currentTimeMillis());check(repo.localEtf("BTC").data.getJSONArray("rows").length()>0,"corrupt ETF cache falls back to bundled snapshot");}
  finally{if(old==null)repo.store.getWritableDatabase().delete("cache","key=?",new String[]{key});else repo.store.put(key,old.payload,old.fetched);}
  long boundary=Market.boundary("1h",System.currentTimeMillis());org.json.JSONArray candle=new org.json.JSONArray().put(boundary-3600000).put(100).put(101).put(99).put(100).put(10).put(boundary-1);
  check(!Repository.parseBars(new org.json.JSONArray().put(candle),"1h",boundary-100).get(0).closed,"request before close retains candidate status after delayed response");
 }
 private void offlineAndSignals()throws Exception{
  Repository repo=Repository.get(activity);
  check(repo.store.notices().stream().anyMatch(x->x[1].equals("观潮测试通知")),"same-signer reinstall preserves previously created inbox data");
  for(String coin:new String[]{"BTC","ETH","SOL"}){
   Repository.Quote q=repo.localQuote(coin);check(q.cached&&q.price>0&&q.asOf>0,"offline quote is explicitly historical "+coin);
   for(String period:Market.PERIODS){Repository.Bars b=repo.localBars(coin,period);check(b.cached&&!b.analysis.valid()&&b.analysis.events.isEmpty()&&b.bars.size()>=55,"offline bars usable but never notify "+coin+period);}
  }
  android.content.SharedPreferences prefs=AlertJob.prefs(activity);java.util.Map<String,?> original=prefs.getAll();long at=System.currentTimeMillis();String id="signal-test:"+at;
  try{
   prefs.edit().putBoolean("alerts",true).putBoolean("type_close",true).putLong("enabledAt",at-1000).commit();AlertJob.channel(activity);
   check(activity.getSystemService(NotificationManager.class).getNotificationChannel("market-alerts").getImportance()==NotificationManager.IMPORTANCE_HIGH,"heads-up alert channel high importance");
   SignalChecks.deliver(activity,()->true,id,"TEST FIXTURE · closed candle","Notification pipeline fixture, not a real market event",at,"close");
   SignalChecks.deliver(activity,()->true,id,"TEST FIXTURE · closed candle","Notification pipeline fixture, not a real market event",at,"close");
   check(repo.store.sent(id),"eligible closed signal submitted and marked");
   check(repo.store.notices().stream().filter(x->x[0].equals(id)).count()==1,"repeat signal produces one inbox item");
   prefs.edit().putBoolean("type_close",false).commit();SignalChecks.deliver(activity,()->true,id+"-off","test","test",at,"close");check(!repo.store.sent(id+"-off"),"disabled category does not notify");
   SignalChecks.deliver(activity,()->true,id+"-old","test","test",at-2000,"move");check(!repo.store.sent(id+"-old"),"pre-enable events not replayed");
   SignalChecks.deliver(activity,()->false,id+"-cancel","test","test",at,"move");check(!repo.store.sent(id+"-cancel"),"cancelled check cannot deliver");
   prefs.edit().putBoolean("type_close",true).commit();ui(()->WatchService.start(activity));SystemClock.sleep(1800);
   check(prefs.getBoolean("watchRunning",false),"Android foreground watch starts from visible Activity");
   boolean ongoing=false;for(android.service.notification.StatusBarNotification n:activity.getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==900)ongoing=(n.getNotification().flags&Notification.FLAG_ONGOING_EVENT)!=0;
   check(ongoing,"watch has visible ongoing Android notification");
   ui(()->activity.moveTaskToBack(true));SystemClock.sleep(1200);check(prefs.getBoolean("watchRunning",false),"watch survives Activity entering background");
   if(scenario.equals("light")){
    long first=0,second=0,deadline=SystemClock.elapsedRealtime()+45000;long session=prefs.getLong("watchStarted",0);
    while(SystemClock.elapsedRealtime()<deadline){long response=prefs.getLong("watchResponse",0);if(response>=session){if(first==0)first=response;else if(response>first){second=response;break;}}SystemClock.sleep(500);}
    check(first>0&&second>first,"two fresh real-source quote refreshes while Activity remains in background");
    check(prefs.getString("watchResult","").matches(".*BTC [0-9,.]+.*ETH [0-9,.]+.*SOL [0-9,.]+.*"),"background status contains three live source prices");
    try(FileOutputStream f=new FileOutputStream(new File(out,"background-live-source.json"))){f.write(("{\"status\":\"passed\",\"firstSourceRefresh\":"+first+",\"secondSourceRefresh\":"+second+",\"fixtureData\":false,\"physicalDevice\":false}").getBytes("UTF-8"));}
   }
   SignalChecks.deliver(activity,()->true,id+"-background","TEST FIXTURE · background notification","Background delivery fixture, not a real market signal",System.currentTimeMillis(),"close");check(repo.store.sent(id+"-background"),"notification pipeline delivers while Activity is backgrounded");
   ui(()->WatchService.stop(activity));SystemClock.sleep(700);check(!prefs.getBoolean("watchRunning",true),"user stop cancels live watch");
   boolean remains=false;for(android.service.notification.StatusBarNotification n:activity.getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==900)remains=true;check(!remains,"stop removes ongoing notification; late work cannot restore it");
   ui(()->activity.startActivity(new Intent(activity,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));SystemClock.sleep(600);
  }finally{
   WatchService.stop(activity);android.content.SharedPreferences.Editor edit=prefs.edit().clear();for(java.util.Map.Entry<String,?> entry:original.entrySet()){Object v=entry.getValue();String k=entry.getKey();if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);else if(v instanceof Long)edit.putLong(k,(Long)v);else if(v instanceof Integer)edit.putInt(k,(Integer)v);else if(v instanceof Float)edit.putFloat(k,(Float)v);else if(v instanceof String)edit.putString(k,(String)v);}edit.commit();
   repo.store.getWritableDatabase().delete("inbox","id LIKE ?",new String[]{id+"%"});for(String suffix:new String[]{"","-background"})activity.getSystemService(NotificationManager.class).cancel(id+suffix,0);
  }
 }
 private void shot(String name)throws Exception{SystemClock.sleep(700);Bitmap b=getUiAutomation().takeScreenshot();check(b!=null,"screenshot available");try(FileOutputStream f=new FileOutputStream(new File(out,name+"-"+scenario+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,f);}b.recycle();}
 public void onStart(){Bundle result=new Bundle();try{out=new File(getTargetContext().getExternalFilesDir(null),"verification");out.mkdirs();activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  ui(()->{set("tab",0);invoke("render");set("generation",((Integer)get("generation"))+1);set("active",false);((Handler)get("main")).removeCallbacksAndMessages(null);
   List<Market.Bar>bars=new ArrayList<>();long size=Market.duration("4h"),base=Market.boundary("4h",System.currentTimeMillis())-90*size;for(int i=0;i<90;i++){double close=100+Math.sin(i*.4)*3+i*.05;bars.add(new Market.Bar(base+i*size,base+(i+1)*size-1,close-.4,close+1,close-1,close,100+i,true));}
   CandleChart c=(CandleChart)get("chart");c.data(bars,Market.indicators(bars));((TextView)get("quote")).setText("示例 · 100.42");((TextView)get("quoteMeta")).setText("示例数据 · 非真实行情");check(((TextView)get("reading")).getText().toString().contains("RSI14"),"native chart callback");c.zoom(.75f);
  });shot("instrumented-chart-fixture");ui(()->{checkNav();chartGestures();});dataGuards();offlineAndSignals();

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
  });etfScrollRestore();shot("instrumented-etf-history-fixture");
  ui(()->{set("tab",4);set("lessonId","");invoke("render");invoke("originHelp");
   check(containsText((android.view.View)get("body"),"OriginOS 后台提醒"),"OriginOS help available");
   check(containsText((android.view.View)get("body"),"即时提醒仍需接通厂商推送"),"push status not overstated");
  });
    ui(()->{Store s=Repository.get(activity).store;s.add("device-test-dedupe","test","test",1);s.add("device-test-dedupe","test","test",1);long count=s.notices().stream().filter(x->x[0].equals("device-test-dedupe")).count();check(count==1,"SQLite duplicate suppression");s.mark("device-test-dedupe");check(s.sent("device-test-dedupe"),"SQLite delivery persistence");s.getWritableDatabase().delete("inbox","id=?",new String[]{"device-test-dedupe"});});
  String receipt="{\"status\":\"passed\",\"checks\":"+checks+",\"fixtureScreenshotsLabeled\":true,\"physicalDevice\":false}";try(FileOutputStream f=new FileOutputStream(new File(out,"instrumentation.json"))){f.write(receipt.getBytes("UTF-8"));}result.putString("stream",receipt);finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
