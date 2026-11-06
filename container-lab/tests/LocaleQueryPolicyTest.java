package app.containerlab;
import com.tegufy.blackbox.utils.compat.LocaleQueryPolicy;
public final class LocaleQueryPolicyTest {
 public static void main(String[] args) {
  Object[] source={"com.tencent.mm",0};
  Object[] mapped=LocaleQueryPolicy.map(source,"com.tencent.mm",0,"app.containerlab",0);
  if(mapped==null || !"app.containerlab".equals(mapped[0]) || !Integer.valueOf(0).equals(mapped[1]))throw new AssertionError("own guest locale query must use real host identity");
  if(mapped==source || !"com.tencent.mm".equals(source[0]))throw new AssertionError("do not mutate original arguments");
  if(LocaleQueryPolicy.map(source,"other.app",0,"app.containerlab",0)!=null)throw new AssertionError("other guest unchanged");
  if(LocaleQueryPolicy.map(source,"com.tencent.mm",1,"app.containerlab",0)!=null)throw new AssertionError("other virtual user unchanged");
  if(LocaleQueryPolicy.map(new Object[]{"other.app",0},"com.tencent.mm",0,"app.containerlab",0)!=null)throw new AssertionError("cross-package query unchanged");
  if(LocaleQueryPolicy.map(new Object[]{"com.tencent.mm",10},"com.tencent.mm",0,"app.containerlab",0)!=null)throw new AssertionError("cross-system-user query unchanged");
  if(LocaleQueryPolicy.map(new Object[]{"com.tencent.mm",0,1},"com.tencent.mm",0,"app.containerlab",0)!=null)throw new AssertionError("unknown signature unchanged");
  if(LocaleQueryPolicy.map(null,"com.tencent.mm",0,"app.containerlab",0)!=null)throw new AssertionError("null args unchanged");
  if(LocaleQueryPolicy.map(source,null,0,"app.containerlab",0)!=null)throw new AssertionError("no bound guest unchanged");
  System.out.println("PASS: 9 locale query policy checks");
 }
}
