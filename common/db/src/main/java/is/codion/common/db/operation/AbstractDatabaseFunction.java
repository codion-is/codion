/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.operation;

/**
 * A base Function implementation
 * @param <C> the connection type to use when executing this function
 * @param <T> the result type
 */
public abstract class AbstractDatabaseFunction<C, T> extends AbstractDatabaseOperation implements DatabaseFunction<C, T> {

  /**
   * Instantiates a new AbstractDatabaseFunction.
   * @param id the function id
   * @param name the function name
   */
  public AbstractDatabaseFunction(final String id, final String name) {
    super(id, name);
  }
}
