/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;

/**
 * A default DatabaseStatistics implementation.
 */
public final class DbStatistics implements DatabaseStatistics, Serializable {

  private static final long serialVersionUID = 1;

  private final long timestamp = System.currentTimeMillis();
  private final int queriesPerSecond;
  private final int selectsPerSecond;
  private final int insertsPerSecond;
  private final int deletesPerSecond;
  private final int updatesPerSecond;

  /**
   * Instantiates a new DbStatistics object
   * @param queriesPerSecond the number of queries being run per second
   * @param selectsPerSecond the number of select queries being run per second
   * @param insertsPerSecond the number of insert queries being run per second
   * @param deletesPerSecond the number of delete queries being run per second
   * @param updatesPerSecond the number of update queries being run per second
   */
  public DbStatistics(final int queriesPerSecond, final int selectsPerSecond, final int insertsPerSecond,
                      final int deletesPerSecond, final int updatesPerSecond) {
    this.queriesPerSecond = queriesPerSecond;
    this.selectsPerSecond = selectsPerSecond;
    this.insertsPerSecond = insertsPerSecond;
    this.deletesPerSecond = deletesPerSecond;
    this.updatesPerSecond = updatesPerSecond;
  }

  /** {@inheritDoc} */
  public int getQueriesPerSecond() {
    return queriesPerSecond;
  }

  /** {@inheritDoc} */
  public int getDeletesPerSecond() {
    return deletesPerSecond;
  }

  /** {@inheritDoc} */
  public int getInsertsPerSecond() {
    return insertsPerSecond;
  }

  /** {@inheritDoc} */
  public int getSelectsPerSecond() {
    return selectsPerSecond;
  }

  /** {@inheritDoc} */
  public int getUpdatesPerSecond() {
    return updatesPerSecond;
  }

  /** {@inheritDoc} */
  public long getTimestamp() {
    return timestamp;
  }
}
