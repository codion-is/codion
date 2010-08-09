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
