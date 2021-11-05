/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
  public QueryTimeoutException(final SQLException cause, final String message) {
    super(cause, message);
  }
}
