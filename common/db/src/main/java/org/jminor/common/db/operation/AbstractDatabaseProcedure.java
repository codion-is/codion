/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.operation;

/**
 * A base Procedure implementation
 * @param <C> the connection type to use when executing this function
 */
public abstract class AbstractDatabaseProcedure<C> extends AbstractDatabaseOperation implements DatabaseProcedure<C> {

  /**
   * Instantiates a new AbstractDatabaseProcedure
   * @param id the procedure id
   * @param name the procedure name
   */
  public AbstractDatabaseProcedure(final String id, final String name) {
    super(id, name);
  }
}
