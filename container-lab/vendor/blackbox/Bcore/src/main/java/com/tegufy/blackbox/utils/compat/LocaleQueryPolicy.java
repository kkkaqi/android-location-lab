package com.tegufy.blackbox.utils.compat;
public final class LocaleQueryPolicy {
 public static Object[] map(Object[] args,String guest,int guestUser,String host,int hostUser) {
  if(!"com.tencent.mm".equals(guest) || guestUser!=0 || host==null || host.isEmpty() || args==null || args.length!=2
    || !"com.tencent.mm".equals(args[0]) || !(args[1] instanceof Integer) || ((Integer)args[1])!=hostUser)return null;
  Object[] mapped=args.clone();mapped[0]=host;return mapped;
 }
}
