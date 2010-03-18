/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;

/**
* User: Björn Darri
* Date: 8.12.2007
* Time: 16:17:01
*/
public class ConnectionPoolState implements Serializable, Comparable<ConnectionPoolState> {

  private static final long serialVersionUID = 1;

  public long time;
  public int connectionCount;
  public int connectionsInUse;

  public ConnectionPoolState(final long time, final int connectionCount, final int connectionsInUse) {
    set(time, connectionCount, connectionsInUse);
  }

  public void set(final long time, final int connectionCount, final int connectionsInUse) {
    this.time = time;
    this.connectionCount = connectionCount;
    this.connectionsInUse = connectionsInUse;
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
