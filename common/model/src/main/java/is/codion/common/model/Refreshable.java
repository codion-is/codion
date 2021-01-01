/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model;

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
