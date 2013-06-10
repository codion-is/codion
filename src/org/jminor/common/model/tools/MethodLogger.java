/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * A cyclical method call logger allowing nested logging of method calls.
 * TODO this class should be able to handle/recover from incorrect usage, not crash the application
 */
public class MethodLogger implements Serializable {

  private static final long serialVersionUID = 1;

  private final transient Stack<Entry> callStack = new Stack<Entry>();
  private LinkedList<Entry> entries = new LinkedList<Entry>();
  private int maxSize;
  private boolean enabled = false;
  private long lastAccessTime = System.currentTimeMillis();
  private long lastExitTime = System.currentTimeMillis();
  private String lastAccessedMethod;
  private String lastAccessMessage;
  private String lastExitedMethod;

  /**
   * Instantiates a new MethodLogger.
   * @param maxSize the maximum log size
   */
  public MethodLogger(final int maxSize) {
    this(maxSize, false);
  }

  /**
   * Instantiates a new MethodLogger.
   * @param maxSize the maximum log size
   * @param enabled true if this logger should be enabled
   */
  public MethodLogger(final int maxSize, final boolean enabled) {
    this.maxSize = maxSize;
    this.enabled = enabled;
  }

  /**
   * @param method the method being accessed
   */
  public final void logAccess(final String method) {
    logAccess(method, null);
  }

  /**
   * @param method the method being accessed
   * @param arguments the method arguments
   */
  public final synchronized void logAccess(final String method, final Object[] arguments) {
    if (shouldMethodBeLogged(method)) {
      final String accessMessage = argumentArrayToString(arguments);
      if (enabled) {
        final Entry entry = new Entry(method, accessMessage);
        setLastAccessInfo(method, entry.getAccessTime(), entry.getAccessMessage());
        callStack.push(entry);
      }
      else {
        setLastAccessInfo(method, System.currentTimeMillis(), accessMessage);
      }
    }
  }

  /**
   * @param method the method being exited
   * @return the Entry
   */
  public final Entry logExit(final String method) {
    return logExit(method, null);
  }

  /**
   * @param method the method being exited
   * @param exception the exception, if any
   * @return the Entry
   */
  public final Entry logExit(final String method, final Throwable exception) {
    return logExit(method, exception, null);
  }

  /**
   * @param method the method being exited
   * @param exception the exception, if any
   * @param exitMessage the message to associate with exiting the method
   * @return the Entry
   */
  public final synchronized Entry logExit(final String method, final Throwable exception, final String exitMessage) {
    if (shouldMethodBeLogged(method)) {
      if (enabled) {
        if (callStack.isEmpty()) {
          throw new IllegalStateException("Call stack is empty when trying to log method exit");
        }
        final Entry entry = callStack.pop();
        if (!entry.getMethod().equals(method)) {
          throw new IllegalStateException("Expecting method " + entry.getMethod() + " but got " + method + " when trying to log method exit");
        }
        entry.setExitTime();
        entry.setException(exception);
        entry.setExitMessage(exitMessage);
        setLastExitInfo(method, entry.getExitTime());
        if (callStack.empty()) {
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
      else {
        setLastExitInfo(method, System.currentTimeMillis());
      }
    }

    return null;
  }

  /**
   * @return true if this logger is enabled
   */
  public final synchronized boolean isEnabled() {
    return enabled;
  }

  /**
   * @param enabled true to enable this logger
   */
  public final synchronized void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      entries.clear();
      callStack.clear();
    }
  }

  /**
   * @return the number of log entries
   */
  public final synchronized int size() {
    return entries.size();
  }

  /**
   * @param index the index
   * @return the entry at the given index
   */
  public final synchronized Entry getEntryAt(final int index) {
    return entries.get(index);
  }

  /**
   * @return the last log entry
   */
  public final synchronized Entry getLastEntry() {
    return entries.getLast();
  }

  /**
   * @return the first log entry
   */
  public final synchronized Entry getFirstEntry() {
    return entries.getFirst();
  }

  /**
   * @return the time of last access
   */
  public final long getLastAccessTime() {
    return lastAccessTime;
  }

  /**
   * @return the last accessed method
   */
  public final String getLastAccessedMethod() {
    return lastAccessedMethod;
  }

  /**
   * @return the last access message
   */
  public final String getLastAccessMessage() {
    return lastAccessMessage;
  }

  /**
   * @return the last exit message
   */
  public final long getLastExitTime() {
    return lastExitTime;
  }

  /**
   * @return the last exited method
   */
  public final String getLastExitedMethod() {
    return lastExitedMethod;
  }

  /**
   * @return an unmodifiable view of the log entries
   */
  public final synchronized List<Entry> getEntries() {
    return Collections.unmodifiableList(entries);
  }

  /**
   * Override to exclude certain methods from being logged
   * @param method the method
   * @return true if the given method should be logged
   */
  protected boolean shouldMethodBeLogged(final String method) {
    return true;
  }

  /**
   * Override to provide specific string representations of method arguments
   * @param argument the argument
   * @return a String representation of the given argument
   */
  protected String getMethodArgumentAsString(final Object argument) {
    return String.valueOf(argument);
  }

  protected final String argumentArrayToString(final Object[] arguments) {
    if (arguments == null) {
      return "";
    }

    final StringBuilder stringBuilder = new StringBuilder(arguments.length*40);
    for (int i = 0; i < arguments.length; i++) {
      stringBuilder.append(getMethodArgumentAsString(arguments[i]));
      if (i < arguments.length-1) {
        stringBuilder.append(", ");
      }
    }

    return stringBuilder.toString();
  }

  private void setLastAccessInfo(final String method, final long accessTime, final String accessMessage) {
    lastAccessTime = accessTime;
    lastAccessedMethod = method;
    lastAccessMessage = accessMessage;
  }

  private void setLastExitInfo(final String method, final long exitTime) {
    lastExitedMethod = method;
    lastExitTime = exitTime;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeInt(maxSize);
    stream.writeBoolean(enabled);
    stream.writeLong(lastAccessTime);
    stream.writeLong(lastExitTime);
    stream.writeObject(lastAccessedMethod);
    stream.writeObject(lastAccessMessage);
    stream.writeObject(lastExitedMethod);
    stream.writeInt(entries.size());
    for (final Entry entry : entries) {
      stream.writeObject(entry);
    }
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    this.maxSize = stream.readInt();
    this.enabled = stream.readBoolean();
    this.lastAccessTime = stream.readLong();
    this.lastExitTime = stream.readLong();
    this.lastAccessedMethod = (String) stream.readObject();
    this.lastAccessMessage = (String) stream.readObject();
    this.lastExitedMethod = (String) stream.readObject();
    this.entries = new LinkedList<Entry>();
    final int entryCount = stream.readInt();
    for (int i = 0; i < entryCount; i++) {
      entries.add((Entry) stream.readObject());
    }
  }

  /**
   * A log entry
   */
  public static final class Entry implements Serializable {

    private static final long serialVersionUID = 1;
    private static final ThreadLocal<DateFormat> TIMESTAMP_FORMAT = DateUtil.getThreadLocalDateFormat(DateFormats.EXACT_TIMESTAMP);

    private String method;
    private String accessMessage;
    private long accessTime;
    private long accessTimeNano;
    private String exitMessage;
    private long exitTime;
    private long exitTimeNano;
    private String stackTrace;
    private LinkedList<Entry> subEntries = new LinkedList<Entry>();

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
      if (isComplete()) {
        stringBuilder.append(indentString).append(TIMESTAMP_FORMAT.get().format(accessTime)).append(" @ ").append(method).append(
                !Util.nullOrEmpty(accessMessage) ? (": " + accessMessage) : "").append("\n");
        stringBuilder.append(indentString).append(TIMESTAMP_FORMAT.get().format(exitTime)).append(" > ").append(getDelta()).append(" ms")
                .append(exitMessage == null ? "" : " (" + exitMessage + ")");
        if (stackTrace != null) {
          stringBuilder.append("\n").append(stackTrace);
        }
      }
      else {
        stringBuilder.append(indentString).append(TIMESTAMP_FORMAT.get().format(accessTime)).append(" @ ").append(method).append(
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
      this.subEntries = new LinkedList<Entry>();
      final int subLogSize = stream.readInt();
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
