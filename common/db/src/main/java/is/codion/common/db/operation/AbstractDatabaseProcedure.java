/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

/**
 * A base Procedure implementation
 * @param <C> the connection type to use when executing this function
 */
public abstract class AbstractDatabaseProcedure<C, T> extends AbstractDatabaseOperation implements DatabaseProcedure<C, T> {

  /**
   * Instantiates a new AbstractDatabaseProcedure
   * @param type the procedure type
   */
  public AbstractDatabaseProcedure(final ProcedureType<C, T> type) {
    super(type);
  }

  /**
   * @return this procedures type
   */
  @Override
  public final ProcedureType<C, T> getType() {
    return (ProcedureType<C, T>) super.getOperationType();
  }
}
