/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.tools;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Util;
import org.jminor.common.model.formats.DateFormats;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A method call logger allowing logging of nested method calls.
 * TODO this class should be able to handle/recover from incorrect usage, not crash the application
 */
public final class MethodLogger {

  /**
   * Provides String representations of method arguments
   */
  public interface ArgumentStringProvider {

    /**
     * @param argument the argument
     * @return a String representation of the given argument
     */
    String toString(final Object argument);

    /**
     * @param arguments the arguments
     * @return a String representation of the given arguments array
     */
    String toString(final Object[] arguments);
  }

  private final Deque<Entry> callStack = new LinkedList<>();
  private final LinkedList<Entry> entries = new LinkedList<>();
  private final ArgumentStringProvider argumentStringProvider;
  private final int maxSize;

  private boolean enabled = false;

  /**
   * Instantiates a new MethodLogger.
   * @param maxSize the maximum log size
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
  public MethodLogger(final int maxSize, final boolean enabled, final ArgumentStringProvider argumentStringProvider) {
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
      final String accessMessage = argumentStringProvider.toString(arguments);
      final Entry entry = new Entry(method, accessMessage);
      callStack.push(entry);
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
   * @return the Entry
   */
  public synchronized Entry logExit(final String method, final Throwable exception, final String exitMessage) {
    if (enabled) {
      if (callStack.isEmpty()) {
        throw new IllegalStateException("Call stack is empty when trying to log method exit");
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

    return null;
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
   * @return the number of log entries
   */
  public synchronized int size() {
    return entries.size();
  }

  /**
   * @param index the index
   * @return the entry at the given index
   */
  public synchronized Entry getEntryAt(final int index) {
    return entries.get(index);
  }

  /**
   * @return the last log entry
   */
  public synchronized Entry getLastEntry() {
    return entries.getLast();
  }

  /**
   * @return the first log entry
   */
  public synchronized Entry getFirstEntry() {
    return entries.getFirst();
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
      final List<Entry> subLog = entry.getSubLog();
      appendLogEntries(log, subLog, indentationLevel + 1);
    }
  }

  /**
   * Appends  the given log entries to the log
   * @param log the log
   * @param logger the log containing the entries to append
   * @param indentationLevel the indentation to use for the given log entries
   */
  public static void appendLogEntries(final StringBuilder log, final List<Entry> logger, final int indentationLevel) {
    if (logger != null) {
      for (final MethodLogger.Entry logEntry : logger) {
        log.append(logEntry.toString(indentationLevel)).append("\n");
        final List<Entry> subLog = logEntry.getSubLog();
        appendLogEntries(log, subLog, indentationLevel + 1);
      }
    }
  }

  /**
   * A default {@link ArgumentStringProvider} implementation based on {@link String#valueOf(Object)}
   */
  public static class DefaultArgumentStringProvider implements ArgumentStringProvider {

    private static final int CHAR_PER_ARGUMENT = 40;

    /** {@inheritDoc} */
    @Override
    public String toString(final Object argument) {
      return String.valueOf(argument);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString(final Object[] arguments) {
      if (arguments == null || arguments.length == 0) {
        return "";
      }

      final StringBuilder stringBuilder = new StringBuilder(arguments.length * CHAR_PER_ARGUMENT);
      for (int i = 0; i < arguments.length; i++) {
        stringBuilder.append(toString(arguments[i]));
        if (i < arguments.length-1) {
          stringBuilder.append(", ");
        }
      }

      return stringBuilder.toString();
    }
  }

  /**
   * A log entry
   */
  public static final class Entry implements Serializable {

    private static final long serialVersionUID = 1;
    private static final ThreadLocal<DateFormat> TIMESTAMP_FORMAT = DateUtil.getThreadLocalDateFormat(DateFormats.EXACT_TIMESTAMP);
    private static final NumberFormat MICROSECONDS_FORMAT = NumberFormat.getIntegerInstance();

    private LinkedList<Entry> subEntries = new LinkedList<>();
    private String method;
    private String accessMessage;
    private long accessTime;
    private long accessTimeNano;
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
      return subEntries.size() > 0;
    }

    /**
     * @return the sub log entries
     */
    public List<Entry> getSubLog() {
      return Collections.unmodifiableList(subEntries);
    }

    /**
     * Adds a sub entry to this log entry
     * @param subEntry the sub entry to add
     */
    public void addSubEntry(final Entry subEntry) {
      this.subEntries.addLast(subEntry);
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
     * Sets the exit time using the current time
     */
    public void setExitTime() {
      setExitTime(System.currentTimeMillis(), System.nanoTime());
    }

    /**
     * @param exitTime the exit time
     * @param exitTimeNano the exit time in nanoseconds
     */
    public void setExitTime(final long exitTime, final long exitTimeNano) {
      this.exitTime = exitTime;
      this.exitTimeNano = exitTimeNano;
    }

    /**
     * @param exception the exception that occurred during the method call logged by this entry
     */
    public void setException(final Throwable exception) {
      this.stackTrace = getStackTrace(exception);
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
     * @param exitMessage the exit message
     */
    public void setExitMessage(final String exitMessage) {
      this.exitMessage = exitMessage;
    }

    /**
     * Returns the duration of the method call this entry represents,
     * this value is 0 or undefined until <code>setExitTime()</code>
     * has been called, this can be checked via <code>isComplete()</code>.
     * @return the duration of the method call this entry represents
     */
    public long getDelta() {
      return exitTime - accessTime;
    }

    /**
     * Returns the duration of the method call this entry represents in nanoseconds,
     * this value is 0 or undefined until <code>setExitTime()</code>
     * has been called, this can be checked via <code>isComplete()</code>.
     * @return the duration of the method call this entry represents
     */
    public long getDeltaNano() {
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
      final String indentString = indentation > 0 ? Util.padString("", indentation, '\t', false) : "";
      final StringBuilder stringBuilder = new StringBuilder();
      final DateFormat timestampFormat = TIMESTAMP_FORMAT.get();
      if (isComplete()) {
        stringBuilder.append(indentString).append(timestampFormat.format(accessTime)).append(" @ ").append(method).append(
                !Util.nullOrEmpty(accessMessage) ? (": " + accessMessage) : "").append("\n");
        stringBuilder.append(indentString).append(timestampFormat.format(exitTime)).append(" > ")
                .append(MICROSECONDS_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(getDeltaNano()))).append(" μs")
                .append(exitMessage == null ? "" : " (" + exitMessage + ")");
        if (stackTrace != null) {
          stringBuilder.append("\n").append(stackTrace);
        }
      }
      else {
        stringBuilder.append(indentString).append(timestampFormat.format(accessTime)).append(" @ ").append(method).append(
                !Util.nullOrEmpty(accessMessage) ? (": " + accessMessage) : "");
      }

      return stringBuilder.toString();
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(method);
      stream.writeObject(accessMessage);
      stream.writeObject(exitMessage);
      stream.writeLong(accessTime);
      stream.writeLong(exitTime);
      stream.writeLong(accessTimeNano);
      stream.writeLong(exitTimeNano);
      stream.writeObject(stackTrace);
      stream.writeInt(subEntries.size());
      for (final Entry subEntry : subEntries) {
        stream.writeObject(subEntry);
      }
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
      this.method = (String) stream.readObject();
      this.accessMessage = (String) stream.readObject();
      this.exitMessage = (String) stream.readObject();
      this.accessTime = stream.readLong();
      this.exitTime = stream.readLong();
      this.accessTimeNano = stream.readLong();
      this.exitTimeNano = stream.readLong();
      this.stackTrace = (String) stream.readObject();
      final int subLogSize = stream.readInt();
      this.subEntries = new LinkedList<>();
      for (int i = 0; i < subLogSize; i++) {
        this.subEntries.addLast((Entry) stream.readObject());
      }
    }

    private static String getStackTrace(final Throwable exception) {
      if (exception == null) {
        return null;
      }
      final StringWriter sw = new StringWriter();
      exception.printStackTrace(new PrintWriter(sw));

      return sw.toString();
    }
  }
}
