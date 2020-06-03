/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

/**
 * A database operation
 */
public interface DatabaseOperation {

  /**
   * @return this operations type
   */
  OperationType getType();

  /**
   * @return the name of this operation
   */
  String getName();
}
