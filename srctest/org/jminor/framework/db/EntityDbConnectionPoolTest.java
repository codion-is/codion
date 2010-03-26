/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.ConnectionPoolStatistics;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.DatabaseProvider;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Date;

public class EntityDbConnectionPoolTest {

  @Test
  public void test() throws Exception {
    final Date startDate = new Date();
    final ConnectionPoolSettings settings = ConnectionPoolSettings.getDefault(new User("scott", null));
    assertEquals(60000, settings.getPooledConnectionTimeout());
    final EntityDbConnectionPool pool = new EntityDbConnectionPool(DatabaseProvider.createInstance(), settings);
    pool.setPassword("tiger");
    pool.setCollectFineGrainedStatistics(true);
    assertTrue(pool.isCollectFineGrainedStatistics());
    ConnectionPoolStatistics statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(new User("scott", null), statistics.getUser());
    assertNotNull(statistics.getTimestamp());
    assertNotNull(statistics.getCreationDate());
    assertEquals(0, statistics.getConnectionsDestroyed());
    assertEquals(0, statistics.getConnectionRequests());
    assertEquals(0, statistics.getAvailableInPool());
    assertEquals(0, statistics.getConnectionsInUse());

    final EntityDbConnection dbConnectionOne = pool.checkOutConnection();
    assertTrue(dbConnectionOne.isConnectionValid());
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(1, statistics.getConnectionRequests());
    assertEquals(1, statistics.getConnectionsCreated());
    assertEquals(0, statistics.getAvailableInPool());
    assertEquals(1, statistics.getConnectionsInUse());
    assertEquals(1, statistics.getLiveConnectionCount());

    final EntityDbConnection dbConnectionTwo = pool.checkOutConnection();
    assertTrue(dbConnectionTwo.isConnectionValid());
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(2, statistics.getConnectionRequests());
    assertEquals(2, statistics.getConnectionsCreated());
    assertEquals(0, statistics.getAvailableInPool());
    assertEquals(2, statistics.getConnectionsInUse());
    assertEquals(2, statistics.getLiveConnectionCount());

    pool.checkInConnection(dbConnectionOne);
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(2, statistics.getConnectionRequests());
    assertEquals(2, statistics.getConnectionsCreated());
    assertEquals(1, statistics.getAvailableInPool());
    assertEquals(1, statistics.getConnectionsInUse());
    assertEquals(2, statistics.getLiveConnectionCount());

    final EntityDbConnection dbConnectionThree = pool.checkOutConnection();
    assertTrue(dbConnectionThree.isConnectionValid());
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(3, statistics.getConnectionRequests());
    assertEquals(2, statistics.getConnectionsCreated());
    assertEquals(0, statistics.getAvailableInPool());
    assertEquals(2, statistics.getConnectionsInUse());
    assertEquals(2, statistics.getLiveConnectionCount());

    pool.checkInConnection(dbConnectionTwo);
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(3, statistics.getConnectionRequests());
    assertEquals(2, statistics.getConnectionsCreated());
    assertEquals(1, statistics.getAvailableInPool());
    assertEquals(1, statistics.getConnectionsInUse());
    assertEquals(2, statistics.getLiveConnectionCount());

    pool.checkInConnection(dbConnectionThree);
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(3, statistics.getConnectionRequests());
    assertEquals(2, statistics.getConnectionsCreated());
    assertEquals(2, statistics.getAvailableInPool());
    assertEquals(0, statistics.getConnectionsInUse());
    assertEquals(2, statistics.getLiveConnectionCount());

    assertTrue(statistics.getPoolStatistics().size() > 0);

    final EntityDbConnection dbConnectionFour = pool.checkOutConnection();
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(4, statistics.getConnectionRequests());
    assertEquals(2, statistics.getConnectionsCreated());
    assertEquals(1, statistics.getAvailableInPool());
    assertEquals(1, statistics.getConnectionsInUse());
    assertEquals(2, statistics.getLiveConnectionCount());

    dbConnectionFour.disconnect();
    pool.checkInConnection(dbConnectionFour);
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(2, statistics.getConnectionsCreated());
    assertEquals(1, statistics.getAvailableInPool());
    assertEquals(0, statistics.getConnectionsInUse());
    assertEquals(1, statistics.getLiveConnectionCount());

    assertNotNull(statistics.getRequestsPerSecond());
    assertNotNull(statistics.getRequestsDelayedPerSecond());
    assertNotNull(statistics.getConnectionRequestsDelayed());

    pool.resetPoolStatistics();
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(0, statistics.getConnectionRequests());
    assertEquals(0, statistics.getConnectionsCreated());
    assertNotNull(statistics.getResetDate());

    settings.setEnabled(false);
    pool.setConnectionPoolSettings(settings);
    try {
      pool.checkInConnection(dbConnectionThree);
      fail();
    }
    catch (IllegalStateException e) {}
    try {
      pool.checkOutConnection();
      fail();
    }
    catch (IllegalStateException e) {}
  }
}
