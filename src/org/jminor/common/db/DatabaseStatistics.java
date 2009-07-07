/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;

public class DatabaseStatistics implements Serializable {

  private static final long serialVersionUID = 1;

  private final long timestamp = System.currentTimeMillis();
  private final int queriesPerSecond;
  private final int cachedQueriesPerSecond;

  public DatabaseStatistics(final int queriesPerSecond, final int cachedQueriesPerSecond) {
    this.queriesPerSecond = queriesPerSecond;
    this.cachedQueriesPerSecond = cachedQueriesPerSecond;
  }

  public int getCachedQueriesPerSecond() {
    return cachedQueriesPerSecond;
  }

  public int getQueriesPerSecond() {
    return queriesPerSecond;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
