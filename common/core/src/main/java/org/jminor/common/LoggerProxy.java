/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;

/**
 * A simple logging proxy facilitating the setting of log levels
 */
public interface LoggerProxy {

  Logger LOG = LoggerFactory.getLogger(LoggerProxy.class);

  /**
   * Specifies the logger proxy implementation.<br>
   * Value type: String<br>
   * Default value: org.jminor.plugin.logback.LogbackProxy.
   * @see LoggerProxy
   */
  PropertyValue<String> LOGGER_PROXY_IMPLEMENTATION = Configuration.stringValue("jminor.logger.proxy", "org.jminor.plugin.logback.LogbackProxy");

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
   * @return the LoggerProxy implementation, null if none is found
   * @see LoggerProxy#LOGGER_PROXY_IMPLEMENTATION
   */
  static LoggerProxy createLoggerProxy() {
    final String loggingProxyImpl = LoggerProxy.LOGGER_PROXY_IMPLEMENTATION.get();
    final ServiceLoader<LoggerProxy> loader = ServiceLoader.load(LoggerProxy.class);
    for (final LoggerProxy provider : loader) {
      if (provider.getClass().getName().equals(loggingProxyImpl)) {
        return provider;
      }
    }

    LOG.warn("No LoggerProxy service implementation of type: " + loggingProxyImpl + " found");
    return null;
  }
}
