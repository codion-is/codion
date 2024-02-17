/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

import is.codion.common.db.database.Database.Operation;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * An exception indication a referential integrity failure
 */
public final class ReferentialIntegrityException extends DatabaseException {

  private final Operation operation;

  /**
   * Instantiates a new {@link ReferentialIntegrityException}
   * @param cause the underlying cause
   * @param message the error message
   * @param operation the operation causing this exception
   */
  public ReferentialIntegrityException(SQLException cause, String message, Operation operation) {
    super(cause, message);
    this.operation = requireNonNull(operation);
  }

  /**
   * @return the {@link Operation} causing this exception
   */
  public Operation operation() {
    return operation;
  }
}
