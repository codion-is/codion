/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;

/**
 * Utility class for exception logging
 */
public final class ExceptionUtil {

  private ExceptionUtil() {}

  public static Exception unwrapAndLog(final Exception exception, final Class<? extends Exception> wrappingExceptionClass,
                                       final Logger logger) {
    return unwrapAndLog(exception, wrappingExceptionClass, logger, Collections.<Class<? extends Exception>>emptyList());
  }

  public static Exception unwrapAndLog(final Exception exception, final Class<? extends Exception> wrappingExceptionClass,
                                       final Logger logger, final Collection<Class<? extends Exception>> dontLog) {
    if (exception.getCause() instanceof Exception) {//else we can't really unwrap it
      if (wrappingExceptionClass.equals(exception.getClass())) {
        return unwrapAndLog((Exception) exception.getCause(), wrappingExceptionClass, logger);
      }

      if (dontLog != null && dontLog.contains(exception.getClass())) {
        return exception;
      }
    }
    if (logger != null) {
      logger.error(exception.getMessage(), exception);
    }

    return exception;
  }
}
