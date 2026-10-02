package com.tide.journal.domain;
import java.util.ArrayDeque;

/** Fresh quotes only, up to five minutes, one alert per five minutes for each window. */
public final class PriceWindow {
 public static final class Move {public final double before,price,percent;public final long at,seconds;private Move(double before,double price,long at,long span){this.before=before;this.price=price;this.percent=(price/before-1)*100;this.at=at;this.seconds=span/1000;}}
 private static final class Sample {final double price;final long at;Sample(double p,long t){price=p;at=t;}}
 private final ArrayDeque<Sample> samples=new ArrayDeque<>();private long lastAlert;
 public PriceWindow(){this(0);}
 public PriceWindow(long previousAlert){lastAlert=Math.max(0,previousAlert);}
 public void markAlert(long at){lastAlert=Math.max(lastAlert,at);}
 public Move observe(double price,long at,long now,double threshold){
  if(!Double.isFinite(price)||price<=0||at<=0||Math.abs(at-now)>90000||!Double.isFinite(threshold)||threshold<=0)return null;
  if(!samples.isEmpty()&&at<=samples.peekLast().at)return null;
  if(!samples.isEmpty()&&at-samples.peekLast().at>90000)samples.clear();
  while(!samples.isEmpty()&&at-samples.peekFirst().at>300000)samples.removeFirst();
  Move move=null;
  if(!samples.isEmpty()){Sample base=samples.peekFirst();double percent=(price/base.price-1)*100;if(at-base.at>=15000&&Math.abs(percent)>=threshold&&(lastAlert==0||at-lastAlert>=300000)){move=new Move(base.price,price,at,at-base.at);}}
  samples.addLast(new Sample(price,at));return move;
 }
}
