/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import java.io.Serializable;

/**
 * User: Björn Darri<br>
 * Date: 14.7.2010<br>
 * Time: 23:12:21
 */
public class DbConnectionPoolState implements ConnectionPoolState, Serializable {

  private static final long serialVersionUID = 1;

  private long time;
  private int connectionCount;
  private int connectionsInUse;

  public DbConnectionPoolState(final long time, final int connectionCount, final int connectionsInUse) {
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
