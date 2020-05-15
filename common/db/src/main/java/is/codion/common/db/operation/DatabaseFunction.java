/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

/**
 * A database function
 * @param <C> the connection type required by this function
 * @param <T> the result type
 */
public interface DatabaseFunction<C, T> extends DatabaseOperation {

  /**
   * Executes this function with the given connection
   * @param connection the connection to use when executing
   * @param arguments the function arguments, if any
   * @return the function return argument
   * @throws DatabaseException in case of an exception during the execution
   */
  T execute(C connection, Object... arguments) throws DatabaseException;
}
