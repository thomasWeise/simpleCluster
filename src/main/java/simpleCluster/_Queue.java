package simpleCluster;

import java.io.BufferedWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Consumer;

import thomasWeise.tools.Configuration;
import thomasWeise.tools.ConsoleIO;
import thomasWeise.tools.IOUtils;

/** the job queue */
final class _Queue {

  /** the jobs queue */
  public static final String PARAM_QUEUE_DIR = "queue";

  /** the local synchronizer */
  private static final Object SYNCH = new Object();

  /** the queue directory */
  static final Path _QUEUE_DIR = Configuration
      .getPath(_Queue.PARAM_QUEUE_DIR, () -> Paths.get("."));

  /** the queue file */
  private static final Path QUEUE_FILE = IOUtils
      .canonicalizePath(_Queue._QUEUE_DIR.resolve(".queue"));

  /** the queue lock */
  private static final Path QUEUE_LOCK = IOUtils
      .canonicalizePath(_Queue._QUEUE_DIR.resolve(".queue.lock"));

  /**
   * sleep for at least 100ms when waiting for accessing the queue
   */
  private static final long MIN_QUEUE_SLEEP = 3000L;
  /**
   * the maximum time to sleep when waiting for accessing the queue
   */
  private static final long MAX_QUEUE_SLEEP = 30000L;

  /**
   * sleep for at least 100ms when waiting for getting the next job
   */
  private static final long MIN_JOB_SLEEP = _Queue.MIN_QUEUE_SLEEP;
  /**
   * the maximum time to sleep when waiting for getting the next job
   */
  private static final long MAX_JOB_SLEEP = 180000L;

  /**
   * access the queue
   *
   * @param accessor
   *          the accessor
   */
  private static final void __accessQueue(final Consumer<Path> accessor) {
    long queueSleep = _Queue.MIN_QUEUE_SLEEP;
    synchronized (_Queue.SYNCH) {
      outer: for (;;) {
        Path lock = null;

        try {
          lock = Files.createFile(_Queue.QUEUE_LOCK);
        } catch (final Throwable error) {
          if (!(error instanceof FileAlreadyExistsException)) {
            ConsoleIO.stderr("error when trying to access lock '"
                + _Queue.QUEUE_LOCK + '\'', error);
          }
          try {
            Thread.sleep(queueSleep);
          } catch (final InterruptedException ie) {
            // ignore
          }
          queueSleep = Math.max(_Queue.MIN_QUEUE_SLEEP,
              Math.min(_Queue.MAX_QUEUE_SLEEP, (queueSleep * 10) / 9));
          continue outer;
        }
        if (lock == null) {
          ConsoleIO.stderr(
              "lock file '" + _Queue.QUEUE_LOCK + "' has null name??",
              null);
          lock = _Queue.QUEUE_LOCK;
        }

        try {
          if (!(Files.exists(_Queue.QUEUE_FILE))) {
            try {
              Files.createFile(_Queue.QUEUE_FILE);
            } catch (final Throwable error) {
              ConsoleIO.stderr("error when creating queue file '"
                  + _Queue.QUEUE_FILE + '\'', error);
            }
          }

          accessor.accept(_Queue.QUEUE_FILE);
          return;

        } finally {
          try {
            Files.delete(lock);
          } catch (final Throwable error) {
            ConsoleIO.stderr(
                "error when deleting lock file '" + lock + '\'', error);
          }
        }
      }
    }
  }

  /**
   * submit a job to the job queue
   *
   * @param command
   *          the command
   * @param dir
   *          the directory
   * @param times
   *          the number of times to submit the job
   */
  static final void _submit(final String command, final String dir,
      final int times) {

    ConsoleIO.stdout("Start submission of command '" + command
        + "' in directory '" + dir + "' for " + times + " times to queue '"
        + _Queue._QUEUE_DIR + '\'');

    if (times <= 0) {
      throw new IllegalArgumentException(
          "number of times cannot be " + times);
    }
    String c = command.trim();

    synchronized (_Queue.SYNCH) {
      for (;;) {
        final int len = c.length();
        if ((len <= 0) || (c.indexOf(';') >= 0)) {
          throw new IllegalArgumentException("command '" + command
              + "', equivalent to '" + c + "' is invalid");
        }
        final char ch = c.charAt(0);
        if (((ch == '\'') || (ch == '"')) && (c.charAt(len - 1) == ch)) {
          c = c.substring(1, len - 1).trim();
        } else {
          break;
        }
      }

      String d = dir.trim();
      for (;;) {
        final int len = d.length();
        if ((len <= 0) || (d.indexOf(';') >= 0)) {
          throw new IllegalArgumentException("directory '" + dir
              + "', equivalent to '" + d + "' is invalid");
        }
        final char ch = d.charAt(0);
        if (((ch == '\'') || (ch == '"')) && (d.charAt(len - 1) == ch)) {
          d = d.substring(1, len - 1).trim();
        } else {
          break;
        }
      }

      final String cc = c;
      final String dd = d;

      _Queue.__accessQueue((path) -> {

        try (BufferedWriter bw = Files.newBufferedWriter(path,
            StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {

          final String line = cc + ';' + dd;
          for (int i = times; (--i) >= 0;) {
            bw.write(line);
            bw.newLine();
          }

          bw.flush();
        } catch (final Throwable error) {
          ConsoleIO.stderr("error when submitting command '" + cc
              + "' for directory '" + dd + "' to queue '" + path + "' for "
              + times + " times.", error);
        }
      });
    }
  }

  /**
   * extract a job from the queue
   *
   * @return the job command and work directory
   */
  static final String[] _getJob() {
    final String[][] res = new String[1][];
    long jobSleep = _Queue.MIN_JOB_SLEEP;

    synchronized (_Queue.SYNCH) {
      for (;;) {
        _Queue.__accessQueue((path) -> {
          List<String> lines = null;
          try {
            try {
              lines = Files.readAllLines(path);
            } catch (final Throwable error) {
              ConsoleIO.stderr(//
                  "error when trying to read queue '" + path + '\'',
                  error);
              lines = null;
            }
            if (lines != null) {
              final int size = lines.size();
              if (size > 0) {
                looper: for (int lineIndex = 0; lineIndex < size; lineIndex++) {
                  final String s = lines.get(lineIndex);
                  if ((s != null) && (!s.isEmpty())) {
                    final int idx = s.indexOf(';');
                    if ((idx > 0) && (idx < (s.length() - 1))) {
                      res[0] = new String[] { s.substring(0, idx).trim(),
                          s.substring(idx + 1).trim() };
                      try {
                        Files.write(path,
                            lines.subList(lineIndex + 1, size),
                            StandardOpenOption.TRUNCATE_EXISTING);
                      } catch (final Throwable error) {
                        ConsoleIO.stderr(//
                            "error when trying to write back job from queue to '"
                                + path + "' after taking job '" + res[0][0]
                                + "' for dir '" + res[0][1] + '\'',
                            error);
                      }
                      break looper;
                    }
                  }
                }
              }
            }

          } catch (final Throwable error) {
            ConsoleIO.stderr(//
                "error when trying to get job from queue '" + path + '\'',
                error);
          }
        });

        if (res[0] != null) {
          ConsoleIO.stdout("received job '" + res[0][0] + "' for dir '"
              + res[0][1] + '\'');
          return res[0];
        }

        try {
          Thread.sleep(jobSleep);
        } catch (final InterruptedException ie) {
          // ignore
        }
        jobSleep = Math.max(_Queue.MIN_JOB_SLEEP,
            Math.min(_Queue.MAX_JOB_SLEEP, (jobSleep * 11) / 9));
      }
    }
  }
}
