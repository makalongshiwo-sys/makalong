package com.tide.journal.sync;

import android.app.*;import android.content.*;import android.content.pm.ServiceInfo;import android.os.*;
import com.tide.journal.MainActivity;import com.tide.journal.R;import com.tide.journal.data.*;
import org.json.*;import java.io.*;import java.net.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicLong;

/** Google-independent stream subscription. Payloads are wake-ups; only our HTTPS feed can notify. */
public final class PushService extends Service {
 public static final String TOPIC="guanchao-signals-1312595426",STATE="https://api.github.com/repos/makalongshiwo-sys/makalong/contents/signals/state.json?ref=guanchao-live-signals";
 private final ExecutorService stream=Executors.newSingleThreadExecutor(),fetch=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());
 private final AtomicLong lastPull=new AtomicLong(),lastApiPull=new AtomicLong();private volatile boolean running;private volatile HttpURLConnection connection;private Future<?> listener,pull;
 private final Runnable tick=new Runnable(){public void run(){if(!running)return;requestFeed();main.postDelayed(this,5*60*1000L);}};
 public static void start(Context c){AlertJob.prefs(c).edit().putBoolean("pushWanted",true).apply();c.startForegroundService(new Intent(c,PushService.class));}
 public static void stop(Context c){AlertJob.prefs(c).edit().putBoolean("pushWanted",false).putString("pushState","已停止远程通知连接").apply();c.stopService(new Intent(c,PushService.class));}
 private boolean enabled(){return running&&!Thread.currentThread().isInterrupted()&&AlertJob.prefs(this).getBoolean("alerts",false)&&AlertJob.prefs(this).getBoolean("pushWanted",false);}
 private Notification notice(String detail){NotificationManager m=getSystemService(NotificationManager.class);m.createNotificationChannel(new NotificationChannel("push-link","远程通知连接",NotificationManager.IMPORTANCE_LOW));PendingIntent open=PendingIntent.getActivity(this,17,new Intent(this,MainActivity.class).putExtra("tab",4),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent stop=PendingIntent.getService(this,18,new Intent(this,PushService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new Notification.Builder(this,"push-link").setSmallIcon(R.drawable.notification).setContentTitle("观潮 · 远程提醒连接").setContentText(detail).setOnlyAlertOnce(true).setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,"停止连接",stop).build()).build();}
 private void state(String value){main.post(()->{if(!enabled())return;AlertJob.prefs(this).edit().putString("pushState",value).apply();getSystemService(NotificationManager.class).notify(901,notice(value));});}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent!=null&&"stop".equals(intent.getAction())){AlertJob.prefs(this).edit().putBoolean("pushWanted",false).apply();stopSelf();return START_NOT_STICKY;}
  if(!AlertJob.prefs(this).getBoolean("pushWanted",false)||!AlertJob.prefs(this).getBoolean("alerts",false)||!AlertJob.allowed(this)){stopSelf();return START_NOT_STICKY;}
  try{if(Build.VERSION.SDK_INT>=34)startForeground(901,notice("正在连接，无需 Google 服务"),ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(901,notice("正在连接，无需 Google 服务"));}catch(RuntimeException e){AlertJob.prefs(this).edit().putString("pushState","系统暂未允许连接，请查看后台设置").apply();stopSelf();return START_NOT_STICKY;}
  if(!running){running=true;AlertJob.prefs(this).edit().putString("pushState","正在连接").putLong("pushStarted",System.currentTimeMillis()).apply();listener=stream.submit(this::listen);main.postDelayed(tick,5*60*1000L);}
  return START_STICKY;
 }
 private void listen(){int retry=2;while(enabled()){
  try{HttpURLConnection c=(HttpURLConnection)new URL("https://ntfy.sh/"+TOPIC+"/json").openConnection();connection=c;c.setConnectTimeout(8000);c.setReadTimeout(70000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","Guanchao native push/0.9");if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
   retry=2;state("连接已建立 · 等待云端事件");AlertJob.prefs(this).edit().putLong("pushConnected",System.currentTimeMillis()).apply();requestFeed();
   try(InputStream in=c.getInputStream()){ByteArrayOutputStream line=new ByteArrayOutputStream();int ch;while(enabled()&&(ch=in.read())!=-1){if(ch=='\n'){if(line.size()>0){JSONObject row=new JSONObject(line.toString("UTF-8"));if("message".equals(row.optString("event"))){AlertJob.prefs(this).edit().putLong("pushWakeReceived",System.currentTimeMillis()).apply();requestFeed();}}line.reset();}else{if(line.size()>=16384)throw new IOException("推送行过大");line.write(ch);}}}
  }catch(Exception e){if(enabled())state("连接中断 · 自动重连，不使用旧数据发信号");}
  finally{HttpURLConnection c=connection;connection=null;if(c!=null)c.disconnect();}
  if(!enabled())break;try{Thread.sleep(retry*1000L);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}retry=Math.min(60,retry*2);
 }}
 private void requestFeed(){long now=SystemClock.elapsedRealtime(),previous=lastPull.get();if((previous>0&&now-previous<60000)||!lastPull.compareAndSet(previous,now))return;
  pull=HttpRequests.submit(fetch,()->{try{JSONObject packet;
   try{packet=new JSONObject(Repository.http("https://raw.githubusercontent.com/makalongshiwo-sys/makalong/guanchao-live-signals/signals/state.json?t="+(System.currentTimeMillis()/120000)));}
   catch(Exception unavailable){long last=lastApiPull.get(),elapsed=SystemClock.elapsedRealtime();if(last>0&&elapsed-last<5*60*1000L)throw new IOException("备用接口等待下一次检查");lastApiPull.set(elapsed);JSONObject response=new JSONObject(Repository.http(STATE));packet=new JSONObject(new String(android.util.Base64.decode(response.getString("content"),android.util.Base64.DEFAULT),"UTF-8"));}
   receive(packet);}catch(Exception e){if(enabled())state("连接与云端读取待恢复 · 正在重试");}});

 }
 void receive(JSONObject packet)throws Exception{
  PushPacket feed=PushPacket.parse(packet,System.currentTimeMillis());
  for(PushPacket.Event event:feed.events){if(!enabled())return;String body=event.body+"\n云端观察："+com.tide.journal.ui.Ui.time(feed.generated)+"\n事件确认："+com.tide.journal.ui.Ui.time(event.at);
   if(event.type.equals("volatility"))SignalChecks.deliverVolatility(this,this::enabled,event.coin,event.change,event.id,event.title,body,event.at);
   else SignalChecks.deliver(this,this::enabled,event.id,event.title,body,event.at,event.type);}

  if(enabled()){AlertJob.prefs(this).edit().putLong("pushFeedRead",System.currentTimeMillis()).apply();state("云端有效行情 "+feed.coverage+"/6 · 等待新的闭合与波动");}
 }

 @Override public void onTaskRemoved(Intent rootIntent){AlertJob.prefs(this).edit().putLong("pushTaskRemoved",System.currentTimeMillis()).apply();super.onTaskRemoved(rootIntent);}
 @Override public void onDestroy(){running=false;main.removeCallbacksAndMessages(null);if(listener!=null)listener.cancel(true);if(pull!=null)pull.cancel(true);HttpURLConnection c=connection;if(c!=null)new Thread(c::disconnect,"guanchao-stream-close").start();stream.shutdownNow();fetch.shutdownNow();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
 @Override public IBinder onBind(Intent intent){return null;}
}
