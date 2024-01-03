/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

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
   * Executes the given procedure.
   * @param connection the connection being used
   * @param procedure the procedure to execute
   * @param argument the procedure argument, if any
   * @throws DatabaseException in case of an exception
   */
  void execute(C connection, DatabaseProcedure<C, T> procedure, T argument) throws DatabaseException;

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
