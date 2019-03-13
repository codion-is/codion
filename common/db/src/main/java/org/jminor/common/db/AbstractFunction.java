/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * A base Function implementation
 * @param <C> the connection type to use when executing this function
 */
public abstract class AbstractFunction<C> extends DefaultDatabaseConnection.DefaultOperation implements DatabaseConnection.Function<C> {

  /**
   * Instantiates a new AbstractFunction.
   * @param id the function ID
   * @param name the function name
   */
  public AbstractFunction(final String id, final String name) {
    super(id, name);
  }
}
