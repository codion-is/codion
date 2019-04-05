/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.User;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.DatabaseConnectionsTest;
import org.jminor.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultConnectionPoolTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  @Test
  public void setMaximumPoolSizeLessThanMinSize() throws ClassNotFoundException, DatabaseException {
    final ConnectionPool pool = new DefaultConnectionPool(DatabaseConnectionsTest.createTestDatabaseConnectionProvider());
    assertThrows(IllegalArgumentException.class, () -> pool.setMaximumPoolSize(3));
  }

  @Test
  public void setMaximumPoolSizeInvalidNumber() throws ClassNotFoundException, DatabaseException {
    final ConnectionPool pool = new DefaultConnectionPool(DatabaseConnectionsTest.createTestDatabaseConnectionProvider());
    assertThrows(IllegalArgumentException.class, () -> pool.setMaximumPoolSize(-1));
  }

  @Test
  public void setMinimumPoolSizeLargerThanMaxSize() throws ClassNotFoundException, DatabaseException {
    final ConnectionPool pool = new DefaultConnectionPool(DatabaseConnectionsTest.createTestDatabaseConnectionProvider());
    assertThrows(IllegalArgumentException.class, () -> pool.setMinimumPoolSize(10));
  }

  @Test
  public void setMinimumPoolSizeInvalidNumber() throws ClassNotFoundException, DatabaseException {
    final ConnectionPool pool = new DefaultConnectionPool(DatabaseConnectionsTest.createTestDatabaseConnectionProvider());
    assertThrows(IllegalArgumentException.class, () -> pool.setMinimumPoolSize(-1));
  }

  @Test
  public void returnConnectionOpenTransaction() throws DatabaseException {
    final DefaultConnectionPool pool = new DefaultConnectionPool(DatabaseConnectionsTest.createTestDatabaseConnectionProvider());
    final DatabaseConnection connection = pool.getDatabaseConnection();
    try {
      connection.beginTransaction();
      assertThrows(IllegalStateException.class, () -> pool.returnConnection(connection));
    }
    finally {
      connection.rollbackTransaction();
      pool.close();
    }
  }

  @Test
  public void getConnectionClosedPool() throws DatabaseException {
    final ConnectionPool pool = new DefaultConnectionPool(DatabaseConnectionsTest.createTestDatabaseConnectionProvider());
    try {
      pool.close();
      assertThrows(IllegalStateException.class, pool::getConnection);
    }
    finally {
      pool.close();
    }
  }

  @Test
  public void noConnectionAvailable() throws DatabaseException {
    final DefaultConnectionPool pool = new DefaultConnectionPool(DatabaseConnectionsTest.createTestDatabaseConnectionProvider());
    pool.setMaximumCheckOutTime(50);
    pool.setNewConnectionThreshold(40);
    assertThrows(ConnectionPoolException.NoConnectionAvailable.class, () -> {
      pool.getConnection();
      pool.getConnection();
      pool.getConnection();
      pool.getConnection();
      pool.getConnection();
      pool.getConnection();
      pool.getConnection();
      pool.getConnection();
      pool.getConnection();
    });
  }

  @Test
  public void test() throws Exception {
    final Instant startDate = Instant.now();
    final DefaultConnectionPool pool = new DefaultConnectionPool(DatabaseConnectionsTest.createTestDatabaseConnectionProvider());
    pool.setCleanupInterval(2000);
    pool.setConnectionTimeout(6000);
    pool.setMaximumPoolSize(8);
    try {
      assertEquals(UNIT_TEST_USER, pool.getUser());
      assertEquals(2000, pool.getCleanupInterval());
      assertEquals(6000, pool.getConnectionTimeout());
      assertEquals(8, pool.getMaximumPoolSize());
      final int minimumPoolSize = 4;
      assertEquals(minimumPoolSize, pool.getMinimumPoolSize());
      assertEquals(minimumPoolSize, pool.getStatistics(System.currentTimeMillis()).getAvailable());
      pool.setMaximumPoolSize(6);
      assertEquals(6, pool.getMaximumPoolSize());
      pool.setMinimumPoolSize(3);
      assertEquals(3, pool.getMinimumPoolSize());

      pool.setCollectFineGrainedStatistics(true);
      assertTrue(pool.isCollectFineGrainedStatistics());
      ConnectionPoolStatistics statistics = pool.getStatistics(startDate.toEpochMilli());
      assertEquals(new User(UNIT_TEST_USER.getUsername(), null), statistics.getUser());
      statistics.getTimestamp();
      statistics.getCreationDate();
      assertEquals(0, statistics.getDestroyed());
      assertEquals(0, statistics.getRequests());
      assertEquals(4, statistics.getAvailable());
      assertEquals(0, statistics.getInUse());

      final DatabaseConnection dbConnectionOne = pool.getDatabaseConnection();
      assertTrue(dbConnectionOne.isConnected());
      statistics = pool.getStatistics(startDate.toEpochMilli());
      assertEquals(1, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(3, statistics.getAvailable());
      assertEquals(1, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      final DatabaseConnection dbConnectionTwo = pool.getDatabaseConnection();
      assertTrue(dbConnectionTwo.isConnected());
      statistics = pool.getStatistics(startDate.toEpochMilli());
      assertEquals(2, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(2, statistics.getAvailable());
      assertEquals(2, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      pool.returnConnection(dbConnectionOne);
      statistics = pool.getStatistics(startDate.toEpochMilli());
      assertEquals(2, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(3, statistics.getAvailable());
      assertEquals(1, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      final DatabaseConnection dbConnectionThree = pool.getDatabaseConnection();
      assertTrue(dbConnectionThree.isConnected());
      statistics = pool.getStatistics(startDate.toEpochMilli());
      assertEquals(3, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(2, statistics.getAvailable());
      assertEquals(2, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      pool.returnConnection(dbConnectionTwo);
      statistics = pool.getStatistics(startDate.toEpochMilli());
      assertEquals(3, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(3, statistics.getAvailable());
      assertEquals(1, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      pool.returnConnection(dbConnectionThree);
      statistics = pool.getStatistics(startDate.toEpochMilli());
      assertEquals(3, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(4, statistics.getAvailable());
      assertEquals(0, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      assertTrue(statistics.getFineGrainedStatistics().size() > 0);

      final DatabaseConnection dbConnectionFour = pool.getDatabaseConnection();
      statistics = pool.getStatistics(startDate.toEpochMilli());
      assertEquals(4, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(3, statistics.getAvailable());
      assertEquals(1, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      dbConnectionFour.disconnect();
      pool.returnConnection(dbConnectionFour);
      statistics = pool.getStatistics(startDate.toEpochMilli());
      assertEquals(4, statistics.getCreated());
      assertEquals(3, statistics.getAvailable());
      assertEquals(0, statistics.getInUse());
      assertEquals(3, statistics.getSize());

      statistics.getRequestsPerSecond();
      statistics.getDelayedRequestsPerSecond();
      statistics.getDelayedRequests();

      final List<ConnectionPoolState> states = statistics.getFineGrainedStatistics();
      assertFalse(states.isEmpty());
      final ConnectionPoolState state = states.get(0);
      assertTrue(state.getSize() != -1);
      assertTrue(state.getInUse() != -1);
      assertEquals(0, state.getWaiting());//not implemented

      pool.resetStatistics();
      statistics = pool.getStatistics(startDate.toEpochMilli());
      assertEquals(0, statistics.getRequests());
      assertEquals(0, statistics.getCreated());

      statistics.getResetTime();

      pool.setCollectFineGrainedStatistics(false);
      statistics = pool.getStatistics(startDate.toEpochMilli());
      assertTrue(statistics.getFineGrainedStatistics().isEmpty());

      pool.close();
      assertEquals(0, pool.getStatistics(System.currentTimeMillis()).getAvailable());
      pool.returnConnection(dbConnectionThree);
    }
    finally {
      pool.close();
    }
  }
}
