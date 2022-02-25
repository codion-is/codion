/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.log4j;

import is.codion.common.logging.LoggerProxy;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * A Log4j LoggerProxy implementation
 */
public final class Log4jProxy implements LoggerProxy {

  @Override
  public Object getLogLevel() {
    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    LoggerConfig loggerConfig = context.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

    return loggerConfig.getLevel();
  }

  @Override
  public void setLogLevel(final Object logLevel) {
    if (!(logLevel instanceof Level)) {
      throw new IllegalArgumentException("logLevel should be of type " + Level.class.getName());
    }
    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    LoggerConfig loggerConfig = context.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    loggerConfig.setLevel((Level) logLevel);
    context.updateLoggers();
  }

  @Override
  public List<Object> getLogLevels() {
    return asList(Level.OFF, Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL);
  }
}
