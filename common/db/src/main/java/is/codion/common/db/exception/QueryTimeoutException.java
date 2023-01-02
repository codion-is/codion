/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

import java.sql.SQLException;

/**
 * Exception thrown when a statement has timed out or been cancelled.
 */
public final class QueryTimeoutException extends DatabaseException {

  /**
   * Instantiates a new {@link QueryTimeoutException}
   * @param cause the underlying cause
   * @param message the error message
   */
  public QueryTimeoutException(SQLException cause, String message) {
    super(cause, message);
  }
}
