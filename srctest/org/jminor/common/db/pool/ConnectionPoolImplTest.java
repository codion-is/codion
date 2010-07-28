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

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectionPoolImplTest {

  private static final int PRINT_PERIOD = 860;
  private static final int SLEEP_MILLIS = 4200;
  private static final int CLOSE_SLEEP_MILLIS = 2500;

  @Test
  public void loadTest() throws Exception {
    final Date startTime = new Date();
    final ConnectionPoolImpl pool = initializeLoadTestPool();
    final LoadTestModel model = initializeLoadTestModel(pool);

    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        System.out.println("created: " + pool.getConnectionPoolStatistics(startTime.getTime()).getConnectionsCreated());
        System.out.println("requests: " + pool.getConnectionPoolStatistics(startTime.getTime()).getConnectionRequests());
        System.out.println("delayed: " + pool.getConnectionPoolStatistics(startTime.getTime()).getConnectionRequestsDelayed());
        System.out.println("destroyed: " + pool.getConnectionPoolStatistics(startTime.getTime()).getConnectionsDestroyed());
        System.out.println("####################################");
      }
    }, startTime, PRINT_PERIOD);
    model.addApplicationBatch();
    model.setCollectChartData(true);
    Thread.sleep(SLEEP_MILLIS);
    model.exit();
    pool.close();
    Thread.sleep(CLOSE_SLEEP_MILLIS);
    final ConnectionPoolStatistics statistics = pool.getConnectionPoolStatistics(startTime.getTime());
    assertEquals(statistics.getConnectionsCreated(), statistics.getConnectionsDestroyed());
  }

  @Test
  public void test() throws Exception {
    final Date startDate = new Date();
    final User user = User.UNIT_TEST_USER;
    final ConnectionPoolImpl pool = new ConnectionPoolImpl(createConnectionProvider(), user);
    pool.getUser().setPassword(User.UNIT_TEST_USER.getPassword());
    assertEquals(user, pool.getUser());
    pool.setCollectFineGrainedStatistics(true);
    assertTrue(pool.isCollectFineGrainedStatistics());
    ConnectionPoolStatistics statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(new User(User.UNIT_TEST_USER.getUsername(), null), statistics.getUser());
    assertNotNull(statistics.getTimestamp());
    assertNotNull(statistics.getCreationDate());
    assertEquals(0, statistics.getConnectionsDestroyed());
    assertEquals(0, statistics.getConnectionRequests());
    assertEquals(0, statistics.getAvailableInPool());
    assertEquals(0, statistics.getConnectionsInUse());

    final PoolableConnection dbConnectionOne = pool.checkOutConnection();
    assertTrue(dbConnectionOne.isConnectionValid());
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(1, statistics.getConnectionRequests());
    assertEquals(1, statistics.getConnectionsCreated());
    assertEquals(0, statistics.getAvailableInPool());
    assertEquals(1, statistics.getConnectionsInUse());
    assertEquals(1, statistics.getLiveConnectionCount());

    final PoolableConnection dbConnectionTwo = pool.checkOutConnection();
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

    final PoolableConnection dbConnectionThree = pool.checkOutConnection();
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

    final PoolableConnection dbConnectionFour = pool.checkOutConnection();
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

    final List<ConnectionPoolState> states = statistics.getPoolStatistics();
    assertFalse(states.isEmpty());
    final ConnectionPoolState state = states.get(0);
    assertTrue(state.getConnectionCount() != -1);
    assertTrue(state.getConnectionsInUse() != -1);

    pool.resetPoolStatistics();
    statistics = pool.getConnectionPoolStatistics(startDate.getTime());
    assertEquals(0, statistics.getConnectionRequests());
    assertEquals(0, statistics.getConnectionsCreated());
    assertNotNull(statistics.getResetDate());

    pool.setEnabled(false);
    assertEquals(0, pool.getConnectionPoolStatistics(System.currentTimeMillis()).getAvailableInPool());
    pool.checkInConnection(dbConnectionThree);
    try {
      pool.checkOutConnection();
      fail();
    }
    catch (IllegalStateException e) {}
  }

  private LoadTestModel initializeLoadTestModel(final ConnectionPoolImpl pool) {
    return new LoadTestModel(User.UNIT_TEST_USER, 200, 1, 20, 20) {
      @Override
      protected void disconnectApplication(Object application) {}

      @Override
      protected Object initializeApplication() throws CancelException {
        return new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            try {
              DbConnection connection = null;
              try {
                connection = (DbConnection) pool.checkOutConnection();
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
                  pool.checkInConnection(connection);
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

  private ConnectionPoolImpl initializeLoadTestPool() {
    final ConnectionPoolImpl pool = new ConnectionPoolImpl(createConnectionProvider(), User.UNIT_TEST_USER);
    pool.setPooledConnectionTimeout(70);
    pool.setMinimumPoolSize(1);
    pool.setPoolCleanupInterval(200);

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
