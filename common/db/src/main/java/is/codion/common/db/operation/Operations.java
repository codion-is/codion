/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * A factory class for {@link ProcedureType} and {@link FunctionType} instances.
 */
public final class Operations {

  private Operations() {}

  public static <C, T> ProcedureType<C, T> procedureType(final String name) {
    return new DefaultProcedureType<>(name);
  }

  public static <C, T, R> FunctionType<C, T, R> functionType(final String name) {
    return new DefaultFunctionType<>(name);
  }

  private static final class DefaultProcedureType<C, T> implements ProcedureType<C, T>, Serializable {

    private final String name;

    private DefaultProcedureType(final String name) {
      this.name = requireNonNull(name, "name");
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
  }

  private static final class DefaultFunctionType<C, T, R> implements FunctionType<C, T, R>, Serializable {

    private final String name;

    public DefaultFunctionType(final String name) {
      this.name = requireNonNull(name, "name");
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
      final DefaultFunctionType<?, ?, ?> that = (DefaultFunctionType<?, ?, ?>) o;

      return name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }

}
