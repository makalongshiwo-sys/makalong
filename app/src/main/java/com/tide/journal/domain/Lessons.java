package com.tide.journal.domain;
public final class Lessons {
 public static final String[] IDS={"rates","cuts","inflation","supply","jobs","etf","expectations","oi","leverage"};
 public static final class Result {public final String[] labels;public final double[] values;public final double scale;public final String unit,formula;Result(String[]l,double[]v,double s,String u,String f){labels=l;values=v;scale=s;unit=u;formula=f;}}
 public static Result calculate(String id,double v){if(!Double.isFinite(v))throw new IllegalArgumentException("参数无效");switch(id){
 case "rates":case "cuts":range(v,id.equals("rates")?3:1,id.equals("rates")?10:7);double old=id.equals("rates")?300:700,next=v*100;return r("原年利息,新年利息,利息差额",new double[]{old,next,next-old},1000,"元 / 年","10,000 × 年利率；本金全年不变，单利");
 case "inflation":range(v,0,50);double p=5*(1+v/100),whole=Math.floor(100/p);return r("原可买整件,现可买整件,少买整件",new double[]{20,whole,20-whole},20,"件","100 ÷ 现价 "+f(p)+" = "+f(100/p)+"；整件向下取整");
 case "supply":range(v,0,30);integer(v);return r("需求,库存,"+(v<20?"缺货":"余货"),new double[]{20,v,Math.abs(20-v)},30,"件","固定价格比较数量；不能推导涨价幅度");
 case "jobs":range(v,0,10);integer(v);return r("就业,失业,退出劳动力",new double[]{90,10-v,v},100,"人","失业率 = "+f(10-v)+" ÷ "+f(100-v)+" = "+f((10-v)/(100-v)*100)+"%");
 case "etf":range(v,0,500);return r("申购,赎回减项,净流量",new double[]{v,-200,v-200},500,"百万美元","净流量 = 申购 − 赎回；不是成交额和 AUM 变化");
 case "expectations":range(v,0,50);return r("预期变化,实际变化,预期差",new double[]{-v,-25,v-25},50,"bp","实际 − 预期；正差相对更紧，负差相对更松");
 case "oi":range(v,-50,100);return r("原生OI指数,价格指数,美元OI指数",new double[]{100,100+v,100+v},200,"基准100","数量不变，价格也会改变美元OI；不识别多空");
 case "leverage":range(v,1,5);return r("初始权益,理论损失,理论剩余",new double[]{100,v*10,100-v*10},100,"假设单位","固定下跌10%；损失=有效杠杆×跌幅，不计算强平价");default:throw new IllegalArgumentException("课程不存在");}}
 private static Result r(String l,double[]v,double s,String u,String f){return new Result(l.split(","),v,s,u,f);}private static void range(double v,double lo,double hi){if(v<lo||v>hi)throw new IllegalArgumentException("教学范围外");}private static void integer(double v){if(v!=Math.rint(v))throw new IllegalArgumentException("人数/件数需整数");}private static String f(double v){return String.format(java.util.Locale.US,"%.2f",v);}
 public static double spring(double x,double goal,double speed,double dt){return speed+(95*(goal-x)-19*speed)*Math.min(.033,Math.max(0,dt));}
}
