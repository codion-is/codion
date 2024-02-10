/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

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
