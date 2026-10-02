package com.tide.journal.data;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Cancelling a task also closes its socket, without blocking the UI thread. */
public final class HttpRequests {
 private HttpRequests() {}
 private static final class Scope {volatile boolean cancelled;volatile HttpURLConnection connection;}
 private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
 private static final ExecutorService CLOSER=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"guanchao-http-close");t.setDaemon(true);return t;});
 public static void checkCancelled() throws InterruptedIOException {
  Scope scope=CURRENT.get();if(Thread.currentThread().isInterrupted()||(scope!=null&&scope.cancelled))throw new InterruptedIOException("请求已取消");
 }
 public static Future<?> submit(ExecutorService executor,Runnable action) {
  FutureTask<Void> task=new FutureTask<Void>(action,null){
   private final Scope scope=new Scope();
   @Override public void run(){CURRENT.set(scope);try{super.run();}finally{CURRENT.remove();}}
   @Override public boolean cancel(boolean interrupt){boolean stopped=super.cancel(interrupt);if(stopped){if(interrupt){scope.cancelled=true;HttpURLConnection c=scope.connection;if(c!=null)CLOSER.execute(c::disconnect);}if(executor instanceof ThreadPoolExecutor)((ThreadPoolExecutor)executor).remove(this);}return stopped;}
  };
  executor.execute(task);return task;
 }
 public static String read(InputStream in,int cap) throws IOException {
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];
  while(true){checkCancelled();int n=in.read(buffer);if(n<0)break;if(n>cap-out.size())throw new IOException("响应过大");out.write(buffer,0,n);}
  checkCancelled();return out.toString("UTF-8");
 }
 public static String get(String url) throws IOException {
  checkCancelled();URI u;
  try{u=URI.create(url);}catch(IllegalArgumentException e){throw new IOException("来源地址无效",e);}
  if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443)||!Arrays.asList("raw.githubusercontent.com","api.github.com","data-api.binance.vision","fapi.binance.com","gamma-api.polymarket.com","www.federalreserve.gov","farside.co.uk").contains(u.getHost()))throw new IOException("来源未获准");
  return receive((HttpURLConnection)new URL(url).openConnection());
 }
 static String receive(HttpURLConnection c) throws IOException {
  Scope scope=CURRENT.get();if(scope!=null)scope.connection=c;
  try{
   checkCancelled();c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(10000);
   c.setRequestProperty("Cache-Control","no-cache");c.setRequestProperty("User-Agent","Guanchao native/0.8");
   if(c.getResponseCode()!=200)throw new IOException("来源 HTTP "+c.getResponseCode());
   try(InputStream in=c.getInputStream()){return read(in,2500000);}
  }finally{if(scope!=null)scope.connection=null;c.disconnect();}
 }
}
