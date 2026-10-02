package com.tide.journal.sync;
import android.content.*;
/** BOOT_COMPLETED only, and only for a previously user-enabled subscription. */
public final class PushBoot extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if(!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()))return;AlertJob.schedule(c);if(AlertJob.prefs(c).getBoolean("pushWanted",false)&&AlertJob.prefs(c).getBoolean("alerts",false)&&AlertJob.allowed(c))try{PushService.start(c);}catch(RuntimeException e){AlertJob.prefs(c).edit().putString("pushState","开机恢复受系统限制，请打开应用恢复连接").apply();}}
}
