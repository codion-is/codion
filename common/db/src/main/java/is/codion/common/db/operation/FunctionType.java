/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
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
