package simpleCluster;

import thomasWeise.tools.Configuration;
import thomasWeise.tools.ConsoleIO;

/** Run the simple cluster */
public final class Main {

  /** the run parameter */
  public static final String PARAM_RUN = "run";//$NON-NLS-1$

  /** the run parameter */
  public static final String PARAM_CORES = "cores";//$NON-NLS-1$

  /** the submit parameter */
  public static final String PARAM_SUBMIT = "submit";//$NON-NLS-1$

  /** the command parameter */
  public static final String PARAM_COMMAND = "cmd";//$NON-NLS-1$
  /** the working directory parameter */
  public static final String PARAM_DIR = "dir";//$NON-NLS-1$
  /** the number of times parameter */
  public static final String PARAM_TIMES = "times";//$NON-NLS-1$
  /** blocks the whole machine */
  public static final String PARAM_MACHINE = _Queue._WHOLE_MACHINE;

  /**
   * The main routine running the simple cluster
   *
   * @param args
   *          the arguments
   */
  public static void main(final String[] args) {
    ConsoleIO.stdout((ps) -> {
      ps.println("Welcome to the Simple Cluster Job Executor");//$NON-NLS-1$
      ps.println("Usage: java -jar simpleCluster.jar [options]");//$NON-NLS-1$
      ps.println(
          "This executable can serve both for submitting jobs and for executing them in parallel.");//$NON-NLS-1$
      ps.println("[options] are");//$NON-NLS-1$
      ps.println();
      ps.println(Main.PARAM_RUN + " [" + Main.PARAM_CORES//$NON-NLS-1$
          + "=numberOfCores] [" + _Worker.PARAM_SHELL + //$NON-NLS-1$
      "=/path/to/shell] to run a worker with the specified amount of threads using the specified shell for command execution.");//$NON-NLS-1$
      ps.println();
      ps.println(Main.PARAM_SUBMIT + ' ' + Main.PARAM_COMMAND + "=command " //$NON-NLS-1$
          + Main.PARAM_DIR + "=workingDir [" //$NON-NLS-1$
          + Main.PARAM_TIMES + "=numberOfTimesToSumit] [" //$NON-NLS-1$
          + Main.PARAM_MACHINE
          + "] to submit a command to be executed in the given directory the specified amount of times.");//$NON-NLS-1$
      ps.print("If " + Main.PARAM_MACHINE + //$NON-NLS-1$
      " is specified, the job blocks the whole machine.");//$NON-NLS-1$
      ps.println();
      ps.print(_Queue.PARAM_QUEUE_DIR);
      ps.println(
          "=queueDir the optional queue directory, valid for both of the above commands.");//$NON-NLS-1$
    });

    Configuration.putCommandLine(args);

    if (Configuration.getBoolean(Main.PARAM_SUBMIT)) {
      final String c = Configuration.getString(Main.PARAM_COMMAND);
      final String d = Configuration.getString(Main.PARAM_DIR);
      final Integer times = Configuration.getInteger(Main.PARAM_TIMES);
      final int t = ((times != null) ? times.intValue() : 1);
      final boolean machine = Configuration.getBoolean(Main.PARAM_MACHINE);
      _Queue._submit(c, d, t, machine);
    }

    if (Configuration.getBoolean(Main.PARAM_RUN)) {
      ConsoleIO.stdout(
          "worker mode set: we will launch workers and begin to process queue '"//$NON-NLS-1$
              + _Queue._QUEUE_DIR + '\'');

      final int[] np = new int[] {
          Runtime.getRuntime().availableProcessors() };
      Configuration.synchronizedConfig(() -> {
        final Integer cores = Configuration.getInteger(Main.PARAM_CORES);
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
          Main.PARAM_CORES) + '=') + numProc + " worker threads"); //$NON-NLS-1$
    }
  }
}
