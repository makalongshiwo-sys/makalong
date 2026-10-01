package com.tide.journal.domain;

import java.io.IOException;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

/** Public disclosure history. Missing cells remain null; totals are never inferred. */
public final class EtfHistory {
 private EtfHistory() {}
 public static final class Row {
  public final String date; public final Double total; public final LinkedHashMap<String,Double> funds;
  public Row(String date,Double total,LinkedHashMap<String,Double> funds){this.date=date;this.total=total;this.funds=funds;}
  public boolean complete(){return total!=null&&!funds.isEmpty()&&!funds.containsValue(null);}
 }
 private static final Pattern TABLE=Pattern.compile("(?is)<table\\b[^>]*>(.*?)</table>");
 private static final Pattern TR=Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>");
 private static final Pattern CELL=Pattern.compile("(?is)<t[dh]\\b[^>]*>(.*?)</t[dh]>");
 private static String plain(String html){
  return html.replaceAll("(?is)<script\\b.*?</script>|<style\\b.*?</style>","")
   .replaceAll("(?s)<[^>]*>"," ").replace("&nbsp;"," ").replace("&#160;"," ").replace("&amp;","&")
   .replace("&#8722;","-").replace('\u2212','-').replace('\u00a0',' ').trim().replaceAll("\\s+"," ");
 }
 private static String date(String cell){
  for(String pattern:new String[]{"dd MMM yyyy","dd MMM yy","yyyy-MM-dd","dd/MM/yyyy"}){
   try{return LocalDate.parse(cell,new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH)).toString();}
   catch(DateTimeParseException ignored){}
  }return null;
 }
 private static Double number(String text)throws IOException{
  String value=text.trim();if(value.isEmpty()||value.equals("-")||value.equals("—")||value.equalsIgnoreCase("N/A"))return null;
  boolean negative=value.startsWith("(")&&value.endsWith(")");
  if(negative)value=value.substring(1,value.length()-1);
  value=value.replace(",","").replace("$","").trim();
  try{double n=Double.parseDouble(value);if(!Double.isFinite(n))throw new NumberFormatException();return negative?-n:n;}
  catch(NumberFormatException e){throw new IOException("ETF 数字格式改变："+text);}
 }
 public static List<Row> parse(String html)throws IOException{
  if(html==null||html.length()>2500000)throw new IOException("ETF 响应过大或为空");
  Matcher tables=TABLE.matcher(html);
  while(tables.find()){
   List<String> header=null;int dateIndex=-1,totalIndex=-1,pendingColumns=-1;TreeMap<String,Row> history=new TreeMap<>();
   Matcher rows=TR.matcher(tables.group(1));
   while(rows.find()){
    List<String> cells=new ArrayList<>();Matcher columns=CELL.matcher(rows.group(1));
    while(columns.find())cells.add(plain(columns.group(1)));
    int di=-1,ti=-1;
    for(int i=0;i<cells.size();i++){if(cells.get(i).equalsIgnoreCase("date"))di=i;if(cells.get(i).equalsIgnoreCase("total"))ti=i;}
    if(di<0&&ti==cells.size()-1&&ti>1&&cells.get(0).isEmpty()){
     boolean tickers=true;for(int i=1;i<ti;i++)if(!cells.get(i).matches("[A-Z][A-Z0-9]{1,7}"))tickers=false;
     if(tickers)di=0;
    }
    if(di>=0&&ti>di+1){header=cells;dateIndex=di;totalIndex=ti;continue;}
    // Some issuer tables use two header rows with Date/Total cells spanning both.
    if(di<0&&ti==cells.size()-1&&ti>1){
     boolean blank=true;for(int i=0;i<ti;i++)if(!cells.get(i).isEmpty())blank=false;
     if(blank){pendingColumns=cells.size();continue;}
    }
    if(pendingColumns>2){
     List<String> tickers=new ArrayList<>(cells);
     if(tickers.size()==pendingColumns&&tickers.get(0).isEmpty()&&tickers.get(tickers.size()-1).isEmpty())tickers=new ArrayList<>(tickers.subList(1,tickers.size()-1));
     boolean valid=tickers.size()==pendingColumns-2;
     for(String ticker:tickers)if(!ticker.matches("[A-Z][A-Z0-9]{1,7}"))valid=false;
     if(valid){header=new ArrayList<>();header.add("Date");header.addAll(tickers);header.add("Total");dateIndex=0;totalIndex=header.size()-1;pendingColumns=-1;continue;}
    }
    if(header==null||cells.size()<=dateIndex)continue;
    String day=date(cells.get(dateIndex));if(day==null)continue;
    if(cells.size()!=header.size())throw new IOException("ETF 表格列数改变，暂停解析");
    if(LocalDate.parse(day).isAfter(LocalDate.now(ZoneOffset.UTC)))throw new IOException("ETF 出现未来交易日");
    LinkedHashMap<String,Double> funds=new LinkedHashMap<>();
    for(int i=dateIndex+1;i<totalIndex;i++){
     String ticker=header.get(i);
     if(!ticker.matches("[A-Z][A-Z0-9]{1,7}"))throw new IOException("ETF 基金表头改变，暂停解析");
     if(funds.containsKey(ticker))throw new IOException("ETF 基金列重复");
     funds.put(ticker,number(cells.get(i)));
    }
    Row value=new Row(day,number(cells.get(totalIndex)),funds);
    if(value.complete()){
     double sum=0;for(double flow:funds.values())sum+=flow;
     if(Math.abs(sum-value.total)>0.15+funds.size()*0.06)throw new IOException("ETF 基金与总额不一致");
    }
    if(history.put(day,value)!=null)throw new IOException("ETF 交易日重复");
   }
   if(!history.isEmpty())return new ArrayList<>(history.values());
  }throw new IOException("未找到可核对的 ETF 历史表");
 }
 public static List<Row> select(List<Row> rows,String start,String end){
  List<Row> result=new ArrayList<>();for(Row row:rows)if((start.isEmpty()||row.date.compareTo(start)>=0)&&(end.isEmpty()||row.date.compareTo(end)<=0))result.add(row);return result;
 }
}
