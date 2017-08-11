/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.log4j;

import org.jminor.common.LoggerProxy;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Arrays;
import java.util.List;

/**
 * A Log4j LoggerProxy implementation
 */
public final class Log4jProxy implements LoggerProxy {

  /** {@inheritDoc} */
  @Override
  public Object getLogLevel() {
    final LoggerContext context = (LoggerContext) LogManager.getContext(false);
    final LoggerConfig loggerConfig = context.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

    return loggerConfig.getLevel();
  }

  /** {@inheritDoc} */
  @Override
  public void setLogLevel(final Object logLevel) {
    if (!(logLevel instanceof Level)) {
      throw new IllegalArgumentException("logLevel should be of type "  + Level.class.getName());
    }
    final LoggerContext context = (LoggerContext) LogManager.getContext(false);
    final LoggerConfig loggerConfig = context.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    loggerConfig.setLevel((Level) logLevel);
    context.updateLoggers();
  }

  /** {@inheritDoc} */
  @Override
  public List getLogLevels() {
    return Arrays.asList(Level.OFF, Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL);
  }
}
