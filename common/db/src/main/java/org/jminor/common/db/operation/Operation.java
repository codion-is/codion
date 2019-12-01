/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.operation;

/**
 * A database operation
 */
public interface Operation {

  /**
   * @return this operations ID
   */
  String getId();

  /**
   * @return the name of this operation
   */
  String getName();
}
