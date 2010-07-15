/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * Encapsulates basic database usage statistics.
 */
public interface DatabaseStatistics {

  int getQueriesPerSecond();

  int getDeletesPerSecond();

  int getInsertsPerSecond();

  int getSelectsPerSecond();

  int getUpdatesPerSecond();

  long getTimestamp();
}
