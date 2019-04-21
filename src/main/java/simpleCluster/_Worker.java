package simpleCluster;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import thomasWeise.tools.Configuration;
import thomasWeise.tools.ConsoleIO;
import thomasWeise.tools.IOUtils;

/** a worker thread */
final class _Worker extends Thread {

  /** the id counter */
  private static final AtomicInteger ID = new AtomicInteger();

  /** the shell parameter */
  static final String PARAM_SHELL = "sh"; //$NON-NLS-1$

  /** the shell */
  private static final String SHELL = _Worker.__shell();

  /**
   * get the shell
   *
   * @return the shell
   */
  private static final String __shell() {
    final String shell;
    final Path sh = Configuration.getExecutable(_Worker.PARAM_SHELL);
    if (sh == null) {
      shell = _Worker.PARAM_SHELL;
    } else {
      shell = sh.toString();
    }

    ConsoleIO.stdout("using command '" + shell//$NON-NLS-1$
        + "' as shell."); //$NON-NLS-1$
    return (shell);
  }

  /** the worker constructor */
  _Worker() {
    super("worker_" + //$NON-NLS-1$
        _Worker.ID.incrementAndGet());
    this.start();
  }

  /**
   * run a job
   *
   * @param command
   *          the command
   * @param dir
   *          the directory
   */
  private static final void __run(final String command, final String dir) {
    final ProcessBuilder pb = new ProcessBuilder();
    pb.command(_Worker.SHELL);
    pb.directory(IOUtils.canonicalizePath(dir).toFile());
    pb.redirectOutput(Redirect.INHERIT);
    pb.redirectError(Redirect.INHERIT);

    try {
      final Process p = pb.start();

      try {
        ConsoleIO.stdout("launched shell '"//$NON-NLS-1$
            + _Worker.SHELL + "' in directory '" + //$NON-NLS-1$
            pb.directory() + '\'');

        boolean ok = false;
        try (final OutputStream os = p.getOutputStream();
            final Writer w = new OutputStreamWriter(os);
            final BufferedWriter bw = new BufferedWriter(w)) {
          bw.write(command);
          bw.newLine();
          ok = true;
        } catch (final Throwable error) {
          ok = false;
          ConsoleIO.stderr("error when piping command '"//$NON-NLS-1$
              + command + "' to shell '"//$NON-NLS-1$
              + _Worker.SHELL + "' in directory '"//$NON-NLS-1$
              + pb.directory() + '\'', error);
        }
        if (ok) {
          ConsoleIO.stdout("successfully piped command '"//$NON-NLS-1$
              + command + "' to shell '" + //$NON-NLS-1$
              _Worker.SHELL + "' in directory '"//$NON-NLS-1$
              + pb.directory() + '\'');
        } else {
          try {
            p.destroyForcibly();
          } catch (final Throwable error) {
            ConsoleIO.stderr("error when destroying shell process", //$NON-NLS-1$
                error);
          }
        }
      } finally {
        try {
          final int r = p.waitFor();
          ConsoleIO.stdout("command '" + command //$NON-NLS-1$
              + "' in directory '" + pb.directory()//$NON-NLS-1$
              + "' executed via shell '" + //$NON-NLS-1$
              _Worker.SHELL + "' finished with exit code " + r);//$NON-NLS-1$

        } catch (final Throwable error) {
          ConsoleIO.stderr("error when waiting for command '"//$NON-NLS-1$
              + command + "' in directory '"//$NON-NLS-1$
              + pb.directory() + "' via shell '" //$NON-NLS-1$
              + _Worker.SHELL + "' to finish.'", //$NON-NLS-1$
              error);
        }
      }

    } catch (final Throwable error) {
      ConsoleIO.stderr("error when executing command '"//$NON-NLS-1$
          + command + " in directory '" + //$NON-NLS-1$
          pb.directory() + "' via shell '"//$NON-NLS-1$
          + _Worker.SHELL + '\'', error);
    }

  }

  /** run */
  @Override
  public final void run() {
    for (;;) {
      _Queue._applyToNextJob(_Worker::__run);
    }
  }
}
