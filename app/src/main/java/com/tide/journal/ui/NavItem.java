package com.tide.journal.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.RippleDrawable;
import android.view.*;
import android.widget.*;

/** Equal-width native tab with a drawn icon, readable label and a single touch target. */
public final class NavItem extends LinearLayout {
 public NavItem(Context c,int tab,String title,boolean selected,Runnable click){
  super(c);setOrientation(VERTICAL);setGravity(Gravity.CENTER);setMinimumHeight(Ui.dp(c,64));
  setPadding(Ui.dp(c,2),Ui.dp(c,6),Ui.dp(c,2),Ui.dp(c,6));
  setBackground(new RippleDrawable(ColorStateList.valueOf(Ui.isDark()?0x339bbcff:0x22235ed5),Ui.shape(selected?Ui.TINT:Ui.SURFACE,16,c),null));
  addView(new Icon(c,tab,selected?Ui.BLUE:Ui.MUTED),new LayoutParams(Ui.dp(c,24),Ui.dp(c,24)));
  Ui.gap(this,6);TextView label=Ui.text(c,title,12,selected?Ui.BLUE:Ui.MUTED);
  label.setGravity(Gravity.CENTER);label.setSingleLine(true);label.setTypeface(Typeface.create(selected?"sans-serif-medium":"sans-serif",Typeface.NORMAL));addView(label);
  setSelected(selected);setFocusable(true);setClickable(true);setContentDescription(title);
  setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);click.run();});
 }
 private static final class Icon extends View {
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int tab,color;
  Icon(Context c,int tab,int color){super(c);this.tab=tab;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  @Override protected void onDraw(Canvas canvas){
   super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/24f,getHeight()/24f);
   p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
   Path path=new Path();
   switch(tab){
    case 0:canvas.drawLine(4,4,4,20,p);canvas.drawLine(4,20,21,20,p);path.moveTo(7,15);path.lineTo(11,10);path.lineTo(15,13);path.lineTo(21,6);canvas.drawPath(path,p);break;
    case 1:canvas.drawRoundRect(5,3,19,21,2,2,p);canvas.drawLine(8,8,16,8,p);canvas.drawLine(8,12,16,12,p);canvas.drawLine(8,16,13,16,p);break;
    case 2:path.moveTo(12,6);path.cubicTo(8,3,4,4,3,5);path.lineTo(3,19);path.cubicTo(6,17,9,18,12,20);path.cubicTo(15,18,18,17,21,19);path.lineTo(21,5);path.cubicTo(18,4,15,3,12,6);path.close();canvas.drawPath(path,p);canvas.drawLine(12,6,12,20,p);break;
    case 3:canvas.drawLine(3,20,21,20,p);canvas.drawRoundRect(5,12,8,20,1,1,p);canvas.drawRoundRect(11,7,14,20,1,1,p);canvas.drawRoundRect(17,3,20,20,1,1,p);break;
    default:path.moveTo(5,17);path.lineTo(7,14);path.lineTo(7,9);path.cubicTo(7,3,17,3,17,9);path.lineTo(17,14);path.lineTo(19,17);path.close();canvas.drawPath(path,p);canvas.drawArc(10,18,14,22,0,180,false,p);canvas.drawLine(12,2,12,4,p);
   }
   canvas.restore();
  }
 }
}
