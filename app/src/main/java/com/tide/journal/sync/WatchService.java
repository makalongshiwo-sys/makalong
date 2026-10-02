package com.tide.journal.sync;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import com.tide.journal.MainActivity;
import com.tide.journal.R;
import com.tide.journal.data.*;
import com.tide.journal.domain.PriceWindow;
import java.util.*;
import java.util.concurrent.*;

/** Explicit, visible data-sync session. Android 15+ also enforces its daily six-hour limit. */
public final class WatchService extends Service {
 private final ExecutorService executor=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());
 private final Map<String,PriceWindow> history=new HashMap<>();
 private Future<?> request;private volatile boolean running;private long started,checkedAt;
 private boolean enabled(){return running&&!Thread.currentThread().isInterrupted()&&AlertJob.prefs(this).getBoolean("alerts",false);}
 public static void start(Context c){c.startForegroundService(new Intent(c,WatchService.class));}
 public static void stop(Context c){AlertJob.prefs(c).edit().putBoolean("watchWanted",false).apply();c.stopService(new Intent(c,WatchService.class));}
 private Notification notice(String detail){
  NotificationManager m=getSystemService(NotificationManager.class);m.createNotificationChannel(new NotificationChannel("live-watch","实时盯盘状态",NotificationManager.IMPORTANCE_LOW));
  PendingIntent open=PendingIntent.getActivity(this,7,new Intent(this,MainActivity.class).putExtra("tab",4),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getService(this,8,new Intent(this,WatchService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Builder(this,"live-watch").setSmallIcon(R.drawable.notification).setContentTitle("观潮 · 实时盯盘").setContentText(detail).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_SERVICE).addAction(new Notification.Action.Builder(null,"停止盯盘",stop).build()).build();
 }
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent!=null&&"stop".equals(intent.getAction())){AlertJob.prefs(this).edit().putBoolean("watchWanted",false).apply();stopSelf();return START_NOT_STICKY;}
  android.content.SharedPreferences prefs=AlertJob.prefs(this);long previous=prefs.getLong("watchStarted",0),age=System.currentTimeMillis()-previous;
  if(intent==null&&(!prefs.getBoolean("watchWanted",false)||previous<=0||age<0||age>=6*60*60*1000L)){stopSelf();return START_NOT_STICKY;}
  if(!AlertJob.prefs(this).getBoolean("alerts",false)||!AlertJob.allowed(this)){stopSelf();return START_NOT_STICKY;}
  try{Notification n=notice("BTC · ETH · SOL | 正在连接行情");if(Build.VERSION.SDK_INT>=29)startForeground(900,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);else startForeground(900,n);}
  catch(RuntimeException e){AlertJob.prefs(this).edit().putString("watchResult","系统暂未允许盯盘："+e.getClass().getSimpleName()).apply();stopSelf();return START_NOT_STICKY;}
  if(!running){running=true;started=SystemClock.elapsedRealtime()-(intent==null?age:0);AlertJob.prefs(this).edit().putBoolean("watchRunning",true).putBoolean("watchWanted",true).putLong("watchStarted",intent==null?previous:System.currentTimeMillis()).putString("watchResult","正在连接行情").apply();poll();}
  return START_STICKY;
 }
 @Override public void onTaskRemoved(Intent rootIntent){AlertJob.prefs(this).edit().putLong("watchTaskRemovedAt",System.currentTimeMillis()).apply();super.onTaskRemoved(rootIntent);}
 private void poll(){
  if(!running)return;if(SystemClock.elapsedRealtime()-started>=6*60*60*1000L||!AlertJob.prefs(this).getBoolean("alerts",false)){stopSelf();return;}
  request=HttpRequests.submit(executor,()->{
   try{int valid=0;StringBuilder prices=new StringBuilder();String error="";Repository repo=Repository.get(this);
   for(String coin:new String[]{"BTC","ETH","SOL"}){if(!enabled())return;try{Repository.Quote q=repo.quote(coin);valid++;prices.append(prices.length()==0?"":" · ").append(coin).append(" ").append(String.format(Locale.US,q.price>=1000?"%,.0f":"%.2f",q.price));volatility(coin,q);}catch(Exception e){if(!enabled())return;error="行情连接暂不可用，暂停新信号";}}
   final int coverage=valid;final String state=valid==3?prices.toString():error;
   main.post(()->{if(!enabled())return;AlertJob.prefs(this).edit().putLong("watchAttempt",System.currentTimeMillis()).putString("watchResult",state).apply();if(coverage==3)AlertJob.prefs(this).edit().putLong("watchResponse",System.currentTimeMillis()).apply();getSystemService(NotificationManager.class).notify(900,notice(state));});
   if(enabled()&&SystemClock.elapsedRealtime()-checkedAt>=60000){checkedAt=SystemClock.elapsedRealtime();SignalChecks.check(this,this::enabled);}
   }catch(Exception e){if(enabled())AlertJob.prefs(this).edit().putString("watchResult","检查暂未完成，请查看网络与后台设置").apply();}
   finally{main.postDelayed(()->{if(running)poll();},15000);}
  });
 }
 private void volatility(String coin,Repository.Quote q){
  android.content.SharedPreferences prefs=AlertJob.prefs(this);String key="lastVolatility-"+coin;
  PriceWindow window=history.computeIfAbsent(coin,k->new PriceWindow(prefs.getLong(key,0)));
  PriceWindow.Move move=window.observe(q.price,q.asOf,System.currentTimeMillis(),prefs.getFloat("volatilityPct",1f));
  if(move!=null&&SignalChecks.deliverVolatility(this,this::enabled,coin,move.percent,"volatility:"+coin+":"+move.at,coin+" 短时波动 "+String.format(Locale.US,"%+.2f%%",move.percent),"最近 "+move.seconds+" 秒，由 "+String.format(Locale.US,"%,.2f",move.before)+" 到 "+String.format(Locale.US,"%,.2f",move.price)+" USDT。来自新鲜报价；不是买卖建议。",move.at)){window.markAlert(move.at);}
 }
 @Override public void onTimeout(int startId,int fgsType){AlertJob.prefs(this).edit().putBoolean("watchWanted",false).putString("watchResult","本次盯盘已到系统时长限制，请回到应用重新开启").apply();stopSelf();}
 @Override public void onDestroy(){running=false;main.removeCallbacksAndMessages(null);if(request!=null)request.cancel(true);executor.shutdownNow();AlertJob.prefs(this).edit().putBoolean("watchRunning",false).apply();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
