/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

/**
* An interface encapsulating the state of a connection pool at a given time.<br>
* User: Bjorn Darri<br>
* Date: 8.12.2007<br>
* Time: 16:17:01<br>
*/
public interface ConnectionPoolState {

  void set(final long time, final int connectionCount, final int connectionsInUse);

  int getConnectionCount();

  int getConnectionsInUse();

  long getTime();
}
