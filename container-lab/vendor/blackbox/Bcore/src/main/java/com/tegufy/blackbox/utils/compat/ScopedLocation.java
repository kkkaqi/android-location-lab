package com.tegufy.blackbox.utils.compat;

import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.*;
import android.util.Log;
import com.tegufy.blackbox.BlackBoxCore;
import com.tegufy.blackbox.app.BActivityThread;
import com.tegufy.blackbox.entity.location.BLocation;
import com.tegufy.blackbox.fake.frameworks.BLocationManager;
import com.tegufy.blackbox.core.system.location.IBLocationManagerService;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Per-guest API adapter. Never injects a system test provider or changes outside processes. */
public final class ScopedLocation {
 public static final Object UNHANDLED=new Object();
 private static final ConcurrentMap<Object,Subscription> SUBS=new ConcurrentHashMap<>();
 private static final ScheduledExecutorService CLOCK=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"guest-location");t.setDaemon(true);return t;});
 private static int deliveries;
 public static boolean isActive()throws RemoteException{return target()!=null;}
 private static LocationUpdateBudget budget(Object[] args){
  int max=Integer.MAX_VALUE;long duration=Long.MAX_VALUE;
  if(args!=null)for(Object arg:args)if(arg!=null && arg.getClass().getName().equals("android.location.LocationRequest")){
   try{max=((Number)arg.getClass().getMethod("getMaxUpdates").invoke(arg)).intValue();}catch(ReflectiveOperationException ignored){}
   try{duration=((Number)arg.getClass().getMethod("getDurationMillis").invoke(arg)).longValue();}catch(ReflectiveOperationException ignored){}
  }
  return new LocationUpdateBudget(max,duration,SystemClock.elapsedRealtime());
 }
 private static BLocation target() throws RemoteException {
  if(BActivityThread.getAppConfig()==null || !"com.tencent.mm".equals(BActivityThread.getAppPackageName()) || BActivityThread.getUserId()!=0)return null;
  IBLocationManagerService service=BLocationManager.get().getService();
  if(service==null)throw new IllegalStateException("Container location service unavailable");
  if(!ScopedLocationPolicy.enabled(true,BActivityThread.getAppPackageName(),0,service.getPattern(0,"com.tencent.mm")))return null;
  BLocation p=service.getLocation(0,"com.tencent.mm");
  if(p==null)throw new IllegalStateException("Enabled container location has no coordinates");
  ScopedLocationPolicy.parse(Double.toString(p.getLatitude()),Double.toString(p.getLongitude()));
  return p;
 }
 private static Location fresh(BLocation p,String provider){
  Location location=new Location(provider);location.setLatitude(p.getLatitude());location.setLongitude(p.getLongitude());
  location.setAccuracy(20f);location.setTime(System.currentTimeMillis());location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
  return location;
 }
 private static String provider(Object[] args){
  if(args!=null && args.length>0 && args[0] instanceof String && Arrays.asList("gps","network","fused","passive").contains(args[0]))return (String)args[0];
  return "gps";
 }
 private static IInterface callback(Object[] args,String descriptor)throws RemoteException{
  if(args!=null)for(Object arg:args)if(arg instanceof IInterface){
   IInterface value=(IInterface)arg;
   if(value.asBinder()!=null && descriptor.equals(value.asBinder().getInterfaceDescriptor()))return value;
  }
  return null;
 }
 private static PendingIntent pending(Object[] args){if(args!=null)for(Object arg:args)if(arg instanceof PendingIntent)return (PendingIntent)arg;return null;}
 private static Object done(Method method){return method.getReturnType()==int.class?0:method.getReturnType()==boolean.class?Boolean.TRUE:null;}
 private static void deliver(IInterface callback,Location location,boolean once)throws Exception{
  String name=once?"onLocation":"onLocationChanged";
  for(Method m:callback.getClass().getMethods())if(m.getName().equals(name)){
   Class<?>[] types=m.getParameterTypes();
   if(types.length==1 && types[0]==Location.class){m.setAccessible(true);m.invoke(callback,location);return;}
   String[] names=new String[types.length];for(int i=0;i<types.length;i++)names[i]=types[i].getName();
   if(!once && ScopedLocationPolicy.listCallback(names)){m.setAccessible(true);m.invoke(callback,Collections.singletonList(location),null);return;}
  }
  throw new NoSuchMethodException("Unsupported virtual location callback: "+name);
 }
 public static Object intercept(Method method,Object[] args)throws Throwable{
  String name=method.getName();
  if(name.equals("removeUpdates")||name.equals("unregisterLocationListener")||name.equals("unregisterLocationPendingIntent")){
   IInterface listener=callback(args,"android.location.ILocationListener");Object key=listener==null?pending(args):listener.asBinder();
   Subscription old=key==null?null:SUBS.get(key);if(old!=null){old.close();return done(method);}
  }
  BLocation p=target();if(p==null)return UNHANDLED;
  String provider=provider(args);
  if(name.equals("getLastLocation")||name.equals("getLastKnownLocation")){Log.d("GuestLocation","last "+provider);return fresh(p,provider);}
  if(name.equals("getCurrentLocation")){
   IInterface value=callback(args,"android.location.ILocationCallback");
   if(value==null)throw new IllegalStateException("Current-location callback missing");
   deliver(value,fresh(p,provider),true);Log.d("GuestLocation","current delivered");return null;
  }
  if(name.equals("registerLocationListener")||name.equals("requestLocationUpdates")||name.equals("registerLocationPendingIntent")){
   IInterface listener=callback(args,"android.location.ILocationListener");PendingIntent pi=pending(args);
   if(listener==null && pi==null)throw new IllegalStateException("Location callback not supported by this build");
   Object key=listener==null?pi:listener.asBinder();Subscription s=new Subscription(key,listener,pi,provider,budget(args));
   Subscription old=SUBS.put(key,s);if(old!=null)old.close();
   try{s.start();}catch(Throwable error){s.close();throw error;}
   Log.d("GuestLocation","listener registered "+provider);return done(method);
  }
  if(name.equals("getAllProviders")||name.equals("getProviders"))return Arrays.asList("gps","network","fused");
  if(name.equals("getBestProvider"))return "gps";
  if(name.equals("isProviderEnabledForUser")||name.equals("isProviderEnabled"))return true;
  if(name.equals("isLocationEnabledForUser"))return true;
  return UNHANDLED;
 }
 private static final class Subscription implements IBinder.DeathRecipient {
  final Object key;final IInterface listener;final PendingIntent pending;final String provider;
  volatile boolean closed;ScheduledFuture<?> task;final LocationUpdateBudget budget;
  Subscription(Object key,IInterface listener,PendingIntent pending,String provider,LocationUpdateBudget budget){this.key=key;this.listener=listener;this.pending=pending;this.provider=provider;this.budget=budget;}
  void start()throws RemoteException{
   if(listener!=null)listener.asBinder().linkToDeath(this,0);
   ScheduledFuture<?> created=CLOCK.scheduleWithFixedDelay(()->{
    if(closed)return;
    if(!budget.allows(SystemClock.elapsedRealtime())){close();return;}
    try{
     BLocation p=target();if(p==null){close();return;}
     Location point=fresh(p,provider);
     if(listener!=null)deliver(listener,point,false);
     else pending.send(BlackBoxCore.getContext(),0,new Intent().putExtra(LocationManager.KEY_LOCATION_CHANGED,point));
     budget.delivered();if(!budget.allows(SystemClock.elapsedRealtime()))close();
     if(++deliveries<=3)Log.d("GuestLocation","callback delivered "+provider);
    }catch(Exception error){Log.w("GuestLocation","callback stopped",error);close();}
   },20,1000,TimeUnit.MILLISECONDS);
   synchronized(this){if(closed)created.cancel(false);else task=created;}
  }
  void close(){
   synchronized(this){if(closed)return;closed=true;if(task!=null)task.cancel(false);}
   SUBS.remove(key,this);
   if(listener!=null)try{listener.asBinder().unlinkToDeath(this,0);}catch(RuntimeException ignored){}
  }
  @Override public void binderDied(){close();}
 }
}
