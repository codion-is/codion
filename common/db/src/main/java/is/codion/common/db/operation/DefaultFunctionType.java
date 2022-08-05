/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

final class DefaultFunctionType<C, T, R> implements FunctionType<C, T, R>, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;

  DefaultFunctionType(String name) {
    this.name = requireNonNull(name, "name");
  }

  @Override
  public R execute(C connection, DatabaseFunction<C, T, R> function, T argument) throws DatabaseException {
    return requireNonNull(function, "function").execute(connection, argument);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultFunctionType<?, ?, ?> that = (DefaultFunctionType<?, ?, ?>) o;

    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }
}
