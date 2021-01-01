/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

import java.util.List;

/**
 * @param <C> the connection type
 * @param <T> the function argument type
 * @param <R> the function result type
 */
public interface FunctionType<C, T, R> {

  /**
   * @return the function name
   */
  String getName();

  /**
   * Executes the given function.
   * @param connection the connection being used
   * @param function the function to execute
   * @param arguments the function arguments, if any
   * @return the function result
   * @throws DatabaseException in case of an exception
   */
  R execute(C connection, DatabaseFunction<C, T, R> function, List<T>  arguments) throws DatabaseException;

  /**
   * Creates a {@link FunctionType} with the given name and types.
   * @param name
   * @param <C> the connection type
   * @param <T> the function argument type
   * @param <R> the function result type
   * @return a new {@link FunctionType}
   */
  static <C, T, R> FunctionType<C, T, R> functionType(final String name) {
    return new DefaultFunctionType<>(name);
  }
}
