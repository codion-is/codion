/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.operation;

/**
 * A database operation
 */
public interface Operation {

  /**
   * @return this operation's unique ID
   */
  String getID();

  /**
   * @return the name of this operation
   */
  String getName();
}
