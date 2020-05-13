/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

/**
 * Exception used when an expected record was not found.
 */
public class RecordNotFoundException extends DatabaseException {

  /**
   * Instantiates a new RecordNotFoundException
   * @param message the exception message
   */
  public RecordNotFoundException(final String message) {
    super(message);
  }
}
