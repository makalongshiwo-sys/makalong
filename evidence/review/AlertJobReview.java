import android.content.Context;
import android.app.job.JobInfo;
import com.tide.journal.data.Repository;
import com.tide.journal.sync.AlertJob;
import java.lang.reflect.*;

/** Runs the actual AlertJob source against explicit JVM boundary substitutes. */
public final class AlertJobReview {
 private static int checks;
 private static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 private static void runCheck(AlertJob job)throws Exception{
  Class<?> run=Class.forName("com.tide.journal.sync.AlertJob$Run");
  Constructor<?> constructor=run.getDeclaredConstructor();constructor.setAccessible(true);
  Method method=AlertJob.class.getDeclaredMethod("check",run);method.setAccessible(true);
  method.invoke(job,constructor.newInstance());
 }
 public static void main(String[] args)throws Exception{
  Context c=new Context();c.settings.values.put("alerts",true);
  JobInfo periodic=new JobInfo(610),once=new JobInfo(611);
  c.scheduler.jobs.put(610,periodic);c.scheduler.jobs.put(611,once);
  AlertJob.schedule(c);
  check(c.scheduler.jobs.get(610)==periodic,"existing periodic job must be preserved");
  check(c.scheduler.jobs.get(611)==once,"existing one-shot must be preserved");
  check(c.scheduler.scheduled==0,"existing periodic job must not restart");
  check(!c.scheduler.cancelled.contains(610)&&!c.scheduler.cancelled.contains(611),"enabled path must not cancel native jobs");
  check(c.scheduler.cancelled.contains(510)&&c.scheduler.cancelled.contains(511),"legacy jobs removed");
  AlertJob.schedule(c);
  check(c.scheduler.scheduled==0&&c.scheduler.jobs.get(611)==once,"repeated Activity creation is idempotent");
  c.scheduler.jobs.remove(610);AlertJob.schedule(c);
  check(c.scheduler.jobs.containsKey(610)&&c.scheduler.scheduled==1,"missing periodic job is restored");
  check(c.scheduler.jobs.get(611)==once,"periodic restoration preserves one-shot");
  c.settings.values.put("alerts",false);AlertJob.schedule(c);
  check(!c.scheduler.jobs.containsKey(610)&&!c.scheduler.jobs.containsKey(611),"explicit disable cancels both jobs");
  check(!AlertJob.once(c),"disabled alerts cannot request one-shot");
  c.settings.values.put("alerts",true);c.scheduler.result=0;AlertJob.schedule(c);
  check(c.settings.values.get("lastResult").toString().contains("系统未接受"),"schedule rejection is recorded");

  AlertJob job=new AlertJob();job.settings.values.put("alerts",true);
  Repository.instance.packetState="本机缓存";Repository.instance.packetIssue="来源 HTTP 503";
  runCheck(job);String result=job.settings.values.get("lastResult").toString();
  check(result.contains("行情有效覆盖 6/6"),"valid market coverage remains explicit");
  check(result.contains("研究云端读取失败：来源 HTTP 503"),"cached fallback retains report failure");
  check(job.settings.values.containsKey("lastSuccess"),"market-only success retains its timestamp");
  check(Repository.instance.store.added==0,"cached reports do not generate fresh notices");
  Repository.instance.packetState="随包历史资料";Repository.instance.packetIssue="来源 HTTP 404";
  runCheck(job);check(job.settings.values.get("lastResult").toString().contains("研究云端读取失败：来源 HTTP 404"),"bundled fallback retains report failure");
  Repository.instance.packetState="云端已读取";Repository.instance.packetIssue="";
  runCheck(job);check(job.settings.values.get("lastResult").equals("行情有效覆盖 6/6"),"successful report read has no stale failure message");
  int before=Repository.instance.documentCalls;job.settings.values.put("type_report",false);runCheck(job);
  check(Repository.instance.documentCalls==before,"disabled report subscription makes no report request");
  System.out.println("PASS "+checks+" assertions: actual AlertJob schedule preservation, explicit cancellation, scheduler rejection, cached/bundled report-failure logging, successful recovery and disabled subscription.");
  System.out.println("BOUNDARY: JVM substitutes for Android, networking and SQLite; no scheduler timing, real-device, GPU or delivery claim.");
 }
}
