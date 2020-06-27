/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

import java.util.List;

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
   * @param arguments the function arguments, if any
   * @return the function return argument
   * @throws DatabaseException in case of an exception during the execution
   */
  R execute(C connection, List<T> arguments) throws DatabaseException;
}
