/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

/**
 * A database procedure
 * @param <C> the connection type required by this procedure
 * @param <T> the procedure argument type
 */
public interface DatabaseProcedure<C, T> {

  /**
   * Executes this procedure using the given connection
   * @param connection the connection to use
   * @param argument the procedure argument, if any
   * @throws DatabaseException in case of an exception during the execution
   */
  void execute(C connection, T argument) throws DatabaseException;
}
