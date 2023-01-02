/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

final class DefaultProcedureType<C, T> implements ProcedureType<C, T>, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;

  DefaultProcedureType(String name) {
    this.name = requireNonNull(name, "name");
  }

  @Override
  public void execute(C connection, DatabaseProcedure<C, T> procedure, T argument) throws DatabaseException {
    requireNonNull(procedure, "procedure").execute(connection, argument);
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
    DefaultProcedureType<?, ?> that = (DefaultProcedureType<?, ?>) o;

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
