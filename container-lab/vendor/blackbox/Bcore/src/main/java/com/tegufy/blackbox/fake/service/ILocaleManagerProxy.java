package com.tegufy.blackbox.fake.service;

import android.os.IBinder;
import java.lang.reflect.*;
import black.android.os.BRServiceManager;
import com.tegufy.blackbox.BlackBoxCore;
import com.tegufy.blackbox.app.BActivityThread;
import com.tegufy.blackbox.fake.hook.*;
import com.tegufy.blackbox.utils.compat.LocaleQueryPolicy;

/** Normal own-app locale reads use the actual installed host identity. No locale setters or grants. */
public final class ILocaleManagerProxy extends BinderInvocationStub {
 public ILocaleManagerProxy(){super(BRServiceManager.get().getService("locale"));}
 @Override protected Object getWho(){
  try {
   Method method=Class.forName("android.app.ILocaleManager$Stub").getDeclaredMethod("asInterface",IBinder.class);
   method.setAccessible(true);
   return method.invoke(null,BRServiceManager.get().getService("locale"));
  }catch(ReflectiveOperationException error){throw new IllegalStateException("Locale service interface unavailable",error);}
 }
 @Override protected void inject(Object base,Object proxy){
  replaceSystemService("locale");
  try {
   Object manager=BlackBoxCore.getContext().getSystemService("locale");
   if(manager!=null){Field field=manager.getClass().getDeclaredField("mService");field.setAccessible(true);field.set(manager,proxy);}
  }catch(ReflectiveOperationException error){android.util.Log.w("LocaleCompatibility","Existing locale manager cache not replaced",error);}
 }
 @Override public boolean isBadEnv(){return false;}
 @ProxyMethod("getApplicationLocales")
 public static class GetApplicationLocales extends MethodHook {
  @Override protected Object hook(Object who,Method method,Object[] args)throws Throwable{
   Object[] mapped=BActivityThread.getAppConfig()==null?null:LocaleQueryPolicy.map(args,BActivityThread.getAppPackageName(),BActivityThread.getUserId(),
      BlackBoxCore.getHostPkg(),BlackBoxCore.getHostUid()/100000);
   try{return method.invoke(who,mapped==null?args:mapped);}
   catch(InvocationTargetException error){throw error.getCause();}
  }
 }
}
