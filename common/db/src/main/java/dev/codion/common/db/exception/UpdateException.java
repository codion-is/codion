/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.exception;

/**
 * An exception indicating a failed update operation
 */
public class UpdateException extends DatabaseException {

  /**
   * Instantiates a new {@link UpdateException}
   * @param message the message
   */
  public UpdateException(final String message) {
    super(message);
  }
}
