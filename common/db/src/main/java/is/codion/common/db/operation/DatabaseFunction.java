/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

/**
 * A database function
 * @param <C> the connection type required by this function
 * @param <T> the argument type
 * @param <R> the return type
 */
public interface DatabaseFunction<C, T, R> extends DatabaseOperation {

 /**
   * @return this functions type
   */
  @Override
  FunctionType<C, T, R> getType();

  /**
   * Executes this function with the given connection
   * @param connection the connection to use when executing
   * @param arguments the function arguments, if any
   * @return the function return argument
   * @throws DatabaseException in case of an exception during the execution
   */
  R execute(C connection, T... arguments) throws DatabaseException;
}
