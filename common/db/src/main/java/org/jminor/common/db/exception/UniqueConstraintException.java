/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

import java.sql.SQLException;

/**
 * An exception indication a unique constraint failure
 */
public final class UniqueConstraintException extends DatabaseException {

  /**
   * Instantiates a new {@link UniqueConstraintException}
   * @param cause the underlying cause
   * @param message the error message
   */
  public UniqueConstraintException(final SQLException cause, final String message) {
    super(cause, message);
  }
}
