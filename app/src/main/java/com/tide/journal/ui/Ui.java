package com.tide.journal.ui;

import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Shared native styles. Colors follow the system before each Activity is inflated. */
public final class Ui {
 public static int BG,SURFACE,SURFACE2,INK,MUTED,GREEN,RED,LINE,BLUE,TINT,ON_ACCENT;
 private static boolean dark;
 static {palette(false);}
 private static void palette(boolean night){
  dark=night;
  BG=night?0xff101f23:0xfff3f6f4;
  SURFACE=night?0xff182b30:Color.WHITE;
  SURFACE2=night?0xff21383d:0xffedf2ef;
  INK=night?0xffedf6f3:0xff17312f;
  MUTED=night?0xffa6beb9:0xff5c726d;
  LINE=night?0xff2c454a:0xffdfe8e2;
  BLUE=night?0xff81dfce:0xff086859;
  TINT=night?0xff234a47:0xffe2f1ea;
  GREEN=night?0xff6cdaa7:0xff16704a;
  RED=night?0xffff929c:0xffc43d4a;
  ON_ACCENT=night?0xff102522:Color.WHITE;
 }
 public static void configure(Context c){palette((c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);}
 public static boolean isDark(){return dark;}
 public static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 public static float sp(Context c,float n){return n*c.getResources().getDisplayMetrics().scaledDensity;}
 public static GradientDrawable shape(int color,float radius,Context c){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
 public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
 public static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 public static TextView text(Context c,String s,float size,int color){
  TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);
  t.setIncludeFontPadding(false);t.setFontFeatureSettings("tnum");t.setLineSpacing(dp(c,4),1);
  return t;
 }
 public static TextView heading(Context c,String s,int size){TextView t=text(c,s,size,INK);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
 public static TextView button(Context c,String s,boolean selected,Runnable click){
  TextView t=text(c,s,14,INK);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(c,48));
  t.setPadding(dp(c,14),dp(c,10),dp(c,14),dp(c,10));selectButton(t,selected);
  t.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);click.run();});
  t.setFocusable(true);t.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(Button.class.getName());}});return t;
 }
 public static void selectButton(TextView t,boolean selected){
  t.setSelected(selected);t.setTextColor(selected?BLUE:INK);
  t.setBackground(new RippleDrawable(ColorStateList.valueOf(isDark()?0x3381dfce:0x22086859),shape(selected?TINT:SURFACE2,12,t.getContext()),null));
 }
 public static TextView primary(Context c,String s,Runnable click){
  TextView t=button(c,s,false,click);t.setTextColor(ON_ACCENT);
  t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000),shape(BLUE,12,c),null));return t;
 }
 public static LinearLayout card(Context c){
  LinearLayout l=column(c);l.setPadding(dp(c,18),dp(c,18),dp(c,18),dp(c,18));
  GradientDrawable background=shape(SURFACE,24,c);background.setStroke(dp(c,1),LINE);l.setBackground(background);
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.bottomMargin=dp(c,16);l.setLayoutParams(params);return l;
 }
 public static void gap(LinearLayout l,int height){l.addView(new Space(l.getContext()),new LinearLayout.LayoutParams(1,dp(l.getContext(),height)));}
 public static void weighted(LinearLayout row,View child){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(row.getContext(),3),0,dp(row.getContext(),3),0);row.addView(child,p);}
 public static String n(double x){return Double.isFinite(x)?String.format(java.util.Locale.US,Math.abs(x)>=1000?"%,.2f":"%.3f",x):"—";}
 public static String quoteTime(long t){return t<=0?"尚无报价":java.time.Instant.ofEpochMilli(t).atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss 'UTC'"));}
 public static String time(long t){return t<=0?"尚无成功记录":java.time.Instant.ofEpochMilli(t).atZone(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm 'UTC'"));}
}
