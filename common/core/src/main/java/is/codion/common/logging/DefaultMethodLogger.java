/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.logging;

import is.codion.common.Text;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * TODO this class should be able to handle/recover from incorrect usage, not crash the application
 */
final class DefaultMethodLogger implements MethodLogger {

  private final Deque<DefaultEntry> callStack = new LinkedList<>();
  private final LinkedList<Entry> entries = new LinkedList<>();
  private final Function<Object, String> argumentStringProvider;
  private final int maxSize;

  private boolean enabled = false;

  DefaultMethodLogger(final int maxSize, final Function<Object, String> argumentStringProvider) {
    this.maxSize = maxSize;
    this.argumentStringProvider = requireNonNull(argumentStringProvider, "argumentStringProvider");
  }

  @Override
  public synchronized void logAccess(final String method) {
    if (enabled) {
      callStack.push(new DefaultEntry(method, null));
    }
  }

  @Override
  public synchronized void logAccess(final String method, final Object argument) {
    if (enabled) {
      callStack.push(new DefaultEntry(method, argumentStringProvider.apply(argument)));
    }
  }

  @Override
  public Entry logExit(final String method) {
    return logExit(method, null);
  }

  @Override
  public Entry logExit(final String method, final Throwable exception) {
    return logExit(method, exception, null);
  }

  @Override
  public synchronized Entry logExit(final String method, final Throwable exception, final String exitMessage) {
    if (!enabled) {
      return null;
    }
    if (callStack.isEmpty()) {
      throw new IllegalStateException("Call stack is empty when trying to log method exit: " + method);
    }
    final DefaultEntry entry = callStack.pop();
    if (!entry.getMethod().equals(method)) {//todo pop until found or empty?
      throw new IllegalStateException("Expecting method " + entry.getMethod() + " but got " + method + " when trying to log method exit");
    }
    entry.setExitTime();
    entry.setException(exception);
    entry.setExitMessage(exitMessage);
    if (callStack.isEmpty()) {
      if (entries.size() == maxSize) {
        entries.removeFirst();
      }
      entries.addLast(entry);
    }
    else {
      callStack.peek().addChildEntry(entry);
    }

    return entry;
  }

  @Override
  public synchronized boolean isEnabled() {
    return enabled;
  }

  @Override
  public synchronized void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    entries.clear();
    callStack.clear();
  }

  @Override
  public synchronized List<Entry> getEntries() {
    return Collections.unmodifiableList(entries);
  }

  private static final class DefaultEntry implements Entry, Serializable {

    private static final long serialVersionUID = 1;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final NumberFormat MICROSECONDS_FORMAT = NumberFormat.getIntegerInstance();

    private final LinkedList<Entry> childEntries = new LinkedList<>();
    private final String method;
    private final String accessMessage;
    private final long accessTime;
    private final long accessTimeNano;
    private String exitMessage;
    private long exitTime;
    private long exitTimeNano;
    private String stackTrace;

    /**
     * Instantiates a new Entry, using the current time
     * @param method the method being logged
     * @param accessMessage the message associated with accessing the method
     */
    private DefaultEntry(final String method, final String accessMessage) {
      this(method, accessMessage, System.currentTimeMillis(), System.nanoTime());
    }

    /**
     * Instantiates a new Entry
     * @param method the method being logged
     * @param accessMessage the message associated with accessing the method
     * @param accessTime the time to associate with accessing the method
     * @param accessTimeNano the nano time to associate with accessing the method
     */
    private DefaultEntry(final String method, final String accessMessage, final long accessTime, final long accessTimeNano) {
      this.method = method;
      this.accessTime = accessTime;
      this.accessTimeNano = accessTimeNano;
      this.accessMessage = accessMessage;
    }

    @Override
    public boolean hasChildEntries() {
      return !childEntries.isEmpty();
    }

    @Override
    public List<Entry> getChildEntries() {
      return Collections.unmodifiableList(childEntries);
    }

    @Override
    public String getMethod() {
      return method;
    }

    @Override
    public boolean isComplete() {
      return exitTime != 0;
    }

    @Override
    public long getAccessTime() {
      return accessTime;
    }

    @Override
    public long getExitTime() {
      return exitTime;
    }

    @Override
    public String getAccessMessage() {
      return accessMessage;
    }

    @Override
    public long getDuration() {
      return exitTimeNano - accessTimeNano;
    }

    @Override
    public void append(final StringBuilder builder) {
      builder.append(toString()).append("\n");
      appendLogEntries(builder, getChildEntries(), 1);
    }

    @Override
    public String toString() {
      return toString(0);
    }

    @Override
    public String toString(final int indentation) {
      final String indentString = indentation > 0 ? Text.padString("", indentation, '\t', Text.Alignment.RIGHT) : "";
      final StringBuilder stringBuilder = new StringBuilder();
      final LocalDateTime accessDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(accessTime), TimeZone.getDefault().toZoneId());
      if (isComplete()) {
        stringBuilder.append(indentString).append(TIMESTAMP_FORMATTER.format(accessDateTime)).append(" @ ").append(method).append(
                !nullOrEmpty(accessMessage) ? (": " + accessMessage) : "").append("\n");
        final LocalDateTime exitDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(exitTime), TimeZone.getDefault().toZoneId());
        stringBuilder.append(indentString).append(TIMESTAMP_FORMATTER.format(exitDateTime)).append(" > ")
                .append(MICROSECONDS_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(getDuration()))).append(" μs")
                .append(exitMessage == null ? "" : " (" + exitMessage + ")");
        if (stackTrace != null) {
          stringBuilder.append("\n\n").append(indentString).append(stackTrace);
        }
      }
      else {
        stringBuilder.append(indentString).append(TIMESTAMP_FORMATTER.format(accessDateTime)).append(" @ ").append(method).append(
                !nullOrEmpty(accessMessage) ? (": " + accessMessage) : "");
      }

      return stringBuilder.toString();
    }

    /**
     * Adds a child entry to this log entry
     * @param childEntry the child entry to add
     */
    private void addChildEntry(final Entry childEntry) {
      childEntries.addLast(childEntry);
    }

    /**
     * Sets the exit time using the current time
     */
    private void setExitTime() {
      setExitTime(System.currentTimeMillis(), System.nanoTime());
    }

    /**
     * @param exitTime the exit time
     * @param exitTimeNano the exit time in nanoseconds
     */
    private void setExitTime(final long exitTime, final long exitTimeNano) {
      this.exitTime = exitTime;
      this.exitTimeNano = exitTimeNano;
    }

    /**
     * @param exception the exception that occurred during the method call logged by this entry
     */
    private void setException(final Throwable exception) {
      if (exception != null) {
        this.stackTrace = getStackTrace(exception);
      }
    }

    /**
     * @param exitMessage the exit message
     */
    private void setExitMessage(final String exitMessage) {
      this.exitMessage = exitMessage;
    }

    /**
     * Appends the given log entries to the log
     * @param log the log
     * @param entries the List containing the entries to append
     * @param indentationLevel the indentation to use for the given log entries
     */
    private static void appendLogEntries(final StringBuilder log, final List<Entry> entries, final int indentationLevel) {
      for (final Entry entry : entries) {
        log.append(entry.toString(indentationLevel)).append("\n");
        appendLogEntries(log, entry.getChildEntries(), indentationLevel + 1);
      }
    }

    private static String getStackTrace(final Throwable exception) {
      final StringWriter sw = new StringWriter();
      exception.printStackTrace(new PrintWriter(sw));

      return sw.toString();
    }
  }
}
