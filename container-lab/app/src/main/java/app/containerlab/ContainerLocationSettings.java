package app.containerlab;

import android.content.Context;
import com.tegufy.blackbox.entity.location.BLocation;
import com.tegufy.blackbox.fake.frameworks.BLocationManager;
import com.tegufy.blackbox.core.system.location.IBLocationManagerService;
import java.util.Locale;

final class ContainerLocationSettings {
 static String latitude(Context c){return c.getSharedPreferences("location",0).getString("lat","30.595000");}
 static String longitude(Context c){return c.getSharedPreferences("location",0).getString("lon","114.297500");}
 static void apply(Context context,double[] point,boolean enabled)throws Exception{
  IBLocationManagerService service=BLocationManager.get().getService();
  if(service==null)throw new IllegalStateException("容器位置服务不可用");
  if(!enabled){service.setPattern(0,ImportPolicy.WECHAT,BLocationManager.CLOSE_MODE);
   if(service.getPattern(0,ImportPolicy.WECHAT)!=BLocationManager.CLOSE_MODE)throw new IllegalStateException("关闭未确认");return;}
  try{
   service.setLocation(0,ImportPolicy.WECHAT,new BLocation(point[0],point[1]));
   service.setPattern(0,ImportPolicy.WECHAT,BLocationManager.OWN_MODE);
   BLocation stored=service.getLocation(0,ImportPolicy.WECHAT);
   if(service.getPattern(0,ImportPolicy.WECHAT)!=2 || stored==null || stored.getLatitude()!=point[0] || stored.getLongitude()!=point[1])throw new IllegalStateException("坐标保存回读不一致");
   if(!context.getSharedPreferences("location",0).edit().putString("lat",Double.toString(point[0])).putString("lon",Double.toString(point[1])).commit())throw new IllegalStateException("界面坐标保存失败");
  }catch(Exception error){try{service.setPattern(0,ImportPolicy.WECHAT,0);}catch(Exception rollback){error.addSuppressed(rollback);}throw error;}
 }
 static String summary(Context context){
  try{
   IBLocationManagerService service=BLocationManager.get().getService();
   if(service==null)return "容器定位：位置服务不可用";
   if(service.getPattern(0,ImportPolicy.WECHAT)!=2)return "容器定位：已关闭";
   BLocation point=service.getLocation(0,ImportPolicy.WECHAT);
   return point==null?"容器定位：配置缺少坐标":String.format(Locale.CHINA,"容器定位：微信专用 %.6f, %.6f",point.getLatitude(),point.getLongitude());
  }catch(Exception error){return "容器定位：读取失败（"+error.getClass().getSimpleName()+"）";}
 }
}
