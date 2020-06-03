/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

/**
 * A base Operation implementation
 */
abstract class AbstractDatabaseOperation implements DatabaseOperation {

  private final OperationType type;

  /**
   * Instantiates a new AbstractOperation
   * @param type a unique operation type
   */
  AbstractDatabaseOperation(final OperationType type) {
    this.type = type;
  }

  @Override
  public final String getName() {
    return type.getName();
  }

  /**
   * @return this operations type
   */
  protected OperationType getOperationType() {
    return type;
  }
}
