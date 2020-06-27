/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

import java.io.Serializable;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultProcedureType<C, T> implements ProcedureType<C, T>, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;

  DefaultProcedureType(final String name) {
    this.name = requireNonNull(name, "name");
  }

  @Override
  public void execute(final C connection, final DatabaseProcedure<C, T> procedure, final List<T> arguments) throws DatabaseException {
    procedure.execute(connection, arguments);
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
    final DefaultProcedureType<?, ?> that = (DefaultProcedureType<?, ?>) o;

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
