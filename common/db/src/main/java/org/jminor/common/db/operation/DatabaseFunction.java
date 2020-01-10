/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.operation;

import org.jminor.common.db.exception.DatabaseException;

import java.util.List;

/**
 * A database function
 * @param <C> the connection type required by this function
 */
public interface DatabaseFunction<C> extends DatabaseOperation {

  /**
   * Executes this function with the given connection
   * @param connection the connection to use when executing
   * @param arguments the function arguments, if any
   * @return the function return arguments
   * @throws DatabaseException in case of an exception during the execution
   */
  List execute(C connection, Object... arguments) throws DatabaseException;
}
