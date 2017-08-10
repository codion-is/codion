/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.logback;

import org.jminor.common.LoggerProxy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * A Logback LoggerProxy implementation
 */
public final class LogbackProxy implements LoggerProxy {

  /** {@inheritDoc} */
  @Override
  public Object getLogLevel() {
    return ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).getLevel();
  }

  /** {@inheritDoc} */
  @Override
  public void setLogLevel(final Object logLevel) {
    if (!(logLevel instanceof Level)) {
      throw new IllegalArgumentException("logLevel should be of type "  + Level.class.getName());
    }
    ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel((Level) logLevel);
  }

    /** {@inheritDoc} */
  @Override
  public List getLogLevels() {
    return Arrays.asList(Level.OFF, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR);
  }
}
