/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

import java.io.Serializable;

/**
 * A default ConnectionPoolState implementation
 */
final class DefaultConnectionPoolState implements ConnectionPoolState, Serializable {

  private static final long serialVersionUID = 1;

  private long time;
  private int connectionCount = -1;
  private int connectionsInUse = -1;
  private int connectionsWaiting = -1;

  @Override
  public void set(final long time, final int connectionCount, final int connectionsInUse, final int connectionsWaiting) {
    this.time = time;
    this.connectionCount = connectionCount;
    this.connectionsInUse = connectionsInUse;
    this.connectionsWaiting = connectionsWaiting;
  }

  @Override
  public int getSize() {
    return connectionCount;
  }

  @Override
  public int getInUse() {
    return connectionsInUse;
  }

  @Override
  public int getWaiting() {
    return connectionsWaiting;
  }

  @Override
  public long getTimestamp() {
    return time;
  }
}
