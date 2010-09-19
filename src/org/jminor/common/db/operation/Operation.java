/*
 * Copyright (c) 2004 - 2010, Bj�rn Darri Sigur�sson. All Rights Reserved.
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
