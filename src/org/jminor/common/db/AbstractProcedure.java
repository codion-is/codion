/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * A base Procedure implementation
 */
public abstract class AbstractProcedure extends AbstractOperation implements DatabaseConnection.Procedure {

  /**
   * Instantiates a new AbstractProcedure
   * @param id the procedure ID
   * @param name the procedure name
   */
  public AbstractProcedure(final String id, final String name) {
    super(id, name);
  }
}
