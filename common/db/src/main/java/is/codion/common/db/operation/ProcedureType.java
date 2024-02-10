/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

/**
 * @param <C> the connection type
 * @param <T> the procedure argument type
 */
public interface ProcedureType<C, T> {

  /**
   * @return the procedure name
   */
  String name();

  /**
   * Creates a {@link ProcedureType} with the given name and types.
   * @param name the name
   * @param <C> the connection type
   * @param <T> the procedure argument type
   * @return a new {@link ProcedureType}
   */
  static <C, T> ProcedureType<C, T> procedureType(String name) {
    return new DefaultProcedureType<>(name);
  }
}
