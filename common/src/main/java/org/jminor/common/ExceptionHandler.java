/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * Handles exceptions
 */
public interface ExceptionHandler {

  /**
   * Handles the given exception
   * @param throwable the exception to handle
   */
  void handleException(final Throwable throwable);
}
