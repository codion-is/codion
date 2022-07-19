/*
 * Copyright (c) 2015 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

/**
 * An exception indicating a failed update operation
 */
public class UpdateException extends DatabaseException {

  /**
   * Instantiates a new {@link UpdateException}
   * @param message the message
   */
  public UpdateException(String message) {
    super(message);
  }
}
