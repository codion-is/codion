/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

/**
 * A database function
 * @param <C> the connection type required by this function
 * @param <T> the argument type
 * @param <R> the return type
 */
public interface DatabaseFunction<C, T, R> {

  /**
   * Executes this function with the given connection
   * @param connection the connection being used to execute this function
   * @param argument the function argument, if any
   * @return the function return argument
   * @throws DatabaseException in case of an exception during the execution
   */
  R execute(C connection, T argument) throws DatabaseException;
}
