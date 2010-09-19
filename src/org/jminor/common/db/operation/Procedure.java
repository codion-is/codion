/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.operation;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.db.DbConnection;

import java.util.List;

/**
 * A database procedure
 */
public interface Procedure extends Operation {

  /**
   * Executes this procedure with the given connection
   * @param connection the db connection to use when executing
   * @param arguments the procedure arguments, if any
   * @throws org.jminor.common.db.exception.DbException in case of an exception during the execution
   */
  void execute(final DbConnection connection, final List<Object> arguments) throws DbException;
}
