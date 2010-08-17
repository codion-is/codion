/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.model.User;

import java.util.List;

/**
 * An interface encapsulating database connection pool statistics
 */
public interface ConnectionPoolStatistics {

  /**
   * @return the connection pool user
   */
  User getUser();

  /**
   * Returns a list containing connection pool states spanning some interval,
   * the order of items in the list is not guaranteed.
   * @return fine grained list of pool states associated with this statistics object
   */
  List<ConnectionPoolState> getFineGrainedStatistics();

  /**
   * @return the number of available connections
   */
  int getAvailableInPool();

  /**
   * @return the number of connections in use
   */
  int getConnectionsInUse();

  /**
   * @return the timestamp
   */
  long getTimestamp();

  /**
   * @return the time at which this statistics object was created
   */
  long getCreationDate();

  /**
   * @return the number of connections created by the pool
   */
  int getConnectionsCreated();

  /**
   * @return the number of idle connections destroyed by the pool
   */
  int getConnectionsDestroyed();

  /**
   * @return the last time stats were reset
   */
  long getResetDate();

  /**
   * @return the number of delayed connection requests since last reset
   */
  int getConnectionRequestsDelayed();

  /**
   * @return the number of connection requests since last reset
   */
  int getConnectionRequests();

  /**
   * @return the number of delayed connection requests per second
   */
  int getRequestsDelayedPerSecond();

  /**
   * @return the number of connection requests per second
   */
  int getRequestsPerSecond();

  /**
   * @return the avarage check out time in nanoseconds
   */
  long getAverageCheckOutTime();

  /**
   * @return the number of connections being managed by the pool
   */
  int getPoolSize();
}