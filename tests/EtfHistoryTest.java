import com.tide.journal.domain.EtfHistory;
import java.util.*;import java.io.*;
public final class EtfHistoryTest {
 private static int checks;private static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;}
 private static void rejects(String html,String name)throws Exception{try{EtfHistory.parse(html);throw new AssertionError(name);}catch(IOException expected){checks++;}}
 public static void main(String[]args)throws Exception{
  String header="<tr><th>Date</th><th>IBIT</th><th>FBTC</th><th>Total</th></tr>";
  String rows="<tr><td>12 Jan 2024</td><td>10.0</td><td>(2.0)</td><td>8.0</td></tr>"+
   "<tr><td>11 Jan 24</td><td>0.0</td><td>—</td><td>-</td></tr>"+
   "<tr><td>Total</td><td>10</td><td>-2</td><td>8</td></tr>";
  List<EtfHistory.Row> parsed=EtfHistory.parse("<table>"+header+rows+"</table>");
  check(parsed.size()==2,"summary rows excluded");check(parsed.get(0).date.equals("2024-01-11"),"sorted dates and short year");
  check(parsed.get(0).funds.get("IBIT")==0,"reported zero preserved");check(parsed.get(0).funds.get("FBTC")==null,"missing fund stays missing");
  check(parsed.get(0).total==null&&!parsed.get(0).complete(),"missing total not summed");check(parsed.get(1).total==8&&parsed.get(1).complete(),"complete row");check(parsed.get(1).funds.get("FBTC")==-2,"parenthesized outflow");
  check(EtfHistory.select(parsed,"2024-01-12","2024-01-12").size()==1,"inclusive range");
  check(EtfHistory.select(parsed,"2025-01-01","").isEmpty(),"empty range");
  rejects("<table>"+header+rows.replace("8.0","80.0")+"</table>","inconsistent totals rejected");
  rejects("<table>"+header+rows.replace("(2.0)","garbage")+"</table>","invalid numeric value rejected");
  rejects("<table>"+header+rows.replace("<td>8.0</td>","")+"</table>","column drift rejected");
  rejects("<table>"+header+rows+rows+"</table>","duplicate dates rejected");
  rejects("<table>"+header+rows.replace("12 Jan 2024","12 Jan 2099")+"</table>","future date rejected");
  rejects("<html>Service unavailable</html>","error page rejected");
  check(EtfHistory.parse("<table>"+header.replace("<th>Date</th>","<th></th>")+rows+"</table>").size()==2,"blank date header with explicit ticker and total columns");
  String split="<tr><td>&nbsp;</td><td></td><td></td><td>Total</td></tr>";
  check(EtfHistory.parse("<table>"+split+"<tr><td></td><td>IBIT</td><td>FBTC</td><td></td></tr>"+rows+"</table>").size()==2,"two-row header with empty edge cells");
  check(EtfHistory.parse("<table>"+split+"<tr><td>IBIT</td><td>FBTC</td></tr>"+rows+"</table>").size()==2,"two-row header with rowspans");
  rejects("<table>"+header+rows.replace("12 Jan 2024","30 Feb 2024")+"</table>","invalid calendar date rejected");
  rejects("<table>"+header+rows.replace("12 Jan 2024","2024-02-30")+"</table>","invalid ISO date rejected");
  // Full history must survive well beyond the old ten-row display limit.
  StringBuilder many=new StringBuilder("<table>"+header);
  for(int i=0;i<400;i++)many.append("<tr><td>").append(java.time.LocalDate.of(2024,1,1).plusDays(i)).append("</td><td>1.0</td><td>0.0</td><td>1.0</td></tr>");
  many.append("</table>");check(EtfHistory.parse(many.toString()).size()==400,"full history retained");
  System.out.println("PASS: ETF history, "+checks+" assertions");
 }
}
