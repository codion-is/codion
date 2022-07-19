/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

import java.util.List;

/**
 * An interface encapsulating database connection pool statistics
 */
public interface ConnectionPoolStatistics {

  /**
   * @return the connection pool username
   */
  String getUsername();

  /**
   * Returns a list containing a snapshot of connection pool states.
   * @return a snapshot of pool states associated with this statistics object
   */
  List<ConnectionPoolState> getSnapshot();

  /**
   * @return the number of connections being managed by the pool
   */
  int getSize();

  /**
   * @return the number of available connections
   */
  int getAvailable();

  /**
   * @return the number of connections in use
   */
  int getInUse();

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
  int getCreated();

  /**
   * @return the number of idle connections destroyed by the pool
   */
  int getDestroyed();

  /**
   * @return the last time stats were reset
   */
  long getResetTime();

  /**
   * @return the number of connection requests since last reset
   */
  int getRequests();

  /**
   * @return the number of connection requests per second
   */
  int getRequestsPerSecond();

  /**
   * @return the number of failed connection requests since last reset
   */
  int getFailedRequests();

  /**
   * @return the number of failed connection requests per second
   */
  int getFailedRequestsPerSecond();

  /**
   * @return the avarage check out time in ms
   */
  long getAverageGetTime();

  /**
   * @return the minimum check out time in ms
   */
  long getMinimumCheckOutTime();

  /**
   * @return the maximum check out time in ms
   */
  long getMaximumCheckOutTime();
}