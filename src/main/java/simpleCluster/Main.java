package simpleCluster;

import thomasWeise.tools.Configuration;
import thomasWeise.tools.ConsoleIO;

/** Run the simple cluster */
public final class Main {

  /** the run parameter */
  public static final String PARAM_RUN = "run";

  /** the run parameter */
  public static final String PARAM_CORES = "cores";

  /** the submit parameter */
  public static final String PARAM_SUBMIT = "submit";

  /** the command parameter */
  public static final String PARAM_COMMAND = "cmd";
  /** the working directory parameter */
  public static final String PARAM_DIR = "dir";
  /** the number of times parameter */
  public static final String PARAM_TIMES = "times";

  /**
   * The main routine running the simple cluster
   *
   * @param args
   *          the arguments
   */
  public static void main(final String[] args) {
    ConsoleIO.stdout((ps) -> {
      ps.println("Welcome to the Simple Cluster Job Executor");
      ps.println("Usage: java -jar simpleCluster.jar [options]");
      ps.println("[options] are");
      ps.println(Main.PARAM_RUN + " [" + Main.PARAM_CORES
          + "=numberOfCores] to run a worker with the specified amount of threads.");
      ps.println(Main.PARAM_SUBMIT + " " + Main.PARAM_COMMAND
          + "=command " + Main.PARAM_DIR + "=workingDir" + " ["
          + Main.PARAM_TIMES
          + "=numberOfTimesToSumit] to submit a command to be executed in the given directory the specified amount of times.");
      ps.print(_Queue.PARAM_QUEUE_DIR);
      ps.println(
          "=queueDir the optional queue directory, valid for both of the above commands.");
    });

    Configuration.putCommandLine(args);

    if (Configuration.getBoolean(Main.PARAM_SUBMIT)) {
      final String c =
          Configuration.getString(Main.PARAM_COMMAND);
      final String d = Configuration.getString(Main.PARAM_DIR);
      final Integer times =
          Configuration.getInteger(Main.PARAM_TIMES);
      final int t = ((times != null) ? times.intValue() : 1);
      _Queue._submit(c, d, t);
    }

    if (Configuration.getBoolean(Main.PARAM_RUN)) {
      ConsoleIO.stdout(
          "worker mode set: we will launch workers and begin to process queue '"
              + _Queue._QUEUE_DIR + '\'');

      final int[] np = new int[] {
          Runtime.getRuntime().availableProcessors() };
      Configuration.synchronizedConfig(() -> {
        final Integer cores =
            Configuration.getInteger(Main.PARAM_CORES);
        if (cores != null) {
          final int i = cores.intValue();
          if ((i > 0) && (i < 100)) {
            np[0] = i;
          }
        }
        Configuration.putInteger(Main.PARAM_CORES, np[0]);
      });

      final int numProc = np[0];
      for (int index = 1; index <= numProc; index++) {
        new _Worker();
      }
      ConsoleIO.stdout((("started " + //$NON-NLS-1$
          Main.PARAM_CORES) + '=') + numProc
          + " worker threads"); //$NON-NLS-1$
    }
  }
}
