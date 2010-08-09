/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * Encapsulates basic database usage statistics.
 */
public interface DatabaseStatistics {

  /**
   * @return the number of queries being run per second
   */
  int getQueriesPerSecond();

  /**
   * @return the number of delete queries being run per second
   */
  int getDeletesPerSecond();

  /**
   * @return the number of insert queries being run per second
   */
  int getInsertsPerSecond();

  /**
   * @return the number of select queries being run per second
   */
  int getSelectsPerSecond();

  /**
   * @return the number of update queries being run per second
   */
  int getUpdatesPerSecond();

  /**
   * @return the timestamp of these statistics
   */
  long getTimestamp();
}
