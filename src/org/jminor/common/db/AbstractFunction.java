/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * A base Function implementation
 */
public abstract class AbstractFunction extends DefaultDatabaseConnection.DefaultOperation implements DatabaseConnection.Function {

  /**
   * Instantiates a new AbstractFunction.
   * @param id the function ID
   * @param name the function name
   */
  public AbstractFunction(final String id, final String name) {
    super(id, name);
  }
}
