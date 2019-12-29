/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * A method call logger allowing logging of nested method calls.
 * TODO this class should be able to handle/recover from incorrect usage, not crash the application
 */
public final class MethodLogger {

  private final Deque<Entry> callStack = new LinkedList<>();
  private final LinkedList<Entry> entries = new LinkedList<>();
  private final Function<Object, String> argumentStringProvider;
  private final int maxSize;

  private boolean enabled;

  /**
   * Instantiates a new MethodLogger.
   * @param maxSize the maximum log size
   * @param enabled true if this logger should be enabled
   */
  public MethodLogger(final int maxSize, final boolean enabled) {
    this(maxSize, enabled, new DefaultArgumentStringProvider());
  }

  /**
   * Instantiates a new MethodLogger.
   * @param maxSize the maximum log size
   * @param enabled true if this logger should be enabled
   * @param argumentStringProvider the ArgumentStringProvider
   */
  public MethodLogger(final int maxSize, final boolean enabled, final Function<Object, String> argumentStringProvider) {
    this.maxSize = maxSize;
    this.enabled = enabled;
    this.argumentStringProvider = argumentStringProvider;
  }

  /**
   * @param method the method being accessed
   */
  public void logAccess(final String method) {
    logAccess(method, null);
  }

  /**
   * @param method the method being accessed
   * @param arguments the method arguments
   */
  public synchronized void logAccess(final String method, final Object[] arguments) {
    if (enabled) {
      callStack.push(new Entry(method, argumentStringProvider.apply(arguments)));
    }
  }

  /**
   * @param method the method being exited
   * @return the Entry
   */
  public Entry logExit(final String method) {
    return logExit(method, null);
  }

  /**
   * @param method the method being exited
   * @param exception the exception, if any
   * @return the Entry
   */
  public Entry logExit(final String method, final Throwable exception) {
    return logExit(method, exception, null);
  }

  /**
   * @param method the method being exited
   * @param exception the exception, if any
   * @param exitMessage the message to associate with exiting the method
   * @return the Entry, or null if this logger is not enabled
   */
  public synchronized Entry logExit(final String method, final Throwable exception, final String exitMessage) {
    if (!enabled) {
      return null;
    }
    if (callStack.isEmpty()) {
      throw new IllegalStateException("Call stack is empty when trying to log method exit: " + method);
    }
    final Entry entry = callStack.pop();
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
      callStack.peek().addSubEntry(entry);
    }

    return entry;
  }

  /**
   * @return true if this logger is enabled
   */
  public synchronized boolean isEnabled() {
    return enabled;
  }

  /**
   * @param enabled true to enable this logger
   */
  public synchronized void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    entries.clear();
    callStack.clear();
  }

  /**
   * @return an unmodifiable view of the log entries
   */
  public synchronized List<Entry> getEntries() {
    return Collections.unmodifiableList(entries);
  }

  /**
   * Appends  the given log entries to the log
   * @param log the log
   * @param entry the log entry to append
   * @param indentationLevel the indentation to use for the given log entries
   */
  public static void appendLogEntry(final StringBuilder log, final Entry entry, final int indentationLevel) {
    if (entry != null) {
      log.append(entry.toString(indentationLevel)).append("\n");
      appendLogEntries(log, entry.getSubLog(), indentationLevel + 1);
    }
  }

  /**
   * Appends the given log entries to the log
   * @param log the log
   * @param entries the List containing the entries to append
   * @param indentationLevel the indentation to use for the given log entries
   */
  public static void appendLogEntries(final StringBuilder log, final List<Entry> entries, final int indentationLevel) {
    if (entries != null) {
      for (final MethodLogger.Entry entry : entries) {
        log.append(entry.toString(indentationLevel)).append("\n");
        appendLogEntries(log, entry.getSubLog(), indentationLevel + 1);
      }
    }
  }

  /**
   * Provides String represenations of method arguments.
   */
  public static class DefaultArgumentStringProvider implements Function<Object, String> {

    @Override
    public final String apply(final Object object) {
      return toString(object);
    }

    /**
     * Returns a String representation of the given object.
     * @param object the object
     * @return a String representation of the given object
     */
    protected String toString(final Object object) {
      if (object == null) {
        return "";
      }
      if (object.getClass().isArray()) {
        return toString((Object[]) object);
      }
      if (object instanceof Collection) {
        return "[" + toString(((Collection) object).toArray()) + "]";
      }

      return object.toString();
    }

    private String toString(final Object[] arguments) {
      if (arguments.length == 0) {
        return "";
      }

      return stream(arguments).map(this::toString).collect(joining(", "));
    }
  }

  /**
   * A log entry
   */
  public static final class Entry implements Serializable {

    private static final long serialVersionUID = 1;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final NumberFormat MICROSECONDS_FORMAT = NumberFormat.getIntegerInstance();

    private final LinkedList<Entry> subEntries = new LinkedList<>();
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
    public Entry(final String method, final String accessMessage) {
      this(method, accessMessage, System.currentTimeMillis(), System.nanoTime());
    }

    /**
     * Instantiates a new Entry
     * @param method the method being logged
     * @param accessMessage the message associated with accessing the method
     * @param accessTime the time to associate with accessing the method
     * @param accessTimeNano the nano time to associate with accessing the method
     */
    public Entry(final String method, final String accessMessage, final long accessTime, final long accessTimeNano) {
      this.method = method;
      this.accessTime = accessTime;
      this.accessTimeNano = accessTimeNano;
      this.accessMessage = accessMessage;
    }

    /**
     * @return true if this log entry contains sub log entries
     */
    public boolean containsSubLog() {
      return !subEntries.isEmpty();
    }

    /**
     * @return the sub log entries
     */
    public List<Entry> getSubLog() {
      return Collections.unmodifiableList(subEntries);
    }

    /**
     * @return the name of the method logged by this entry
     */
    public String getMethod() {
      return method;
    }

    /**
     * @return true if the exit time has been set for this entry
     */
    public boolean isComplete() {
      return exitTime != 0;
    }

    /**
     * @return the method access time
     */
    public long getAccessTime() {
      return accessTime;
    }

    /**
     * @return the exit time
     */
    public long getExitTime() {
      return exitTime;
    }

    /**
     * @return the access message
     */
    public String getAccessMessage() {
      return accessMessage;
    }

    /**
     * Returns the duration of the method call this entry represents in nanoseconds,
     * this value is 0 or undefined until {@code setExitTime()}
     * has been called, this can be checked via {@code isComplete()}.
     * @return the duration of the method call this entry represents
     */
    public long getDuration() {
      return exitTimeNano - accessTimeNano;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return toString(0);
    }

    /**
     * Returns a string representation of this log entry.
     * @param indentation the number of tab indents to prefix the string with
     * @return a string representation of this log entry
     */
    public String toString(final int indentation) {
      final String indentString = indentation > 0 ? TextUtil.padString("", indentation, '\t', TextUtil.Alignment.RIGHT) : "";
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
          stringBuilder.append("\n").append(stackTrace);
        }
      }
      else {
        stringBuilder.append(indentString).append(TIMESTAMP_FORMATTER.format(accessDateTime)).append(" @ ").append(method).append(
                !nullOrEmpty(accessMessage) ? (": " + accessMessage) : "");
      }

      return stringBuilder.toString();
    }

    /**
     * Adds a sub entry to this log entry
     * @param subEntry the sub entry to add
     */
    private void addSubEntry(final Entry subEntry) {
      this.subEntries.addLast(subEntry);
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

    private static String getStackTrace(final Throwable exception) {
      final StringWriter sw = new StringWriter();
      exception.printStackTrace(new PrintWriter(sw));

      return sw.toString();
    }
  }
}
