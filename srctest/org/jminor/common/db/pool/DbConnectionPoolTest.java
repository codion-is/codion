/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.DbConnectionProvider;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Date;

public class DbConnectionPoolTest {

  @Test
  public void test() throws Exception {
    final Date startDate = new Date();
    final User user = new User("scott", null);
    final ConnectionPoolSettings settings = ConnectionPoolSettings.getDefault(user);
    assertEquals(60000, settings.getPooledConnectionTimeout());
    final DbConnectionPool pool = new DbConnectionPool(new DbConnectionProvider() {
      final Database database = DatabaseProvider.createInstance();
      public DbConnection createConnection(final User user) throws ClassNotFoundException, SQLException {
        return new DbConnection(database, user);
      }
    }, settings);
    pool.getConnectionPoolSettings().getUser().setPassword("tiger");
    assertEquals(user, pool.getUser());
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

    final DbConnection dbConnectionOne = pool.checkOutConnection();
    assertTrue(dbConnectionOne.isConnectionValid());
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(1, statistics.getConnectionRequests());
    assertEquals(1, statistics.getConnectionsCreated());
    assertEquals(0, statistics.getAvailableInPool());
    assertEquals(1, statistics.getConnectionsInUse());
    assertEquals(1, statistics.getLiveConnectionCount());

    final DbConnection dbConnectionTwo = pool.checkOutConnection();
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

    final DbConnection dbConnectionThree = pool.checkOutConnection();
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

    final DbConnection dbConnectionFour = pool.checkOutConnection();
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
    assertEquals(0, pool.getConnectionPoolStatistics(System.currentTimeMillis()).getAvailableInPool());
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
