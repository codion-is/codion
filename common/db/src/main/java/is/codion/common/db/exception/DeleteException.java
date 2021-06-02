/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

/**
 * An exception indicating a failed delete operation
 */
public final class DeleteException extends DatabaseException {

  /**
   * Instantiates a new {@link DeleteException}
   * @param message the message
   */
  public DeleteException(final String message) {
    super(message);
  }
}
