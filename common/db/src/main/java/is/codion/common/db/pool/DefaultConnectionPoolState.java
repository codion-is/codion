/*
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

import java.io.Serializable;

/**
 * A default ConnectionPoolState implementation
 */
final class DefaultConnectionPoolState implements ConnectionPoolState, Serializable {

  private static final long serialVersionUID = 1;

  private long timestamp;
  private int size = -1;
  private int inUse = -1;
  private int waiting = -1;

  DefaultConnectionPoolState set(long timestamp, int size, int inUse, int waiting) {
    this.timestamp = timestamp;
    this.size = size;
    this.inUse = inUse;
    this.waiting = waiting;

    return this;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public int inUse() {
    return inUse;
  }

  @Override
  public int waiting() {
    return waiting;
  }

  @Override
  public long timestamp() {
    return timestamp;
  }
}
