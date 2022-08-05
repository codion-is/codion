/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

/**
 * @param <C> the connection type
 * @param <T> the function argument type
 * @param <R> the function result type
 */
public interface FunctionType<C, T, R> {

  /**
   * @return the function name
   */
  String name();

  /**
   * Executes the given function.
   * @param connection the connection being used
   * @param function the function to execute
   * @param argument the function argument, if any
   * @return the function result
   * @throws DatabaseException in case of an exception
   */
  R execute(C connection, DatabaseFunction<C, T, R> function, T argument) throws DatabaseException;

  /**
   * Creates a {@link FunctionType} with the given name and types.
   * @param name the name
   * @param <C> the connection type
   * @param <T> the function argument type
   * @param <R> the function result type
   * @return a new {@link FunctionType}
   */
  static <C, T, R> FunctionType<C, T, R> functionType(String name) {
    return new DefaultFunctionType<>(name);
  }
}
