/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;

/**
 * Encapsulates basic database usage statistics.
 */
public class DatabaseStatistics implements Serializable {

  private static final long serialVersionUID = 1;

  private final long timestamp = System.currentTimeMillis();
  private final int queriesPerSecond;
  private final int selectsPerSecond;
  private final int insertsPerSecond;
  private final int deletesPerSecond;
  private final int updatesPerSecond;

  public DatabaseStatistics(final int queriesPerSecond, final int selectsPerSecond, final int insertsPerSecond,
                            final int deletesPerSecond, final int updatesPerSecond) {
    this.queriesPerSecond = queriesPerSecond;
    this.selectsPerSecond = selectsPerSecond;
    this.insertsPerSecond = insertsPerSecond;
    this.deletesPerSecond = deletesPerSecond;
    this.updatesPerSecond = updatesPerSecond;
  }

  public int getQueriesPerSecond() {
    return queriesPerSecond;
  }

  public int getDeletesPerSecond() {
    return deletesPerSecond;
  }

  public int getInsertsPerSecond() {
    return insertsPerSecond;
  }

  public int getSelectsPerSecond() {
    return selectsPerSecond;
  }

  public int getUpdatesPerSecond() {
    return updatesPerSecond;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
