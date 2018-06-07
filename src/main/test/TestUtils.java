import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.junit.Test;

public class TestUtils
{
  @Test
  public void testParallel() {
   Assert.assertEquals(8d, MainFrame.calcParallel(Lists.newArrayList(16d, 16d)));;
  }
}
