package com.tide.journal.data;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/** Real cancellation and payload limits, using a blocking connection without network. */
public final class HttpRequestsTest {
 private static int checks;
 private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;}
 private static final class Blocking extends HttpURLConnection {
  final CountDownLatch started=new CountDownLatch(1),closed=new CountDownLatch(1),finished=new CountDownLatch(1);
  Blocking()throws Exception{super(new URL("https://data-api.binance.vision/api/v3/time"));}
  public void connect(){}
  public boolean usingProxy(){return false;}
  public void disconnect(){closed.countDown();}
  public int getResponseCode(){return 200;}
  public InputStream getInputStream(){return new InputStream(){public int read()throws IOException{return read(new byte[1],0,1);}public int read(byte[]b,int o,int n)throws IOException{started.countDown();try{while(!closed.await(50,TimeUnit.MILLISECONDS)){/* socket reads may ignore interrupt */}}catch(InterruptedException e){try{closed.await(2,TimeUnit.SECONDS);}catch(InterruptedException ignored){}}throw new IOException("socket closed");}};}
 }
 public static void main(String[]args)throws Exception{
  check(HttpRequests.read(new ByteArrayInputStream("观潮".getBytes("UTF-8")),6).equals("观潮"),"UTF-8 payload and exact limit");
  try{HttpRequests.read(new ByteArrayInputStream(new byte[8193]),8192);throw new AssertionError("oversized accepted");}catch(IOException expected){checks++;}
  for(String url:new String[]{"http://data-api.binance.vision/","https://evil.example/","https://data-api.binance.vision:444/","https://user@data-api.binance.vision/","not a url"}){
   try{HttpRequests.get(url);throw new AssertionError("unapproved address accepted");}catch(IOException expected){checks++;}
  }
  Thread.currentThread().interrupt();try{HttpRequests.read(new ByteArrayInputStream(new byte[0]),1);throw new AssertionError("cancelled read accepted");}catch(InterruptedIOException expected){checks++;}finally{Thread.interrupted();}
  ExecutorService pool=Executors.newSingleThreadExecutor();Blocking connection=new Blocking();
  try{
   Future<?> task=HttpRequests.submit(pool,()->{try{HttpRequests.receive(connection);}catch(IOException expected){}finally{connection.finished.countDown();}});
   check(connection.started.await(2,TimeUnit.SECONDS),"request entered blocking socket");
   long start=System.nanoTime();check(task.cancel(true),"inflight task cancelled");
   check(System.nanoTime()-start<TimeUnit.MILLISECONDS.toNanos(250),"UI cancellation returns promptly");
   check(connection.closed.await(2,TimeUnit.SECONDS),"cancel closes actual connection");
   check(connection.finished.await(2,TimeUnit.SECONDS),"blocked worker released");
   check(pool.submit(()->42).get(2,TimeUnit.SECONDS)==42,"next request can run on released worker");
  }finally{pool.shutdownNow();}
  System.out.println("PASS: HTTP cancellation and response limits, "+checks+" assertions");
 }
}
