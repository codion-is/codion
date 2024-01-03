/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

/**
 * Exception used when an expected record was not found.
 */
public class RecordNotFoundException extends DatabaseException {

  /**
   * Instantiates a new RecordNotFoundException
   * @param message the exception message
   */
  public RecordNotFoundException(String message) {
    super(message);
  }
}
