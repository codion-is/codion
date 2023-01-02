/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.logback;

import is.codion.common.logging.LoggerProxy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * A Logback LoggerProxy implementation
 */
public final class LogbackProxy implements LoggerProxy {

  @Override
  public Object getLogLevel() {
    return ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).getLevel();
  }

  @Override
  public void setLogLevel(Object logLevel) {
    if (!(logLevel instanceof Level)) {
      throw new IllegalArgumentException("logLevel should be of type " + Level.class.getName());
    }
    ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel((Level) logLevel);
  }

  @Override
  public List<Object> logLevels() {
    return asList(Level.OFF, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR);
  }
}
