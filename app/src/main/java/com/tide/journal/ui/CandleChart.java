package com.tide.journal.ui;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import com.tide.journal.domain.Market;
import java.util.*;

/** Canvas plots share one candle cursor; vertical scrolling belongs to the parent. */
public final class CandleChart extends View {
 public interface Selection { void select(Market.Bar bar, Market.Point point); }
 private List<Market.Bar> bars=Collections.emptyList(); private List<Market.Point> points=Collections.emptyList();
 private final Paint p=new Paint(3); private final Selection selection;
 private int count=55,offset,selected=-1; private boolean bb=true,userSelected,horizontal;
 private float startX,startY,lastX; private final ScaleGestureDetector scale;
 public CandleChart(Context c,Selection s){super(c);selection=s;setMinimumHeight(Ui.dp(c,420));setContentDescription("K线、成交量、MACD、RSI；双指缩放，左右拖动，点击查看数值");
  scale=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoom(1/d.getScaleFactor());return true;}});
 }
 public void data(List<Market.Bar>b,List<Market.Point>v){long id=selected>=0&&selected<bars.size()?bars.get(selected).openAt:-1;bars=b;points=v;selected=-1;if(userSelected)for(int i=0;i<b.size();i++)if(b.get(i).openAt==id)selected=i;
  if(selected<0){userSelected=false;for(int i=b.size()-1;i>=0;i--)if(b.get(i).closed){selected=i;break;}}offset=Math.min(offset,Math.max(0,b.size()-count));reading();invalidate();}
 public void bands(boolean v){bb=v;invalidate();} public void latest(){offset=0;userSelected=false;selected=-1;for(int i=bars.size()-1;i>=0;i--)if(bars.get(i).closed){selected=i;break;}reading();invalidate();}
 public void zoom(float factor){count=Math.max(18,Math.min(160,Math.round(count*factor)));offset=Math.min(offset,Math.max(0,bars.size()-count));invalidate();}
 private void reading(){if(selected>=0&&selected<points.size())selection.select(bars.get(selected),points.get(selected));}
 private float left(){return Ui.dp(getContext(),4);}private float right(){return getWidth()-Ui.dp(getContext(),49);}private int end(){return Math.max(0,bars.size()-offset);}private int begin(){return Math.max(0,end()-count);}
 private void line(Canvas c,float x,float y,float x2,float y2,int color,float width){p.setColor(color);p.setStrokeWidth(width);c.drawLine(x,y,x2,y2,p);}
 private void label(Canvas c,String s,float x,float y,int color){p.setColor(color);p.setTextSize(Ui.dp(getContext(),10));c.drawText(s,x,y,p);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float w=right()-left(),h=getHeight(),top=Ui.dp(getContext(),24),bottom=h*.47f;
  if(bars.isEmpty()){label(c,"等待来源 K 线，未使用演示价格",left(),h/2,Ui.MUTED);return;}
  int begin=begin(),end=end(),n=end-begin;if(n<=0)return;float step=w/n;double low=Double.POSITIVE_INFINITY,high=0,vol=1,mac=.000001;
  for(int i=begin;i<end;i++){Market.Bar b=bars.get(i);Market.Point q=points.get(i);low=Math.min(low,b.low);high=Math.max(high,b.high);vol=Math.max(vol,b.volume);if(bb&&Double.isFinite(q.lower)){low=Math.min(low,q.lower);high=Math.max(high,q.upper);}if(Double.isFinite(q.hist))mac=Math.max(mac,Math.max(Math.abs(q.hist),Math.max(Math.abs(q.dif),Math.abs(q.dea))));}
  double pad=Math.max((high-low)*.08,high*.0001);low-=pad;high+=pad;final double lo=low,hi=high;
  for(int k=0;k<5;k++){float y=top+(bottom-top)*k/4;line(c,left(),y,right(),y,Ui.LINE,1);label(c,String.format(Locale.US,"%.1f",high-(high-low)*k/4),right()+3,y+3,Ui.MUTED);}
  label(c,"价格 · USDT",left(),Ui.dp(getContext(),16),Ui.MUTED);label(c,"VOL",left(),h*.52f,Ui.MUTED);label(c,"MACD 12 · 26 · 9",left(),h*.66f,Ui.MUTED);label(c,"RSI 14",left(),h*.84f,Ui.MUTED);
  float vtop=h*.54f,vbottom=h*.62f,mtop=h*.68f,mbottom=h*.80f,mzero=(mtop+mbottom)/2,rtop=h*.86f,rbottom=h-Ui.dp(getContext(),24);
  line(c,left(),mzero,right(),mzero,Ui.LINE,1);for(int level:new int[]{30,70}){float y=rbottom-(rbottom-rtop)*level/100;line(c,left(),y,right(),y,Ui.LINE,1);label(c,""+level,right()+3,y+3,Ui.MUTED);}
  Path mid=new Path(),upper=new Path(),lower=new Path(),dif=new Path(),dea=new Path(),rsi=new Path();boolean pb=false,pm=false,pr=false;
  for(int i=begin;i<end;i++){Market.Bar b=bars.get(i);Market.Point q=points.get(i);float x=left()+(i-begin+.5f)*step;int color=b.close>=b.open?Ui.GREEN:Ui.RED;
   p.setColor(color);p.setAlpha(b.closed?255:95);p.setStrokeWidth(Math.max(1,step*.1f));c.drawLine(x,y(b.high,lo,hi,top,bottom),x,y(b.low,lo,hi,top,bottom),p);
   float a=y(b.open,lo,hi,top,bottom),z=y(b.close,lo,hi,top,bottom);c.drawRect(x-step*.28f,Math.min(a,z),x+step*.28f,Math.max(Math.min(a,z)+1,Math.max(a,z)),p);
   c.drawRect(x-step*.3f,(float)(vbottom-b.volume/vol*(vbottom-vtop)),x+step*.3f,vbottom,p);p.setAlpha(255);
   if(Double.isFinite(q.middle)){point(mid,x,y(q.middle,lo,hi,top,bottom),pb);point(upper,x,y(q.upper,lo,hi,top,bottom),pb);point(lower,x,y(q.lower,lo,hi,top,bottom),pb);pb=true;}else pb=false;
   if(Double.isFinite(q.hist)){float my=(float)(mzero-q.hist/mac*(mbottom-mtop)/2);p.setColor(q.hist>=0?Ui.GREEN:Ui.RED);c.drawRect(x-step*.3f,Math.min(mzero,my),x+step*.3f,Math.max(mzero+1,my),p);point(dif,x,(float)(mzero-q.dif/mac*(mbottom-mtop)/2),pm);point(dea,x,(float)(mzero-q.dea/mac*(mbottom-mtop)/2),pm);pm=true;}else pm=false;
   if(Double.isFinite(q.rsi)){point(rsi,x,rbottom-(float)q.rsi/100*(rbottom-rtop),pr);pr=true;}else pr=false;
  }
  if(bb){path(c,mid,0xffba955c);path(c,upper,Ui.BLUE);path(c,lower,Ui.BLUE);}path(c,dif,0xffd1915b);path(c,dea,Ui.BLUE);path(c,rsi,0xff9a83b4);
  if(selected>=begin&&selected<end){float x=left()+(selected-begin+.5f)*step;line(c,x,top,x,rbottom,0x77637770,1);float y=y(bars.get(selected).close,lo,hi,top,bottom);line(c,left(),y,right(),y,0x55637770,1);p.setColor(Ui.INK);c.drawCircle(x,y,3,p);}
  label(c,Ui.time(bars.get(begin).openAt),left(),h-Ui.dp(getContext(),4),Ui.MUTED);String time=Ui.time(bars.get(end-1).openAt);p.setTextAlign(Paint.Align.RIGHT);label(c,time,right(),h-Ui.dp(getContext(),4),Ui.MUTED);p.setTextAlign(Paint.Align.LEFT);
 }
 private static float y(double v,double lo,double hi,float top,float bottom){return (float)(bottom-(v-lo)/(hi-lo)*(bottom-top));}private static void point(Path p,float x,float y,boolean prev){if(prev)p.lineTo(x,y);else p.moveTo(x,y);}
 private void path(Canvas c,Path path,int color){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Ui.dp(getContext(),1));c.drawPath(path,p);p.setStyle(Paint.Style.FILL);}
 @Override public boolean onTouchEvent(android.view.MotionEvent e){scale.onTouchEvent(e);float x=e.getX(),yy=e.getY();switch(e.getActionMasked()){
  case MotionEvent.ACTION_DOWN:startX=lastX=x;startY=yy;horizontal=false;return true;
  case MotionEvent.ACTION_POINTER_DOWN:getParent().requestDisallowInterceptTouchEvent(true);return true;
  case MotionEvent.ACTION_MOVE:if(scale.isInProgress())return true;if(!horizontal&&Math.abs(x-startX)>Ui.dp(getContext(),8)&&Math.abs(x-startX)>Math.abs(yy-startY)){horizontal=true;getParent().requestDisallowInterceptTouchEvent(true);}if(horizontal){float step=(right()-left())/Math.min(count,Math.max(1,bars.size()));int delta=(int)((x-lastX)/step);if(delta!=0){offset=Math.max(0,Math.min(Math.max(0,bars.size()-count),offset+delta));lastX=x;invalidate();}}return true;
  case MotionEvent.ACTION_UP:if(!horizontal&&Math.hypot(x-startX,yy-startY)<Ui.dp(getContext(),10)&&!bars.isEmpty()){int n=end()-begin();selected=Math.max(begin(),Math.min(end()-1,begin()+(int)((x-left())/(right()-left())*n)));userSelected=true;reading();invalidate();performClick();}getParent().requestDisallowInterceptTouchEvent(false);return true;
  case MotionEvent.ACTION_CANCEL:getParent().requestDisallowInterceptTouchEvent(false);return true;
 }return true;}
 @Override public boolean performClick(){super.performClick();return true;}
}
