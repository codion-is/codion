/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.operation;

/**
 * A database operation
 */
public interface DatabaseOperation {

  /**
   * @return this operations id
   */
  String getId();

  /**
   * @return the name of this operation
   */
  String getName();
}
