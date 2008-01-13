/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;

/**
* User: Bj�rn Darri
* Date: 8.12.2007
* Time: 16:17:01
*/
public class ConnectionPoolState implements Serializable, Comparable<ConnectionPoolState> {

  public long time;
  public int connectionCount;
  public int inUse;

  public ConnectionPoolState(final long time, final int count, final int inUse) {
    this.time = time;
    this.connectionCount = count;
    this.inUse = inUse;
  }

  public void set(final long time, final int size, final int inUse) {
    this.time = time;
    this.connectionCount = size;
    this.inUse = inUse;
  }

  public int compareTo(final ConnectionPoolState poolState) {
    if (this.time < poolState.time)
      return -1;
    else if (this.time > poolState.time)
      return 1;
    else
      return 0;
  }
}
