package com.tegufy.blackbox.utils.compat;
public final class LocationUpdateBudget {
 private final int max;private final long end;private int sent;
 public LocationUpdateBudget(int count,long duration,long now){max=Math.max(0,count);long d=Math.max(0,duration);end=d>Long.MAX_VALUE-now?Long.MAX_VALUE:now+d;}
 public boolean allows(long now){return sent<max && now<end;}
 public void delivered(){if(sent<max)sent++;}
}
