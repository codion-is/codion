/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.operation;

import org.jminor.common.db.exception.DatabaseException;

/**
 * A database procedure
 * @param <C> the connection type required by this procedure
 */
public interface Procedure<C> extends Operation {

  /**
   * Executes this procedure with the given connection
   * @param connection the connection to use when executing
   * @param arguments the procedure arguments, if any
   * @throws DatabaseException in case of an exception during the execution
   */
  void execute(final C connection, final Object... arguments) throws DatabaseException;
}
