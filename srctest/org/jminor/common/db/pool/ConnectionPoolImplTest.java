/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.DbConnectionImpl;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.LoadTestModel;
import org.jminor.common.model.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConnectionPoolImplTest {

  private static final int SLEEP_MILLIS = 2000;
  private static final int CLOSE_SLEEP_MILLIS = 2500;

  @Test
  public void loadTest() throws Exception {
    final Date startTime = new Date();
    final ConnectionPool pool = initializeLoadTestPool();
    final LoadTestModel model = initializeLoadTestModel(pool);
    model.addApplicationBatch();
    model.setCollectChartData(true);
    Thread.sleep(SLEEP_MILLIS);
    model.exit();
    pool.close();
    Thread.sleep(CLOSE_SLEEP_MILLIS);
    final ConnectionPoolStatistics statistics = pool.getStatistics(startTime.getTime());
    assertTrue(statistics.getAverageGetTime() == 0);
    assertEquals(statistics.getCreated(), statistics.getDestroyed());
  }

  @Test
  public void test() throws Exception {
    final Date startDate = new Date();
    final User user = User.UNIT_TEST_USER;
    final ConnectionPool pool = new ConnectionPoolImpl(createConnectionProvider(), user);
    assertTrue(pool.isEnabled());
    pool.getUser().setPassword(User.UNIT_TEST_USER.getPassword());
    assertEquals(user, pool.getUser());
    assertEquals(ConnectionPoolImpl.DEFAULT_CLEANUP_INTERVAL_MS, pool.getCleanupInterval());
    assertEquals(ConnectionPoolImpl.DEFAULT_CONNECTION_TIMEOUT_MS, pool.getConnectionTimeout());
    assertEquals(ConnectionPoolImpl.DEFAULT_MAXIMUM_POOL_SIZE, pool.getMaximumPoolSize());
    assertEquals(ConnectionPoolImpl.DEFAULT_MAXIMUM_POOL_SIZE / 2, pool.getMinimumPoolSize());

    try {
      pool.setMaximumPoolSize(3);
      fail();
    }
    catch (Exception e) {}
    try {
      pool.setMaximumPoolSize(-1);
      fail();
    }
    catch (Exception e) {}
    pool.setMaximumPoolSize(6);
    assertEquals(6, pool.getMaximumPoolSize());

    try {
      pool.setMinimumPoolSize(8);
      fail();
    }
    catch (Exception e) {}
    try {
      pool.setMinimumPoolSize(-1);
      fail();
    }
    catch (Exception e) {}
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
    assertEquals(0, statistics.getAvailable());
    assertEquals(0, statistics.getInUse());

    final PoolableConnection dbConnectionOne = pool.getConnection();
    assertTrue(dbConnectionOne.isValid());
    statistics = pool.getStatistics(startDate.getTime());
    assertEquals(1, statistics.getRequests());
    assertEquals(1, statistics.getCreated());
    assertEquals(0, statistics.getAvailable());
    assertEquals(1, statistics.getInUse());
    assertEquals(1, statistics.getSize());

    final PoolableConnection dbConnectionTwo = pool.getConnection();
    assertTrue(dbConnectionTwo.isValid());
    statistics = pool.getStatistics(startDate.getTime());
    assertEquals(2, statistics.getRequests());
    assertEquals(2, statistics.getCreated());
    assertEquals(0, statistics.getAvailable());
    assertEquals(2, statistics.getInUse());
    assertEquals(2, statistics.getSize());

    pool.returnConnection(dbConnectionOne);
    statistics = pool.getStatistics(startDate.getTime());
    assertEquals(2, statistics.getRequests());
    assertEquals(2, statistics.getCreated());
    assertEquals(1, statistics.getAvailable());
    assertEquals(1, statistics.getInUse());
    assertEquals(2, statistics.getSize());

    final PoolableConnection dbConnectionThree = pool.getConnection();
    assertTrue(dbConnectionThree.isValid());
    statistics = pool.getStatistics(startDate.getTime());
    assertEquals(3, statistics.getRequests());
    assertEquals(2, statistics.getCreated());
    assertEquals(0, statistics.getAvailable());
    assertEquals(2, statistics.getInUse());
    assertEquals(2, statistics.getSize());

    pool.returnConnection(dbConnectionTwo);
    statistics = pool.getStatistics(startDate.getTime());
    assertEquals(3, statistics.getRequests());
    assertEquals(2, statistics.getCreated());
    assertEquals(1, statistics.getAvailable());
    assertEquals(1, statistics.getInUse());
    assertEquals(2, statistics.getSize());

    pool.returnConnection(dbConnectionThree);
    statistics = pool.getStatistics(startDate.getTime());
    assertEquals(3, statistics.getRequests());
    assertEquals(2, statistics.getCreated());
    assertEquals(2, statistics.getAvailable());
    assertEquals(0, statistics.getInUse());
    assertEquals(2, statistics.getSize());

    assertTrue(statistics.getFineGrainedStatistics().size() > 0);

    final PoolableConnection dbConnectionFour = pool.getConnection();
    statistics = pool.getStatistics(startDate.getTime());
    assertEquals(4, statistics.getRequests());
    assertEquals(2, statistics.getCreated());
    assertEquals(1, statistics.getAvailable());
    assertEquals(1, statistics.getInUse());
    assertEquals(2, statistics.getSize());

    dbConnectionFour.disconnect();
    pool.returnConnection(dbConnectionFour);
    statistics = pool.getStatistics(startDate.getTime());
    assertEquals(2, statistics.getCreated());
    assertEquals(1, statistics.getAvailable());
    assertEquals(0, statistics.getInUse());
    assertEquals(1, statistics.getSize());

    assertNotNull(statistics.getRequestsPerSecond());
    assertNotNull(statistics.getDelayedRequestsPerSecond());
    assertNotNull(statistics.getDelayedRequests());

    final List<ConnectionPoolState> states = statistics.getFineGrainedStatistics();
    assertFalse(states.isEmpty());
    final ConnectionPoolState state = states.get(0);
    assertTrue(state.getSize() != -1);
    assertTrue(state.getInUse() != -1);

    pool.resetStatistics();
    statistics = pool.getStatistics(startDate.getTime());
    assertEquals(0, statistics.getRequests());
    assertEquals(0, statistics.getCreated());
    assertNotNull(statistics.getResetTime());

    pool.setEnabled(false);
    assertEquals(0, pool.getStatistics(System.currentTimeMillis()).getAvailable());
    pool.returnConnection(dbConnectionThree);
    try {
      pool.getConnection();
      fail();
    }
    catch (IllegalStateException e) {}
  }

  private LoadTestModel initializeLoadTestModel(final ConnectionPool pool) {
    return new LoadTestModel(User.UNIT_TEST_USER, 100, 1, 5, 20) {
      @Override
      protected void disconnectApplication(Object application) {}

      @Override
      protected Object initializeApplication() throws CancelException {
        return new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            try {
              DbConnection connection = null;
              try {
                connection = (DbConnection) pool.getConnection();
                connection.query("select * from scott.emp", new ResultPacker() {
                  public List pack(ResultSet resultSet, int fetchCount) throws SQLException {
                    while (resultSet.next()) {
                      for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                        resultSet.getObject(i);
                      }
                    }
                    return new ArrayList();
                  }
                }, 100);
              }
              finally {
                if (connection != null) {
                  pool.returnConnection(connection);
                }
              }
            }
            catch (ClassNotFoundException e1) {
              e1.printStackTrace();
            }
            catch (SQLException e1) {
              e1.printStackTrace();
            }
          }
        };
      }

      @Override
      protected void performWork(Object application) {
        ((ActionListener) application).actionPerformed(null);
      }
    };
  }

  private ConnectionPool initializeLoadTestPool() {
    final ConnectionPool pool = new ConnectionPoolImpl(createConnectionProvider(), User.UNIT_TEST_USER);
    pool.setConnectionTimeout(50);
    pool.setMinimumPoolSize(1);
    pool.setCleanupInterval(130);

    return pool;
  }

  private static PoolableConnectionProvider createConnectionProvider() {
    return new PoolableConnectionProvider() {
      final Database database = DatabaseProvider.createInstance();
      public PoolableConnection createConnection(final User user) throws ClassNotFoundException, SQLException {
        return new DbConnectionImpl(database, user);
      }
      public void destroyConnection(final PoolableConnection connection) {
        connection.disconnect();
      }
    };
  }
}
