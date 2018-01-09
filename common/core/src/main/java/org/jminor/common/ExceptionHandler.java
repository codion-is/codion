/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
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
