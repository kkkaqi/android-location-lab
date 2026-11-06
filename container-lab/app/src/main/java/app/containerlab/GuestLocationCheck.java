package app.containerlab;
import android.app.Application;
import android.location.*;
import android.os.*;
import com.tegufy.blackbox.app.BActivityThread;
import com.tegufy.blackbox.fake.frameworks.BLocationManager;
final class GuestLocationCheck {
 static void run(Application app,android.content.Context host){
  if(BActivityThread.getAppConfig()==null || BActivityThread.getUserId()!=0 || !"com.tencent.mm".equals(Application.getProcessName()))return;
  new Handler(Looper.getMainLooper()).post(()->{
   try{
    int fine=app.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION");
    LabLog.append(host,"访客定位权限：fine="+fine+" coarse="+app.checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION")+" background="+app.checkSelfPermission("android.permission.ACCESS_BACKGROUND_LOCATION")+"；宿主fine="+host.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION"));
    LabLog.append(host,"定位适配核验："+com.tegufy.blackbox.fake.service.ILocationManagerProxy.diagnostic(app));
    LocationManager manager=app.getSystemService(LocationManager.class);
    com.tegufy.blackbox.core.system.location.IBLocationManagerService config=BLocationManager.get().getService();
    if(config==null)throw new IllegalStateException("容器位置服务不可用");
    if(config.getPattern(0,"com.tencent.mm")!=2){
     double lat=Double.parseDouble(ContainerLocationSettings.latitude(host)),lon=Double.parseDouble(ContainerLocationSettings.longitude(host));
     int found=0;
     for(String provider:new String[]{"gps","network","fused"}){
      Location p=manager.getLastKnownLocation(provider);
      if(p!=null){found++;if(Math.abs(p.getLatitude()-lat)<1e-7 && Math.abs(p.getLongitude()-lon)<1e-7)throw new IllegalStateException("关闭后仍读到先前目标，需要核查");}
     }
     LabLog.append(host,"访客定位结果：已关闭；系统缓存"+found+"路非目标、"+(3-found)+"路为空；页面未验证");return;
    }
    if(fine!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("访客精确位置权限未获系统确认");
    com.tegufy.blackbox.entity.location.BLocation target=config.getLocation(0,"com.tencent.mm");
    if(target==null)throw new IllegalStateException("配置缺少目标");
    for(String provider:new String[]{"gps","network","fused"}){
     Location p=manager.getLastKnownLocation(provider);
     check(p,target);
     LabLog.append(host,"访客定位核验 "+provider+"="+(p==null?"null":p.getLatitude()+","+p.getLongitude()+" ageMs="+(SystemClock.elapsedRealtimeNanos()-p.getElapsedRealtimeNanos())/1000000));
    }
    int[] count={0};boolean[] current={false},failed={false};
    manager.getCurrentLocation("gps",null,app.getMainExecutor(),p->{try{check(p,target);current[0]=true;LabLog.append(host,"访客定位核验 current="+p.getLatitude()+","+p.getLongitude());}catch(Exception e){failed[0]=true;LabLog.error(host,"访客定位核验失败",e);}});
    LocationListener listener=new LocationListener(){
     @Override public void onLocationChanged(Location p){try{check(p,target);LabLog.append(host,"访客定位核验 callback="+(++count[0])+" "+p.getLatitude()+","+p.getLongitude());if(count[0]==2){manager.removeUpdates(this);LabLog.append(host,"访客定位核验 callback已取消；不代表微信界面采纳");}}catch(Exception e){failed[0]=true;manager.removeUpdates(this);LabLog.error(host,"访客定位核验失败",e);}}
     @Override public void onProviderDisabled(String p){} @Override public void onProviderEnabled(String p){}
     @Override public void onStatusChanged(String p,int s,Bundle b){}
    };
    manager.requestLocationUpdates("gps",1000,0,listener,Looper.getMainLooper());
    new Handler(Looper.getMainLooper()).postDelayed(()->{try{manager.removeUpdates(listener);LabLog.append(host,"访客定位结果："+(!failed[0]&&current[0]&&count[0]==2?"框架检查通过（3路读回/单次/2次回调/取消）；页面未验证":"框架检查失败；请查看本地记录"));}catch(Exception e){LabLog.error(host,"访客定位核验失败",e);}},4000);
   }catch(Exception error){LabLog.error(host,"访客定位核验失败",error);}
  });
 }
 private static void check(Location point,com.tegufy.blackbox.entity.location.BLocation target){
  if(point==null || !Double.isFinite(point.getLatitude()) || !Double.isFinite(point.getLongitude()) || Math.abs(point.getLatitude()-target.getLatitude())>1e-7 || Math.abs(point.getLongitude()-target.getLongitude())>1e-7
      || Math.abs(SystemClock.elapsedRealtimeNanos()-point.getElapsedRealtimeNanos())>5000000000L)throw new IllegalStateException("位置与目标不符或时间戳过期");
 }
}
