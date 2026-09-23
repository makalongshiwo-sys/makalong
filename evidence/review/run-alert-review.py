"""Compile current product AlertJob/Market; substitute only their external boundaries.

Run from any directory: python3 evidence/review/run-alert-review.py
Requires a JDK; no Android SDK or network. Generated classes stay in a temp directory.
This test cannot prove Android scheduling, actual HTTP behavior or notification delivery.
"""
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
stubs = {
"android/content/SharedPreferences.java": '''package android.content; public interface SharedPreferences {boolean getBoolean(String k,boolean d);long getLong(String k,long d);Editor edit();interface Editor{Editor putString(String k,String v);Editor putLong(String k,long v);void apply();}}''',
"android/content/Context.java": '''package android.content;import java.util.*;import android.app.*;import android.app.job.*;public class Context{public final Prefs settings=new Prefs();public final JobScheduler scheduler=new JobScheduler();public final NotificationManager notifications=new NotificationManager();public SharedPreferences getSharedPreferences(String n,int mode){return settings;}public <T>T getSystemService(Class<T>c){return c.cast(c==JobScheduler.class?scheduler:notifications);}public int checkSelfPermission(String s){return 0;}public static class Prefs implements SharedPreferences,SharedPreferences.Editor{public final Map<String,Object>values=new HashMap<>();public boolean getBoolean(String k,boolean d){return(Boolean)values.getOrDefault(k,d);}public long getLong(String k,long d){return((Number)values.getOrDefault(k,d)).longValue();}public Editor edit(){return this;}public Editor putString(String k,String v){values.put(k,v);return this;}public Editor putLong(String k,long v){values.put(k,v);return this;}public void apply(){}}}''',
"android/content/ComponentName.java": '''package android.content;public class ComponentName{public ComponentName(Context c,Class<?>s){}}''',
"android/content/Intent.java": '''package android.content;public class Intent{public static final int FLAG_ACTIVITY_NEW_TASK=1,FLAG_ACTIVITY_CLEAR_TOP=2;public Intent(Context c,Class<?>s){}public Intent putExtra(String k,int v){return this;}public Intent setFlags(int f){return this;}}''',
"android/content/pm/PackageManager.java": '''package android.content.pm;public class PackageManager{public static final int PERMISSION_GRANTED=0;}''',
"android/os/Build.java": '''package android.os;public class Build{public static class VERSION{public static int SDK_INT=36;}}''',
"android/app/job/JobInfo.java": '''package android.app.job;import android.content.ComponentName;public class JobInfo{public static final int NETWORK_TYPE_ANY=1;public final int id;public JobInfo(int i){id=i;}public static class Builder{int id;public Builder(int i,ComponentName c){id=i;}public Builder setRequiredNetworkType(int n){return this;}public Builder setPersisted(boolean b){return this;}public Builder setPeriodic(long n){return this;}public Builder setMinimumLatency(long n){return this;}public JobInfo build(){return new JobInfo(id);}}}''',
"android/app/job/JobScheduler.java": '''package android.app.job;import java.util.*;public class JobScheduler{public static final int RESULT_SUCCESS=1;public int result=1,scheduled;public final Map<Integer,JobInfo>jobs=new HashMap<>();public final Set<Integer>cancelled=new HashSet<>();public void cancel(int id){cancelled.add(id);jobs.remove(id);}public JobInfo getPendingJob(int id){return jobs.get(id);}public int schedule(JobInfo j){scheduled++;if(result==1)jobs.put(j.id,j);return result;}}''',
"android/app/job/JobParameters.java": '''package android.app.job;public class JobParameters{int id;public JobParameters(int i){id=i;}public int getJobId(){return id;}}''',
"android/app/job/JobService.java": '''package android.app.job;public class JobService extends android.content.Context{public boolean onStartJob(JobParameters p){return false;}public boolean onStopJob(JobParameters p){return false;}public void jobFinished(JobParameters p,boolean again){}}''',
"android/app/NotificationManager.java": '''package android.app;public class NotificationManager{public static final int IMPORTANCE_DEFAULT=3;public void createNotificationChannel(NotificationChannel c){}public boolean areNotificationsEnabled(){return true;}public void notify(String tag,int id,Notification n){}}''',
"android/app/NotificationChannel.java": '''package android.app;public class NotificationChannel{public NotificationChannel(String id,String name,int n){}}''',
"android/app/PendingIntent.java": '''package android.app;import android.content.*;public class PendingIntent{public static final int FLAG_UPDATE_CURRENT=1,FLAG_IMMUTABLE=2;public static PendingIntent getActivity(Context c,int id,Intent i,int flags){return new PendingIntent();}}''',
"android/app/Notification.java": '''package android.app;import android.content.Context;public class Notification{public static class BigTextStyle{public BigTextStyle bigText(String s){return this;}}public static class Builder{public Builder(Context c,String id){}public Builder setSmallIcon(int i){return this;}public Builder setContentTitle(String s){return this;}public Builder setContentText(String s){return this;}public Builder setStyle(BigTextStyle s){return this;}public Builder setContentIntent(PendingIntent p){return this;}public Builder setAutoCancel(boolean b){return this;}public Notification build(){return new Notification();}}}''',
"org/json/JSONObject.java": '''package org.json;public class JSONObject{public JSONArray getJSONArray(String s){return new JSONArray();}public String getString(String s){return "";}public String optString(String s){return "";}}''',
"org/json/JSONArray.java": '''package org.json;public class JSONArray{public int length(){return 0;}public JSONObject getJSONObject(int i){throw new IndexOutOfBoundsException();}}''',
"com/tide/journal/MainActivity.java": '''package com.tide.journal;public class MainActivity{}''',
"com/tide/journal/R.java": '''package com.tide.journal;public class R{public static class drawable{public static int notification=1;}}''',
"com/tide/journal/data/Store.java": '''package com.tide.journal.data;public class Store{public int added;public void add(String id,String title,String body,long at){added++;}public boolean sent(String id){return false;}public void mark(String id){}}''',
"com/tide/journal/data/Repository.java": '''package com.tide.journal.data;import android.content.Context;import com.tide.journal.domain.Market;import org.json.JSONObject;public class Repository{public static final Repository instance=new Repository();public String packetState="云端已读取",packetIssue="";public int documentCalls;public final Store store=new Store();public static Repository get(Context c){return instance;}public static class Bars{public Market.Analysis analysis;}public static class Packet{public String state,issue;public JSONObject data=new JSONObject();}public Bars bars(String coin,String period){Bars b=new Bars();b.analysis=new Market.Analysis();b.analysis.quality="valid";b.analysis.closedAt=System.currentTimeMillis()-60000;return b;}public Packet document(String kind){documentCalls++;Packet p=new Packet();p.state=packetState;p.issue=packetIssue;return p;}}''',
}

with tempfile.TemporaryDirectory(prefix="tide-independent-alert-") as directory:
    temp = Path(directory)
    sources = []
    for name, content in stubs.items():
        path = temp / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        sources.append(path)
    sources += [root / "app/src/main/java/com/tide/journal/sync/AlertJob.java",
                root / "app/src/main/java/com/tide/journal/domain/Market.java",
                root / "evidence/review/AlertJobReview.java"]
    output = temp / "classes"
    output.mkdir()
    subprocess.run(["java", "com.sun.tools.javac.Main", "-encoding", "UTF-8", "-d", str(output),
                    *map(str, sources)], check=True)
    subprocess.run(["java", "-ea", "-cp", str(output), "AlertJobReview"], check=True)
