/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

final class DefaultFunctionType<C, T, R> implements FunctionType<C, T, R>, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;

  DefaultFunctionType(final String name) {
    this.name = requireNonNull(name, "name");
  }

  @Override
  public R execute(final C connection, final DatabaseFunction<C, T, R> function, final T argument) throws DatabaseException {
    return requireNonNull(function, "function").execute(connection, argument);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean equals(final Object o) {
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
