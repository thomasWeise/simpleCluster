package thomasWeise.simpleCluster;

import java.io.IOException;

import org.junit.Test;

import simpleCluster.Main;

/** A class for testing the simple cluster engine */
public class MainTest {

  /**
   * Test the main routing
   *
   * @throws IOException
   *           if i/o fails
   */
  @Test(timeout = 3600000)
  public final void testReproducible_7_12_5()
      throws IOException {
    Main.main(new String[0]);
  }
}
