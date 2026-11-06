package com.tegufy.blackbox.utils.compat;
public final class ScopedLocationPolicy {
 public static boolean enabled(boolean bound,String pkg,int user,int mode){return bound && "com.tencent.mm".equals(pkg) && user==0 && mode==2;}
 public static double[] parse(String lat,String lon){
  double a=Double.parseDouble(lat.trim()),b=Double.parseDouble(lon.trim());
  if(!Double.isFinite(a)||!Double.isFinite(b)||Math.abs(a)>90||Math.abs(b)>180)throw new IllegalArgumentException("纬度须在-90到90，经度须在-180到180之间");
  return new double[]{a,b};
 }
 public static boolean listCallback(String[] types){return types!=null && types.length==2 && "java.util.List".equals(types[0]) && "android.os.IRemoteCallback".equals(types[1]);}
}
