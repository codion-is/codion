/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import java.io.Serializable;

/**
* User: Björn Darri
* Date: 8.12.2007
* Time: 16:17:01
*/
public class ConnectionPoolState implements Serializable {

  private static final long serialVersionUID = 1;

  private long time;
  private int connectionCount;
  private int connectionsInUse;

  public ConnectionPoolState(final long time, final int connectionCount, final int connectionsInUse) {
    set(time, connectionCount, connectionsInUse);
  }

  public void set(final long time, final int connectionCount, final int connectionsInUse) {
    this.time = time;
    this.connectionCount = connectionCount;
    this.connectionsInUse = connectionsInUse;
  }

  public int getConnectionCount() {
    return connectionCount;
  }

  public int getConnectionsInUse() {
    return connectionsInUse;
  }

  public long getTime() {
    return time;
  }
}
