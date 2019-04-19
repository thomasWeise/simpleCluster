package simpleCluster;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.atomic.AtomicInteger;

import thomasWeise.tools.ConsoleIO;
import thomasWeise.tools.IOUtils;

/** a worker thread */
final class _Worker extends Thread {

  /** the id counter */
  private static final AtomicInteger ID = new AtomicInteger();

  /** the worker constructor */
  _Worker() {
    super("worker_" + //$NON-NLS-1$
        _Worker.ID.incrementAndGet());
    this.start();
  }

  /** run */
  @Override
  public final void run() {
    for (;;) {
      final String[] job = _Queue._getJob();
      final ProcessBuilder pb = new ProcessBuilder();
      pb.command("sh");
      pb.directory(IOUtils.canonicalizePath(job[1]).toFile());
      pb.redirectOutput(Redirect.INHERIT);
      pb.redirectError(Redirect.INHERIT);

      try {
        final Process p = pb.start();

        try {
          ConsoleIO.stdout("launched shell '" + pb.command()
              + " in directory '" + pb.directory() + '\'');

          boolean ok = false;
          try (final OutputStream os = p.getOutputStream();
              final Writer w = new OutputStreamWriter(os);
              final BufferedWriter bw = new BufferedWriter(w)) {
            bw.write(job[0]);
            bw.newLine();
            ok = true;
          } catch (final Throwable error) {
            ok = false;
            ConsoleIO.stderr("error when piping command '"
                + pb.command() + "' to shell in directory '"
                + pb.directory() + '\'', error);
          }
          if (ok) {
            ConsoleIO.stdout("successfully piped command '"
                + job[0] + "' to shell in directory '"
                + pb.directory() + '\'');
          } else {
            try {
              p.destroyForcibly();
            } catch (final Throwable error) {
              ConsoleIO.stderr(
                  "error when destroying shell process", error);
            }
          }
        } finally {
          try {
            final int r = p.waitFor();
            ConsoleIO.stdout("command '" + pb.command()
                + " in directory '" + pb.directory()
                + "' finished with exit code " + r);

          } catch (final Throwable error) {
            ConsoleIO.stderr("error when waiting for command '"
                + pb.command() + " in directory '"
                + pb.directory() + "' to finish.'", error);
          }
        }

      } catch (final Throwable error) {
        ConsoleIO.stderr(
            "error when executing command '" + pb.command()
                + " in directory '" + pb.directory() + '\'',
            error);
      }

    }
  }

}
