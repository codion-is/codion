/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.operation;

import dev.codion.common.db.exception.DatabaseException;

/**
 * A database procedure
 * @param <C> the connection type required by this procedure
 */
public interface DatabaseProcedure<C> extends DatabaseOperation {

  /**
   * Executes this procedure with the given connection
   * @param connection the connection to use when executing
   * @param arguments the procedure arguments, if any
   * @throws DatabaseException in case of an exception during the execution
   */
  void execute(C connection, Object... arguments) throws DatabaseException;
}
