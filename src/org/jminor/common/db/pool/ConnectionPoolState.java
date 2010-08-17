/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

/**
* An interface encapsulating the state of a connection pool at a given time.
*/
public interface ConnectionPoolState {

  /**
   * @return the total number of connections being managed by the pool
   */
  int getConnectionCount();

  /**
   * @return the number of connections currently in use
   */
  int getConnectionsInUse();

  /**
   * @return the timestamp associated with this pool state
   */
  long getTime();
}
