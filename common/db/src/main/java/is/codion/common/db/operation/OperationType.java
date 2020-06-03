/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import java.io.Serializable;

/**
 * Identifies an operation.
 */
public interface OperationType extends Serializable {

  /**
   * @return the operation name
   */
  String getName();
}
