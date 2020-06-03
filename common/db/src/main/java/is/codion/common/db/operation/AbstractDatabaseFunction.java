/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

/**
 * A base Function implementation
 * @param <C> the connection type to use when executing this function
 * @param <T> the function argument type
 * @param <R> the function return type
 */
public abstract class AbstractDatabaseFunction<C, T, R> extends AbstractDatabaseOperation implements DatabaseFunction<C, T, R> {

  /**
   * Instantiates a new AbstractDatabaseFunction.
   * @param type the function type
   * @param name the function name
   */
  public AbstractDatabaseFunction(final FunctionType<C, T, R> type) {
    super(type);
  }

  /**
   * @return this functions type
   */
  @Override
  public final FunctionType<C, T, R> getType() {
    return (FunctionType<C, T, R>) super.getOperationType();
  }
}
