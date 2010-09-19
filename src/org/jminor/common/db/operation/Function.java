/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.operation;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.DbConnection;

import java.util.List;

/**
 * A database function
 */
public interface Function extends Operation {

  /**
   * Executes this function with the given connection
   * @param connection the db connection to use when executing
   * @param arguments the function arguments, if any
   * @return the function return arguments
   * @throws org.jminor.common.db.exception.DbException in case of an exception during the execution
   */
  List<Object> execute(final DbConnection connection, final List<Object> arguments) throws DbException;
}