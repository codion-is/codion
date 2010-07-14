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

  User getUser();

  void setPoolStatistics(final List<ConnectionPoolState> stats);

  List<ConnectionPoolState> getPoolStatistics();

  int getAvailableInPool();

  void setAvailableInPool(final int availableInPool);

  int getConnectionsInUse();

  void setConnectionsInUse(final int connectionsInUse);

  long getTimestamp();

  void setTimestamp(final long timestamp);

  void setCreationDate(final long time);

  long getCreationDate();

  void setConnectionsCreated(final int connectionsCreated);

  int getConnectionsCreated();

  void setConnectionsDestroyed(final int connectionsDestroyed);

  int getConnectionsDestroyed();

  int getConnectionRequestsDelayed();

  void setConnectionRequestsDelayed(final int connectionRequestsDelayed);

  int getConnectionRequests();

  void setConnectionRequests(final int connectionRequests);

  int getRequestsDelayedPerSecond();

  void setRequestsDelayedPerSecond(final int requestsDelayedPerSecond);

  void setRequestsPerSecond(int requestsPerSecond);

  int getRequestsPerSecond();

  long getAverageCheckOutTime();

  void setAverageCheckOutTime(final long averageCheckOutTime);

  void setLiveConnectionCount(final int liveConnectionCount);

  int getLiveConnectionCount();

  void setResetDate(final long resetDate);

  long getResetDate();
}