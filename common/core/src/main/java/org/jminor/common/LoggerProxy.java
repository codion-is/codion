/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A simple logging proxy facilitating the setting of log levels
 */
public interface LoggerProxy {

  Logger LOG = LoggerFactory.getLogger(LoggerProxy.class);

  /**
   * Specifies the logger proxy implementation.<br>
   * Value type: String<br>
   * Default value: org.jminor.framework.plugins.logback.LogbackProxy.
   * @see LoggerProxy
   */
  Value<String> LOGGER_PROXY_IMPLEMENTATION = Configuration.stringValue("jminor.logger.proxy", "org.jminor.framework.plugins.logback.LogbackProxy");

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
   * @return the LoggerProxy implementation
   * @see LoggerProxy#LOGGER_PROXY_IMPLEMENTATION
   */
  static LoggerProxy createLoggerProxy() {
    final String loggingProxyImpl = LoggerProxy.LOGGER_PROXY_IMPLEMENTATION.get();
    try {
      return ((Class<LoggerProxy>) Class.forName(loggingProxyImpl)).newInstance();
    }
    catch (final ClassNotFoundException e) {
      LOG.warn("LoggerProxy implementation class not found: " + e.getMessage());
      return null;
    }
    catch (final InstantiationException | IllegalAccessException e) {
      LOG.error("Error while instantiating LoggerProxy", e);

      throw new RuntimeException(e);
    }
  }
}
