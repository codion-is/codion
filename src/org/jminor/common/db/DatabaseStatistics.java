package org.jminor.common.db;

import java.io.Serializable;

public class DatabaseStatistics implements Serializable {

  private final long timestamp = System.currentTimeMillis();
  private int queriesPerSecond;
  private int cachedQueriesPerSecond;

  public void setCachedQueriesPerSecond(int cachedQueriesPerSecond) {
    this.cachedQueriesPerSecond = cachedQueriesPerSecond;
  }

  public int getCachedQueriesPerSecond() {
    return cachedQueriesPerSecond;
  }

  public void setQueriesPerSecond(int queriesPerSecond) {
    this.queriesPerSecond = queriesPerSecond;
  }

  public int getQueriesPerSecond() {
    return queriesPerSecond;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
