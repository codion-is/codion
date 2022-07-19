/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

/**
 * An interface encapsulating the state of a connection pool at a given time.
 */
public interface ConnectionPoolState {

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
