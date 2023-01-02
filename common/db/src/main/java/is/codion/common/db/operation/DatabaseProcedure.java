/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
   * Executes this procedure with the given connection
   * @param connection the connection being used to execute this procedure
   * @param argument the procedure argument, if any
   * @throws DatabaseException in case of an exception during the execution
   */
  void execute(C connection, T argument) throws DatabaseException;
}
