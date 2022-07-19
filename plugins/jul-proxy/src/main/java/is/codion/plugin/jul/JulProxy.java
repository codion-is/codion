/*
 * Copyright (c) 2017 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jul;

import is.codion.common.logging.LoggerProxy;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

/**
 * A java.util.logging LoggerProxy implementation
 */
public final class JulProxy implements LoggerProxy {

  @Override
  public Object getLogLevel() {
    return LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME).getLevel();
  }

  @Override
  public void setLogLevel(Object logLevel) {
    if (!(logLevel instanceof Level)) {
      throw new IllegalArgumentException("logLevel should be of type " + Level.class.getName());
    }
    LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel((Level) logLevel);
  }

  @Override
  public List<Object> getLogLevels() {
    return asList(Level.ALL, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.OFF);
  }
}
