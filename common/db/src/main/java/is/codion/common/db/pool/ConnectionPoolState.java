/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

/**
 * An interface encapsulating the state of a connection pool at a given time.
 */
public interface ConnectionPoolState {

  /**
   * Sets this state
   * @param time the time
   * @param connectionCount the number of connections managed by the pool
   * @param connectionsInUse the number of connections in use
   * @param connectionsWaiting the number pending requests for connections
   */
  void set(long time, int connectionCount, int connectionsInUse, int connectionsWaiting);

  /**
   * @return the total number of connections being managed by the pool
   */
  int getSize();

  /**
   * @return the number of connections currently in use
   */
  int getInUse();

  /**
   * @return the number of pending requests
   */
  int getWaiting();

  /**
   * @return the timestamp associated with this pool state
   */
  long getTimestamp();
}
