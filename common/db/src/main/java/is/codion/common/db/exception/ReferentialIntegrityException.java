/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

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
  public ReferentialIntegrityException(SQLException cause, String message) {
    super(cause, message);
  }
}
