package app.containerlab;
import com.tegufy.blackbox.utils.compat.ScopedLocationPolicy;
public final class ScopedLocationPolicyTest {
 private static int count;
 private static void check(boolean value,String msg){count++;if(!value)throw new AssertionError(msg);}
 public static void main(String[] args){
  check(ScopedLocationPolicy.enabled(true,"com.tencent.mm",0,2),"bound own-mode WeChat enabled");
  check(!ScopedLocationPolicy.enabled(false,"com.tencent.mm",0,2),"unbound never enabled");
  check(!ScopedLocationPolicy.enabled(true,"other.app",0,2),"other package excluded");
  check(!ScopedLocationPolicy.enabled(true,"com.tencent.mm",1,2),"other virtual user excluded");
  check(!ScopedLocationPolicy.enabled(true,"com.tencent.mm",0,1),"global mode excluded");
  check(!ScopedLocationPolicy.enabled(true,"com.tencent.mm",0,0),"disabled excluded");
  double[] p=ScopedLocationPolicy.parse("30.595000","114.297500");
  check(p[0]==30.595 && p[1]==114.2975,"latitude/longitude order");
  check(ScopedLocationPolicy.parse("-90","180")[0]==-90,"valid edges");
  for(String[] bad:new String[][]{{"NaN","114"},{"Infinity","114"},{"91","114"},{"30","181"},{"","114"},{"abc","114"}}){
   boolean rejected=false;try{ScopedLocationPolicy.parse(bad[0],bad[1]);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid coordinates rejected");
  }
  check(ScopedLocationPolicy.listCallback(new String[]{"java.util.List","android.os.IRemoteCallback"}),"Android15 list callback");
  check(!ScopedLocationPolicy.listCallback(new String[]{"android.location.Location"}),"legacy callback not mistaken for list");
  check(!ScopedLocationPolicy.listCallback(new String[]{"java.util.List","java.lang.String"}),"unknown callback excluded");
  System.out.println("PASS: "+count+" container location policy checks");
 }
}
