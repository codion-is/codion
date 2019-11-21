/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A simple logging proxy facilitating the setting of log levels
 */
public interface LoggerProxy {

  Logger LOG = LoggerFactory.getLogger(LoggerProxy.class);

  /**
   * @return the current log level
   */
  Object getLogLevel();

  /**
   * Sets the log level
   * @param logLevel the log level
   */
  void setLogLevel(final Object logLevel);

  /**
   * @return the available log levels
   */
  List getLogLevels();

  /**
   * @return the first available LoggerProxy implementation found, null if none is available
   */
  static LoggerProxy createLoggerProxy() {
    final ServiceLoader<LoggerProxy> loader = ServiceLoader.load(LoggerProxy.class);
    final Iterator<LoggerProxy> proxyIterator = loader.iterator();
    if (proxyIterator.hasNext()) {
      return proxyIterator.next();
    }

    LOG.warn("No LoggerProxy service implementation found");
    return null;
  }
}
