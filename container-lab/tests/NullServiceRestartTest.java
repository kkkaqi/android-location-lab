package app.containerlab;
import com.tegufy.blackbox.proxy.record.ProxyServiceRecord;
public final class NullServiceRestartTest {
 private static int checks;
 private static void check(boolean value){checks++;if(!value)throw new AssertionError("Empty restart must not restore stale guest service data");}
 public static void main(String[]args){
  ProxyServiceRecord record=ProxyServiceRecord.create(null);
  check(record.mServiceIntent==null);check(record.mServiceInfo==null);check(record.mToken==null);
  check(record.mUserId==0);check(record.mStartId==0);
  System.out.println("PASS: "+checks+" null service restart checks");
 }
}
