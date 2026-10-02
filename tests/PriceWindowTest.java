import com.tide.journal.domain.PriceWindow;
public final class PriceWindowTest {
 private static int checks;private static void check(boolean ok){if(!ok)throw new AssertionError("PriceWindow check "+checks);checks++;}
 public static void main(String[] args){
  long t=1780000000000L;PriceWindow w=new PriceWindow();check(w.observe(100,t,t,1)==null);check(w.observe(102,t+10000,t+10000,1)==null);
  PriceWindow.Move up=w.observe(102,t+15000,t+15000,1);check(up!=null&&Math.abs(up.percent-2)<1e-8&&up.seconds==15);w.markAlert(up.at);
  check(w.observe(103,t+30000,t+30000,1)==null);check(w.observe(103,t+30000,t+30000,1)==null);check(w.observe(103,t+20000,t+40000,1)==null);
  check(w.observe(Double.NaN,t+45000,t+45000,1)==null);check(w.observe(0,t+45000,t+45000,1)==null);check(w.observe(110,t+45000,t+150000,1)==null);check(w.observe(110,t+45000,t+45000,0)==null);
  for(long dt=60000;dt<=300000;dt+=30000)check(w.observe(100,t+dt,t+dt,1)==null);
  PriceWindow.Move down=w.observe(96,t+315000,t+315000,1);check(down!=null&&down.percent< -1);
  PriceWindow gap=new PriceWindow();check(gap.observe(100,t,t,1)==null);check(gap.observe(110,t+120000,t+120000,1)==null);check(gap.observe(112,t+135000,t+135000,1)!=null);
  PriceWindow threshold=new PriceWindow();check(threshold.observe(100,t,t,.5)==null);check(threshold.observe(100.6,t+15000,t+15000,.5)!=null);
  PriceWindow restart=new PriceWindow(t);check(restart.observe(100,t+10000,t+10000,1)==null);check(restart.observe(103,t+30000,t+30000,1)==null);
  PriceWindow rejected=new PriceWindow();check(rejected.observe(100,t,t,1)==null);check(rejected.observe(102,t+15000,t+15000,1)!=null);check(rejected.observe(102,t+30000,t+30000,1)!=null);rejected.markAlert(t+30000);check(rejected.observe(104,t+45000,t+45000,1)==null);
  System.out.println("PASS: live volatility fresh quotes, thresholds, gaps, ordering and cooldown, "+checks+" assertions");
 }
}
