/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.exception;

import java.sql.SQLException;

/**
 * An exception indication a referential integrity failure
 */
public final class ReferentialIntegrityException extends DatabaseException {

  /**
   * Instantiates a new {@link ReferentialIntegrityException}
   * @param cause the underlying cause
   * @param message the error message
   */
  public ReferentialIntegrityException(final SQLException cause, final String message) {
    super(cause, message);
  }
}
