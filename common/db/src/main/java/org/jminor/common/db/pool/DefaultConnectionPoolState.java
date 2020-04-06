/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import java.io.Serializable;

/**
 * A default ConnectionPoolState implementation
 */
public final class DefaultConnectionPoolState implements ConnectionPoolState, Serializable {

  private static final long serialVersionUID = 1;

  private long time;
  private int connectionCount = -1;
  private int connectionsInUse = -1;
  private int connectionsWaiting = -1;

  /** {@inheritDoc} */
  @Override
  public void set(final long time, final int connectionCount, final int connectionsInUse, final int connectionsWaiting) {
    this.time = time;
    this.connectionCount = connectionCount;
    this.connectionsInUse = connectionsInUse;
    this.connectionsWaiting = connectionsWaiting;
  }

  /** {@inheritDoc} */
  @Override
  public int getSize() {
    return connectionCount;
  }

  /** {@inheritDoc} */
  @Override
  public int getInUse() {
    return connectionsInUse;
  }

  /** {@inheritDoc} */
  @Override
  public int getWaiting() {
    return connectionsWaiting;
  }

  /** {@inheritDoc} */
  @Override
  public long getTimestamp() {
    return time;
  }
}
