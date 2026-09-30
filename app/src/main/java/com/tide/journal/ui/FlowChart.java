package com.tide.journal.ui;
import android.content.Context;import android.graphics.*;import android.view.*;import java.util.*;
/** Native daily disclosure bars. Missing totals never turn into zero-height bars. */
public final class FlowChart extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private List<String>dates=Collections.emptyList();private List<Double>totals=Collections.emptyList();private int selected=-1;
 public FlowChart(Context c){super(c);setFocusable(true);setContentDescription("ETF 每日净流柱状图；下方列表提供完整数字");}
 public void data(List<String>d,List<Double>v){dates=new ArrayList<>(d);totals=new ArrayList<>(v);selected=-1;invalidate();}
 private float dp(float n){return Ui.dp(getContext(),n);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float left=dp(8),right=getWidth()-dp(8),top=dp(30),bottom=getHeight()-dp(28),zero=(top+bottom)/2;
  paint.setTextSize(dp(11));paint.setColor(Ui.MUTED);paint.setTextAlign(Paint.Align.LEFT);
  if(dates.isEmpty()){c.drawText("暂无所选日期的披露",left,zero,paint);return;}
  double scale=1;for(Double n:totals)if(n!=null&&Double.isFinite(n))scale=Math.max(scale,Math.abs(n));float span=(bottom-top)/2,step=(right-left)/dates.size();
  paint.setColor(Ui.LINE);paint.setStrokeWidth(dp(1));c.drawLine(left,zero,right,zero,paint);
  for(int i=0;i<totals.size();i++){Double n=totals.get(i);float x=left+(i+.5f)*step;
   if(n==null||!Double.isFinite(n)){paint.setColor(Ui.MUTED);c.drawLine(x-dp(1),zero-dp(2),x+dp(1),zero+dp(2),paint);continue;}
   paint.setColor(n>=0?Ui.GREEN:Ui.RED);float y=zero-(float)(n/scale)*span,w=Math.max(.5f,step*.65f);
   if(n==0)c.drawCircle(x,zero,dp(1.2f),paint);else c.drawRect(x-w/2,Math.min(y,zero),x+w/2,Math.max(y,zero),paint);
  }
  paint.setColor(Ui.MUTED);c.drawText(dates.get(0),left,getHeight()-dp(7),paint);paint.setTextAlign(Paint.Align.RIGHT);c.drawText(dates.get(dates.size()-1),right,getHeight()-dp(7),paint);paint.setTextAlign(Paint.Align.LEFT);
  String label="每日净流 · 百万美元 · 点选查看";
  if(selected>=0&&selected<dates.size()){Double n=totals.get(selected);label=dates.get(selected)+"  "+(n==null?"未披露":String.format(Locale.US,"%+,.1f",n));}
  c.drawText(label,left,dp(17),paint);
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  if(e.getAction()==MotionEvent.ACTION_UP&&!dates.isEmpty()){selected=Math.max(0,Math.min(dates.size()-1,(int)((e.getX()-dp(8))/Math.max(1,getWidth()-dp(16))*dates.size())));performClick();Double n=totals.get(selected);announceForAccessibility(dates.get(selected)+" "+(n==null?"未披露":n+" 百万美元"));invalidate();return true;}
  return e.getAction()==MotionEvent.ACTION_DOWN||super.onTouchEvent(e);
 }
 @Override public boolean performClick(){super.performClick();return true;}
}
