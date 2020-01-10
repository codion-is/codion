/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.operation;

/**
 * A base Function implementation
 * @param <C> the connection type to use when executing this function
 */
public abstract class AbstractDatabaseFunction<C> extends AbstractDatabaseOperation implements DatabaseFunction<C> {

  /**
   * Instantiates a new AbstractDatabaseFunction.
   * @param id the function ID
   * @param name the function name
   */
  public AbstractDatabaseFunction(final String id, final String name) {
    super(id, name);
  }
}
