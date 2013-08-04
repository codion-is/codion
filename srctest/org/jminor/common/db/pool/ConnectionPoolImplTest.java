/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.DatabaseConnectionProvider;
import org.jminor.common.db.DatabaseConnections;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.tools.QueryLoadTestModel;
import org.jminor.common.model.User;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class ConnectionPoolImplTest {

  private static final int SLEEP_MILLIS = 2000;
  private static final int CLOSE_SLEEP_MILLIS = 2500;

  @Test
  public void loadTest() throws Exception {
    final long startTime = System.currentTimeMillis();
    final Database database = Databases.createInstance();
    final QueryLoadTestModel model = new QueryLoadTestModel(database, User.UNIT_TEST_USER,
            Arrays.asList(new QueryLoadTestModel.QueryScenario("selectEmployees", "select * from scott.emp")));
    model.addApplicationBatch();
    model.setCollectChartData(true);
    Thread.sleep(SLEEP_MILLIS);
    model.exit();
    Thread.sleep(CLOSE_SLEEP_MILLIS);
    final ConnectionPoolStatistics statistics =  model.getConnectionPool().getStatistics(startTime);
    assertTrue(statistics.getAverageGetTime() == 0);
    assertEquals(statistics.getCreated(), statistics.getDestroyed());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setMaximumPoolSizeLessThanMinSize() throws ClassNotFoundException, DatabaseException {
    final ConnectionPool pool = new ConnectionPoolImpl(createConnectionProvider(User.UNIT_TEST_USER));
    pool.setMaximumPoolSize(3);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setMaximumPoolSizeInvalidNumber() throws ClassNotFoundException, DatabaseException {
    final ConnectionPool pool = new ConnectionPoolImpl(createConnectionProvider(User.UNIT_TEST_USER));
    pool.setMaximumPoolSize(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setMinimumPoolSizeLargerThanMaxSize() throws ClassNotFoundException, DatabaseException {
    final ConnectionPool pool = new ConnectionPoolImpl(createConnectionProvider(User.UNIT_TEST_USER));
    pool.setMinimumPoolSize(10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setMinimumPoolSizeInvalidNumber() throws ClassNotFoundException, DatabaseException {
    final ConnectionPool pool = new ConnectionPoolImpl(createConnectionProvider(User.UNIT_TEST_USER));
    pool.setMinimumPoolSize(-1);
  }

  @Test(expected = IllegalStateException.class)
  public void returnConnectionOpenTransaction() throws DatabaseException {
    final ConnectionPoolImpl pool = new ConnectionPoolImpl(createConnectionProvider(User.UNIT_TEST_USER));
    final DatabaseConnection connection = pool.getDatabaseConnection();
    try {
      connection.beginTransaction();
      pool.returnConnection(connection);
    }
    finally {
      connection.rollbackTransaction();
      pool.close();
    }
  }

  @Test(expected = IllegalStateException.class)
  public void getConnectionClosedPool() throws DatabaseException {
    final ConnectionPool pool = new ConnectionPoolImpl(createConnectionProvider(User.UNIT_TEST_USER));
    try {
      pool.close();
      pool.getConnection();
    }
    finally {
      pool.close();
    }
  }

  @Test(expected = ConnectionPoolException.NoConnectionAvailable.class)
  public void noConnectionAvailable() throws DatabaseException {
    final User user = User.UNIT_TEST_USER;
    final ConnectionPoolImpl pool = new ConnectionPoolImpl(createConnectionProvider(user));
    pool.setMaximumCheckOutTime(50);
    pool.setNewConnectionThreshold(40);
    pool.getConnection();
    pool.getConnection();
    pool.getConnection();
    pool.getConnection();
    pool.getConnection();
    pool.getConnection();
    pool.getConnection();
    pool.getConnection();
    pool.getConnection();
  }

  @Test
  public void test() throws Exception {
    final Date startDate = new Date();
    final User user = User.UNIT_TEST_USER;
    final ConnectionPoolImpl pool = new ConnectionPoolImpl(createConnectionProvider(user));
    try {
      assertEquals(user, pool.getUser());
      assertEquals(ConnectionPoolImpl.DEFAULT_CLEANUP_INTERVAL_MS, pool.getCleanupInterval());
      assertEquals(ConnectionPoolImpl.DEFAULT_CONNECTION_TIMEOUT_MS, pool.getConnectionTimeout());
      assertEquals(ConnectionPoolImpl.DEFAULT_MAXIMUM_POOL_SIZE, pool.getMaximumPoolSize());
      final int minimumPoolSize = ConnectionPoolImpl.DEFAULT_MAXIMUM_POOL_SIZE / 2;
      assertEquals(minimumPoolSize, pool.getMinimumPoolSize());
      assertEquals(minimumPoolSize, pool.getStatistics(System.currentTimeMillis()).getAvailable());
      pool.setMaximumPoolSize(6);
      assertEquals(6, pool.getMaximumPoolSize());
      pool.setMinimumPoolSize(3);
      assertEquals(3, pool.getMinimumPoolSize());

      pool.setCollectFineGrainedStatistics(true);
      assertTrue(pool.isCollectFineGrainedStatistics());
      ConnectionPoolStatistics statistics = pool.getStatistics(startDate.getTime());
      assertEquals(new User(User.UNIT_TEST_USER.getUsername(), null), statistics.getUser());
      assertNotNull(statistics.getTimestamp());
      assertNotNull(statistics.getCreationDate());
      assertEquals(0, statistics.getDestroyed());
      assertEquals(0, statistics.getRequests());
      assertEquals(4, statistics.getAvailable());
      assertEquals(0, statistics.getInUse());

      final DatabaseConnection dbConnectionOne = pool.getDatabaseConnection();
      assertTrue(dbConnectionOne.isValid());
      statistics = pool.getStatistics(startDate.getTime());
      assertEquals(1, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(3, statistics.getAvailable());
      assertEquals(1, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      final DatabaseConnection dbConnectionTwo = pool.getDatabaseConnection();
      assertTrue(dbConnectionTwo.isValid());
      statistics = pool.getStatistics(startDate.getTime());
      assertEquals(2, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(2, statistics.getAvailable());
      assertEquals(2, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      pool.returnConnection(dbConnectionOne);
      statistics = pool.getStatistics(startDate.getTime());
      assertEquals(2, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(3, statistics.getAvailable());
      assertEquals(1, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      final DatabaseConnection dbConnectionThree = pool.getDatabaseConnection();
      assertTrue(dbConnectionThree.isValid());
      statistics = pool.getStatistics(startDate.getTime());
      assertEquals(3, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(2, statistics.getAvailable());
      assertEquals(2, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      pool.returnConnection(dbConnectionTwo);
      statistics = pool.getStatistics(startDate.getTime());
      assertEquals(3, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(3, statistics.getAvailable());
      assertEquals(1, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      pool.returnConnection(dbConnectionThree);
      statistics = pool.getStatistics(startDate.getTime());
      assertEquals(3, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(4, statistics.getAvailable());
      assertEquals(0, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      assertTrue(statistics.getFineGrainedStatistics().size() > 0);

      final DatabaseConnection dbConnectionFour = pool.getDatabaseConnection();
      statistics = pool.getStatistics(startDate.getTime());
      assertEquals(4, statistics.getRequests());
      assertEquals(4, statistics.getCreated());
      assertEquals(3, statistics.getAvailable());
      assertEquals(1, statistics.getInUse());
      assertEquals(4, statistics.getSize());

      dbConnectionFour.disconnect();
      pool.returnConnection(dbConnectionFour);
      statistics = pool.getStatistics(startDate.getTime());
      assertEquals(4, statistics.getCreated());
      assertEquals(3, statistics.getAvailable());
      assertEquals(0, statistics.getInUse());
      assertEquals(3, statistics.getSize());

      assertNotNull(statistics.getRequestsPerSecond());
      assertNotNull(statistics.getDelayedRequestsPerSecond());
      assertNotNull(statistics.getDelayedRequests());

      final List<ConnectionPoolState> states = statistics.getFineGrainedStatistics();
      assertFalse(states.isEmpty());
      final ConnectionPoolState state = states.get(0);
      assertTrue(state.getSize() != -1);
      assertTrue(state.getInUse() != -1);
      assertTrue(state.getWaiting() == 0);//not implemented

      pool.resetStatistics();
      statistics = pool.getStatistics(startDate.getTime());
      assertEquals(0, statistics.getRequests());
      assertEquals(0, statistics.getCreated());
      assertNotNull(statistics.getResetTime());

      pool.setCollectFineGrainedStatistics(false);
      statistics = pool.getStatistics(startDate.getTime());
      assertTrue(statistics.getFineGrainedStatistics().isEmpty());

      pool.close();
      assertEquals(0, pool.getStatistics(System.currentTimeMillis()).getAvailable());
      pool.returnConnection(dbConnectionThree);
    }
    finally {
      pool.close();
    }
  }

  private static DatabaseConnectionProvider createConnectionProvider(final User user) {
    return new DatabaseConnectionProvider() {
      final Database database = Databases.createInstance();
      @Override
      public DatabaseConnection createConnection() throws DatabaseException {
        return DatabaseConnections.createConnection(database, user);
      }
      @Override
      public void destroyConnection(final DatabaseConnection connection) {
        connection.disconnect();
      }
      @Override
      public User getUser() {
        return user;
      }
    };
  }
}
