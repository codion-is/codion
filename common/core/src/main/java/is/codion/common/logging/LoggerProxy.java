/*
 * Copyright (c) 2017 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.logging;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A logging proxy facilitating the setting of log levels
 */
public interface LoggerProxy {

  /**
   * The null logger proxy instance, zero functionality.
   */
  LoggerProxy NULL_PROXY = new LoggerProxy() {

    private static final String NO_LOG_LEVEL = "NULL";

    @Override
    public Object getLogLevel() {
      return NO_LOG_LEVEL;
    }

    @Override
    public void setLogLevel(Object logLevel) {/*no op*/}

    @Override
    public List<Object> getLogLevels() {
      return Collections.emptyList();
    }
  };

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
   * @return the first available LoggerProxy implementation found, {@link #NULL_PROXY} if none is available.
   */
  static LoggerProxy loggerProxy() {
    ServiceLoader<LoggerProxy> loader = ServiceLoader.load(LoggerProxy.class);
    Iterator<LoggerProxy> proxyIterator = loader.iterator();
    if (proxyIterator.hasNext()) {
      return proxyIterator.next();
    }

    System.err.println("No LoggerProxy service implementation found");
    return NULL_PROXY;
  }
}
