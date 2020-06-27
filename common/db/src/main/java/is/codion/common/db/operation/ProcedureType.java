/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import is.codion.common.db.exception.DatabaseException;

import java.io.Serializable;
import java.util.List;

/**
 * @param <C> the connection type
 * @param <T> the procedure argument type
 */
public interface ProcedureType<C, T> extends Serializable {

  /**
   * @return the procedure name
   */
  String getName();

  /**
   * Executes the given procedure.
   * @param connection the connection being used
   * @param procedure the procedure to execute
   * @param arguments the procedure arguments, if any
   * @throws DatabaseException in case of an exception
   */
  void execute(C connection, DatabaseProcedure<C, T> procedure, List<T> arguments) throws DatabaseException;

  static <C, T> ProcedureType<C, T> procedureType(final String name) {
    return new DefaultProcedureType<>(name);
  }
}
