/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.log4j;

import is.codion.common.logging.LoggerProxy;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
  public void setLogLevel(Object logLevel) {
    if (!(logLevel instanceof Level)) {
      throw new IllegalArgumentException("logLevel should be of type " + Level.class.getName());
    }
    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    LoggerConfig loggerConfig = context.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    loggerConfig.setLevel((Level) logLevel);
    context.updateLoggers();
  }

  @Override
  public List<Object> levels() {
    return asList(Level.OFF, Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL);
  }

  @Override
  public Collection<String> files() {
    Map<String, Appender> appenderMap = ((Logger) LogManager.getLogger()).getAppenders();

    return appenderMap.values().stream()
            .filter(RollingFileAppender.class::isInstance)
            .map(RollingFileAppender.class::cast)
            .map(RollingFileAppender::getFileName)
            .collect(Collectors.toList());
  }
}
