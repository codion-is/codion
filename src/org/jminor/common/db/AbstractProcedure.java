/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * A base Procedure implementation
 * @param <C> the connection type to use when executing this function
 */
public abstract class AbstractProcedure<C> extends DefaultDatabaseConnection.DefaultOperation implements DatabaseConnection.Procedure<C> {

  /**
   * Instantiates a new AbstractProcedure
   * @param id the procedure ID
   * @param name the procedure name
   */
  public AbstractProcedure(final String id, final String name) {
    super(id, name);
  }
}
