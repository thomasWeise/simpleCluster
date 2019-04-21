package thomasWeise.tools;

import java.io.PrintStream;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * With this class, we can write stuff to the console in a thread-safe way.
 */
public final class ConsoleIO {

  /**
   * Print to stdout and/or stderr
   *
   * @param print
   *          the stream consumer
   */
  public static final void print(
      final BiConsumer<PrintStream, PrintStream> print) {
    synchronized (System.out) {
      synchronized (System.err) {
        synchronized (System.in) {
          System.out.flush();
          System.err.flush();

          print.accept(System.out, System.err);

          System.out.flush();
          System.err.flush();
        }
      }
    }
  }

  /**
   * Print to stdout
   *
   * @param out
   *          the output
   */
  public static final void stdout(final Consumer<PrintStream> out) {
    ConsoleIO.print((u, v) -> {
      ConsoleIO.__printDate(u);
      out.accept(u);
    });
  }

  /**
   * Print to stdout
   *
   * @param line
   *          the line
   */
  public static final void stdout(final String line) {
    ConsoleIO.stdout((stdout) -> stdout.println(line));
  }

  /**
   * print the data
   *
   * @param ps
   *          the stream
   */
  private static final void __printDate(final PrintStream ps) {
    final StringBuilder sb;

    sb = new StringBuilder();
    sb.append(new Date());
    sb.append(' ');
    sb.append('[');
    final Thread c = Thread.currentThread();
    final String id = c.getName();
    if ((id == null) || id.isEmpty()) {
      sb.append("thread ");//$NON-NLS-1$
      sb.append(c.getId());// $NON-NLS-1$
    } else {
      sb.append(id);
    }
    sb.append(']');
    sb.append(' ');
    ps.print(sb.toString());
  }

  /**
   * Print to stderr
   *
   * @param out
   *          the output
   */
  public static final void stderr(final Consumer<PrintStream> out) {
    ConsoleIO.print((u, v) -> {
      ConsoleIO.__printDate(v);
      out.accept(v);
    });
  }

  /**
   * Print to stderr
   *
   * @param out
   *          the output
   * @param error
   *          the error
   */
  public static final void stderr(final Consumer<PrintStream> out,
      final Throwable error) {
    ConsoleIO.stderr((stderr) -> {
      out.accept(stderr);
      if (error != null) {
        if (error.getClass() == RuntimeException.class) {
          final Throwable cause = error.getCause();
          if (cause != null) {
            cause.printStackTrace(stderr);
            return;
          }
        }
        error.printStackTrace(stderr);
      }
    });
  }

  /**
   * Print to stderr
   *
   * @param message
   *          the message
   * @param error
   *          the error
   */
  public static final void stderr(final String message,
      final Throwable error) {
    ConsoleIO.stderr((stderr) -> {
      if (message != null) {
        stderr.println(message);
      }
    }, error);
  }
}