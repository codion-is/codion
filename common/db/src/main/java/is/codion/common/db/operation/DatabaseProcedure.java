/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

import java.util.List;

/**
 * A database procedure
 * @param <C> the connection type required by this procedure
 * @param <T> the procedure argument type
 */
public interface DatabaseProcedure<C, T> {

  /**
   * Executes this procedure with the given connection
   * @param connection the connection being used to execute this procedure
   * @param arguments the procedure arguments, if any
   * @throws DatabaseException in case of an exception during the execution
   */
  void execute(C connection, List<T> arguments) throws DatabaseException;
}
