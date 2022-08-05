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
  int size();

  /**
   * @return the number of connections currently in use
   */
  int inUse();

  /**
   * @return the number of pending requests
   */
  int waiting();

  /**
   * @return the timestamp associated with this pool state
   */
  long timestamp();
}
