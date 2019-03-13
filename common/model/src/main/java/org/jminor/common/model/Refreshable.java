/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * Specifies some sort of data container that can be refreshed and cleared.
 */
public interface Refreshable {

  /**
   * Performs a refresh, reloading the relevant data
   */
  void refresh();

  /**
   * Clears all data from this refreshable instance
   */
  void clear();
}
