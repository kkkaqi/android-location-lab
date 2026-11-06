package app.containerlab;
import com.tegufy.blackbox.utils.compat.LocationUpdateBudget;
public final class LocationUpdateBudgetTest {
 public static void main(String[] a){
  LocationUpdateBudget once=new LocationUpdateBudget(1,1000,10);
  if(!once.allows(10))throw new AssertionError("first update allowed");once.delivered();
  if(once.allows(11))throw new AssertionError("single update must stop");
  LocationUpdateBudget timed=new LocationUpdateBudget(10,100,50);
  if(!timed.allows(149)||timed.allows(150))throw new AssertionError("request duration respected");
  LocationUpdateBudget unlimited=new LocationUpdateBudget(Integer.MAX_VALUE,Long.MAX_VALUE,100);
  if(!unlimited.allows(Long.MAX_VALUE-1))throw new AssertionError("infinite duration must not overflow");
  if(new LocationUpdateBudget(1,0,10).allows(10))throw new AssertionError("expired request does not deliver");
  LocationUpdateBudget two=new LocationUpdateBudget(2,5000,0);two.delivered();
  if(!two.allows(1))throw new AssertionError("second update allowed");two.delivered();
  if(two.allows(2))throw new AssertionError("maxUpdates boundary");
  System.out.println("PASS: 7 location request lifetime checks");
 }
}
