/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.logging;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A simple logging proxy facilitating the setting of log levels
 */
public interface LoggerProxy {

  /**
   * @return the current log level
   */
  Object getLogLevel();

  /**
   * Sets the log level
   * @param logLevel the log level
   */
  void setLogLevel(Object logLevel);

  /**
   * @return the available log levels
   */
  List<Object> getLogLevels();

  /**
   * @return the first available LoggerProxy implementation found, null if none is available
   */
  static LoggerProxy createLoggerProxy() {
    final ServiceLoader<LoggerProxy> loader = ServiceLoader.load(LoggerProxy.class);
    final Iterator<LoggerProxy> proxyIterator = loader.iterator();
    if (proxyIterator.hasNext()) {
      return proxyIterator.next();
    }

    System.err.println("No LoggerProxy service implementation found");
    return null;
  }
}
