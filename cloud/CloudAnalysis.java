import com.tide.journal.domain.Market;
import java.io.*;import java.util.*;
/** Same closed-candle domain as Android; TSV in, bounded JSON events out. */
public final class CloudAnalysis {
 static String q(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";}
 public static void main(String[] args)throws Exception{String coin=args[0],period=args[1];long at=Long.parseLong(args[2]);List<Market.Bar>b=new ArrayList<>();try(BufferedReader r=new BufferedReader(new InputStreamReader(System.in,"UTF-8"))){String line;while((line=r.readLine())!=null){String[]v=line.split("\t");long end=Long.parseLong(v[6]);b.add(new Market.Bar(Long.parseLong(v[0]),end,Double.parseDouble(v[1]),Double.parseDouble(v[2]),Double.parseDouble(v[3]),Double.parseDouble(v[4]),Double.parseDouble(v[5]),end<at));}}
 Market.Analysis a=Market.analyze(b,period,at);if(!a.valid()){System.out.print("[]");return;}List<String>events=new ArrayList<>();for(Market.Event e:a.events)events.add(event(coin+":"+e.id,coin+" "+period+" · "+e.title,e.detail,e.at,e.type));events.add(event(coin+":"+period+":"+a.closedAt+":close",coin+" "+period+" 已闭合",a.trend+"；"+a.structure,a.closedAt,"close"));System.out.print("["+String.join(",",events)+"]");}
 static String event(String id,String title,String body,long at,String type){return "{\"id\":"+q(id)+",\"title\":"+q(title)+",\"body\":"+q(body)+",\"at\":"+at+",\"type\":"+q(type)+"}";}
}
