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

  List<ConnectionPoolState> getPoolStatistics();

  int getAvailableInPool();

  int getConnectionsInUse();

  long getTimestamp();

  long getCreationDate();

  int getConnectionsCreated();

  int getConnectionsDestroyed();

  int getConnectionRequestsDelayed();

  int getConnectionRequests();

  int getRequestsDelayedPerSecond();

  int getRequestsPerSecond();

  long getAverageCheckOutTime();

  int getLiveConnectionCount();

  long getResetDate();
}