/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.logging;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * A method call logger allowing logging of nested method calls.
 * @see #methodLogger(int)
 * @see #methodLogger(int, Function)
 */
public interface MethodLogger {

  /**
   * @param method the method being accessed
   */
  void logAccess(String method);

  /**
   * @param method the method being accessed
   * @param argument the method argument, can be an Object, a collection or an array
   */
  void logAccess(String method, Object argument);

  /**
   * @param method the method being exited
   * @return the Entry
   */
  Entry logExit(String method);

  /**
   * @param method the method being exited
   * @param exception the exception, if any
   * @return the Entry
   */
  Entry logExit(String method, Throwable exception);

  /**
   * @param method the method being exited
   * @param exception the exception, if any
   * @param exitMessage the message to associate with exiting the method
   * @return the Entry, or null if this logger is not enabled
   */
  Entry logExit(String method, Throwable exception, String exitMessage);

  /**
   * @return true if this logger is enabled
   */
  boolean isEnabled();

  /**
   * @param enabled true to enable this logger
   */
  void setEnabled(boolean enabled);

  /**
   * @return an unmodifiable view of the log entries
   */
  List<Entry> getEntries();

  /**
   * Instantiates a new MethodLogger, disabled by default.
   * @param maxSize the maximum log size
   * @return a new MethodLogger instance
   */
  static MethodLogger methodLogger(int maxSize) {
    return methodLogger(maxSize, new ArgumentToString());
  }

  /**
   * Instantiates a new MethodLogger, disabled by default.
   * @param maxSize the maximum log size
   * @param argumentStringProvider responsible for providing String representations of method arguments
   * @return a new MethodLogger instance
   */
  static MethodLogger methodLogger(int maxSize, Function<Object, String> argumentStringProvider) {
    return new DefaultMethodLogger(maxSize, argumentStringProvider);
  }

  /**
   * A method logger entry.
   */
  interface Entry {

    /**
     * @return true if this log entry contains child log entries
     */
    boolean hasChildEntries();

    /**
     * @return the child log entries
     */
    List<Entry> getChildEntries();

    /**
     * @return the name of the method logged by this entry
     */
    String getMethod();

    /**
     * @return true if the exit time has been set for this entry
     */
    boolean isComplete();

    /**
     * @return the method access time
     */
    long getAccessTime();

    /**
     * @return the exit time
     */
    long getExitTime();

    /**
     * @return the access message
     */
    String getAccessMessage();

    /**
     * Returns the duration of the method call this entry represents in nanoseconds,
     * this value is 0 or undefined until the exit time has been set.
     * This can be checked via {@code isComplete()}.
     * @return the duration of the method call this entry represents
     */
    long getDuration();

    /**
     * Appends this logger entry along with any child-entries to the given StringBuilder.
     * @param builder the StringBuilder to append to.
     */
    void append(StringBuilder builder);

    /**
     * Returns a string representation of this log entry.
     * @param indentation the number of tab indents to prefix the string with
     * @return a string representation of this log entry
     */
    String toString(int indentation);
  }

  /**
   * Provides String representations of method arguments.
   */
  class ArgumentToString implements Function<Object, String> {

    private static final String BRACKET_OPEN = "[";
    private static final String BRACKET_CLOSE = "]";

    @Override
    public final String apply(Object object) {
      return toString(object);
    }

    /**
     * Returns a String representation of the given object.
     * @param object the object
     * @return a String representation of the given object
     */
    protected String toString(Object object) {
      if (object == null) {
        return "";
      }
      if (object instanceof List) {
        return toString((List<?>) object);
      }
      if (object instanceof Collection) {
        return toString((Collection<?>) object);
      }
      if (object instanceof byte[]) {
        return "byte[" + ((byte[]) object).length + "]";
      }
      if (object.getClass().isArray()) {
        return toString((Object[]) object);
      }

      return object.toString();
    }

    private String toString(List<?> arguments) {
      if (arguments.isEmpty()) {
        return "";
      }
      if (arguments.size() == 1) {
        return toString(arguments.get(0));
      }

      return arguments.stream()
              .map(this::toString)
              .collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
    }

    private String toString(Collection<?> arguments) {
      if (arguments.isEmpty()) {
        return "";
      }

      return arguments.stream()
              .map(this::toString)
              .collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
    }

    private String toString(Object[] arguments) {
      if (arguments.length == 0) {
        return "";
      }
      if (arguments.length == 1) {
        return toString(arguments[0]);
      }

      return stream(arguments)
              .map(this::toString)
              .collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
    }
  }
}
