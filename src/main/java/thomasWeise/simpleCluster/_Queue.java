package thomasWeise.simpleCluster;

import java.io.BufferedWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import thomasWeise.tools.Configuration;
import thomasWeise.tools.ConsoleIO;
import thomasWeise.tools.IOUtils;

/** the job queue */
final class _Queue {

  /** the jobs queue */
  public static final String PARAM_QUEUE_DIR = "queue";//$NON-NLS-1$

  /** the local synchronizer */
  private static final Object SYNCH = new Object();

  /** the queue directory */
  static final Path _QUEUE_DIR = Configuration
      .getPath(_Queue.PARAM_QUEUE_DIR, () -> Paths.get("."));//$NON-NLS-1$

  /** the queue file */
  private static final Path QUEUE_FILE = IOUtils
      .canonicalizePath(_Queue._QUEUE_DIR.resolve(".queue"));//$NON-NLS-1$

  /** the queue lock */
  private static final Path QUEUE_LOCK = IOUtils
      .canonicalizePath(_Queue._QUEUE_DIR.resolve(".queue.lock"));//$NON-NLS-1$

  /** the tag for blocking the whole machine */
  static final String _WHOLE_MACHINE = "blocksMachine";//$NON-NLS-1$

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

  /** the number of running jobs */
  private static volatile int s_running = 0;

  /** are we locked, i.e., waiting for single-machine job? */
  private static volatile int s_locked = 0;

  /** the pending jobs */
  private static volatile int s_pending = 0;

  /**
   * access the queue to store or pull a job
   *
   * @param accessor
   *          the accessor
   * @param dest
   *          an optional destination
   */
  private static final void __accessQueue(
      final BiConsumer<Path, String[][]> accessor, final String[][] dest) {
    long queueSleep = _Queue.MIN_QUEUE_SLEEP;
    synchronized (_Queue.SYNCH) {
      outer: for (;;) {
        Path lock = null;

        try {
          lock = Files.createFile(_Queue.QUEUE_LOCK);
        } catch (final Throwable error) {
          if (!(error instanceof FileAlreadyExistsException)) {
            ConsoleIO.stderr("error when trying to access lock '"//$NON-NLS-1$
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
          ConsoleIO.stderr("lock file '" + //$NON-NLS-1$
              _Queue.QUEUE_LOCK + "' has null name??", //$NON-NLS-1$
              null);
          lock = _Queue.QUEUE_LOCK;
        }

        try {
          if (!(Files.exists(_Queue.QUEUE_FILE))) {
            try {
              Files.createFile(_Queue.QUEUE_FILE);
            } catch (final Throwable error) {
              ConsoleIO.stderr("error when creating queue file '"//$NON-NLS-1$
                  + _Queue.QUEUE_FILE + '\'', error);
            }
          }

          accessor.accept(_Queue.QUEUE_FILE, dest);
          return;

        } finally {
          try {
            Files.delete(lock);
          } catch (final Throwable error) {
            ConsoleIO.stderr("error when deleting lock file '" //$NON-NLS-1$
                + lock + '\'', error);
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
   * @param wholeMachine
   *          should the job block the whole machine?
   */
  static final void _submit(final String command, final String dir,
      final int times, final boolean wholeMachine) {

    ConsoleIO.stdout("Start submission of command '"//$NON-NLS-1$
        + command + "' in directory '"//$NON-NLS-1$
        + dir + "' for " + //$NON-NLS-1$
        times + " times to queue '"//$NON-NLS-1$
        + _Queue._QUEUE_DIR + '\'');

    if (times <= 0) {
      throw new IllegalArgumentException("number of times cannot be "//$NON-NLS-1$
          + times);
    }
    String c = command.trim();

    for (;;) { // prepare command
      final int len = c.length();
      if ((len <= 0) || (c.indexOf(';') >= 0)) {
        throw new IllegalArgumentException("command '"//$NON-NLS-1$
            + command + "', equivalent to '"//$NON-NLS-1$
            + c + "' is invalid");//$NON-NLS-1$
      }
      final char ch = c.charAt(0);
      if (((ch == '\'') || (ch == '"')) && (c.charAt(len - 1) == ch)) {
        c = c.substring(1, len - 1).trim();
      } else {
        break;
      }
    }

    String d = dir.trim();
    for (;;) { // prepare directory
      final int len = d.length();
      if ((len <= 0) || (d.indexOf(';') >= 0)) {
        throw new IllegalArgumentException("directory '"//$NON-NLS-1$
            + dir + "', equivalent to '"//$NON-NLS-1$
            + d + "' is invalid");//$NON-NLS-1$
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
    final String l1 = c + ';' + d;
    final String line = ((wholeMachine ? (l1 + ';' + _Queue._WHOLE_MACHINE)
        : l1));

    _Queue.__accessQueue((path, ignore) -> {

      try (BufferedWriter bw = Files.newBufferedWriter(path,
          StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
        for (int i = times; (--i) >= 0;) {
          bw.write(line);
          bw.newLine();
        }
        bw.flush();
      } catch (final Throwable error) {
        ConsoleIO.stderr("error when submitting command '"//$NON-NLS-1$
            + cc + "' for directory '"//$NON-NLS-1$
            + dd + "' to queue '" + path//$NON-NLS-1$
            + "' for "//$NON-NLS-1$
            + times + " times as line '" + //$NON-NLS-1$
        line + '\'', error);
      }
    }, null);

  }

  /**
   * read a single job
   *
   * @param path
   *          the file
   * @param res
   *          the destination
   */
  private static final void __readJob(final Path path,
      final String[][] res) {
    List<String> lines = null;
    Arrays.fill(res, null);

    try {
      try {
        lines = Files.readAllLines(path);
      } catch (final Throwable error) {
        ConsoleIO.stderr(//
            "error when trying to read queue '"//$NON-NLS-1$
                + path + '\'',
            error);
        lines = null;
      }

      if (lines != null) { // got lines
        final int size = lines.size();
        if (size > 0) { // got some lines
          looper: for (int lineIndex = 0; lineIndex < size; lineIndex++) {
            final String s = lines.get(lineIndex);// get current line
            if (s == null) {
              continue;
            } // empty line
            final int length = s.length();
            if (length <= 0) {
              continue;
            } // empty line

            final int idx = s.indexOf(';');
            if ((idx <= 0) || (idx >= (length - 1))) {
              // invalid job
              continue;
            }

            final String job = s.substring(0, idx).trim();
            if ((job == null) || (job.isEmpty())) {
              continue;
            } // empty job;

            final int idx2 = s.indexOf(';', idx + 1);
            final String dir, code;
            if (idx2 > idx) {
              dir = s.substring(idx + 1, idx2).trim();
              code = s.substring(idx2 + 1).trim();
            } else {
              dir = s.substring(idx + 1).trim();
              code = null;
            }

            if ((dir == null) || (dir.isEmpty())) {
              continue;
            } // empty dir

            // got a valid job
            res[0] = new String[] { job, dir, code };

            try {
              Files.write(path, lines.subList(lineIndex + 1, size),
                  StandardOpenOption.TRUNCATE_EXISTING);
            } catch (final Throwable error) {
              ConsoleIO.stderr(//
                  "error when trying to write back job from queue to '"//$NON-NLS-1$
                      + path + "' after taking job '"//$NON-NLS-1$
                      + res[0][0] + "' for dir '"//$NON-NLS-1$
                      + res[0][1] + '\'',
                  error);
            }
            break looper;
          }
        }
      }
    } catch (final Throwable error) {
      ConsoleIO.stderr(//
          "error when trying to get job from queue '"//$NON-NLS-1$
              + path + '\'',
          error);
    }
  }

  /**
   * do the job sleeping
   *
   * @param jobSleepTime
   *          the job sleeping
   * @return the job sleep time
   */
  private static final long __jobSleep(final long jobSleepTime) {
    try {
      Thread.sleep(jobSleepTime); // wait
    } catch (final InterruptedException ie) {
      // ignore
    }
    return (Math.max(_Queue.MIN_JOB_SLEEP,
        Math.min(_Queue.MAX_JOB_SLEEP, (jobSleepTime * 11) / 9)));
  }

  /**
   * extract and execute a job from the queue
   *
   * @param executor
   *          the executor
   * @return the job command and work directory
   */
  static final void _applyToNextJob(
      final BiConsumer<String, String> executor) {
    final String[][] res = new String[1][];
    boolean requiresLock = false;
    long jobSleep = _Queue.MIN_JOB_SLEEP;

    looper: for (;;) {
      synchronized (_Queue.SYNCH) {// synchronize threads

        // first, if we already got a job, we check if we can execute it
        if (res[0] != null) {
          // we got one job that now could be executed
          if ((_Queue.s_locked <= 0) || // no locking
              (requiresLock && (_Queue.s_running <= 0))) {
            // we need the lock and no one else is running
            ++_Queue.s_running;
            --_Queue.s_pending; // decrease pending counter
            break looper; // only way to escape looper: can execute
          }
          // here, definitely s_locked must be true
        } // end got job
        // if we get here, we did not have a job or s_locked is true and we
        // need to wait
        if (_Queue.s_locked > 0) {
          // we are locked: three cases: someone else has a job and locked
          // execution while we dont have a job OR we have a lock-requiring
          // job and someone else is running, OR someone else is locking
          // execution and we have a lock-requiring job: wait
          try {
            _Queue.SYNCH.wait(); // so we wait until they are done
          } catch (final InterruptedException ie) {
            //
          }
          continue looper; // try again
        }
        // if we get here, we do not have a job and nobody locked execution

        // we can only get here if there has not been any locking
        _Queue.__accessQueue(_Queue::__readJob, res);
        if (res[0] == null) { // got no job
          if ((_Queue.s_pending) <= 0) {
            // if no job is pending, directly execute wait to block other
            // threads
            jobSleep = _Queue.__jobSleep(jobSleep);
            continue looper;
          }
        } else {
          ++_Queue.s_pending;
          if (_Queue._WHOLE_MACHINE.equals(res[0][2])) {
            ++_Queue.s_locked; // got lock-requiring job: directly lock
            requiresLock = true;
          }
        }
      } // end synchronized

      if (res[0] == null) {
        jobSleep = _Queue.__jobSleep(jobSleep);
        continue looper;
      }
      ConsoleIO.stdout("received job '"//$NON-NLS-1$
          + res[0][0] + "' for dir '"//$NON-NLS-1$
          + res[0][1] + "' with tag " //$NON-NLS-1$
          + res[0][2]);
    } // end job waiting loop

    // we are now outside of the synchronization
    ConsoleIO.stdout("ready to execute job '"//$NON-NLS-1$
        + res[0][0] + "' for dir '"//$NON-NLS-1$
        + res[0][1] + "' with tag " //$NON-NLS-1$
        + res[0][2]);

    try {// execute the job
      executor.accept(res[0][0], res[0][1]);
    } finally {// update pending count
      synchronized (_Queue.SYNCH) { // update synchronization
        --_Queue.s_running;
        if (requiresLock) {
          --_Queue.s_locked;
        }
        _Queue.SYNCH.notifyAll();
      }
    }
  }
}
