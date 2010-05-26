/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.DbConnectionProvider;
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

public class DbConnectionPoolTest {

  @Test
  public void loadTest() throws Exception {
    final Date startTime = new Date();
    final DbConnectionPool pool = initializeLoadTestPool();
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
    }, startTime, 500);
    model.addApplications();
    model.setCollectChartData(true);
    Thread.sleep(4200);
    model.exit();
    pool.close();
    Thread.sleep(1000);
    final ConnectionPoolStatistics statistics = pool.getConnectionPoolStatistics(startTime.getTime());
    assertEquals(statistics.getConnectionsCreated(), statistics.getConnectionsDestroyed());
  }

  @Test
  public void test() throws Exception {
    final Date startDate = new Date();
    final User user = User.UNIT_TEST_USER;
    final ConnectionPoolSettings settings = ConnectionPoolSettings.getDefault(user);
    assertEquals(60000, settings.getPooledConnectionTimeout());
    final DbConnectionPool pool = new DbConnectionPool(createConnectionProvider(), settings);
    pool.getConnectionPoolSettings().getUser().setPassword(User.UNIT_TEST_USER.getPassword());
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
    pool.checkInConnection(dbConnectionThree);
    try {
      pool.checkOutConnection();
      fail();
    }
    catch (IllegalStateException e) {}
  }

  private LoadTestModel initializeLoadTestModel(final DbConnectionPool pool) {
    return new LoadTestModel(User.UNIT_TEST_USER, 200, 1, 20, 20) {
      @Override
      protected void disconnectApplication(Object application) {}

      @Override
      protected Object initializeApplication() throws CancelException {
        return new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            try {
              DbConnection connection = null;
              try {
                connection = pool.checkOutConnection();
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
                if (connection != null)
                  pool.checkInConnection(connection);
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

  private DbConnectionPool initializeLoadTestPool() {
    return new DbConnectionPool(createConnectionProvider(),
            new ConnectionPoolSettings(User.UNIT_TEST_USER, true, 70, 1, 200));
  }

  private static DbConnectionProvider createConnectionProvider() {
    return new DbConnectionProvider() {
      final Database database = DatabaseProvider.createInstance();
      public DbConnection createConnection(final User user) throws ClassNotFoundException, SQLException {
        return new DbConnection(database, user);
      }
      public void destroyConnection(final DbConnection connection) {
        connection.disconnect();
      }
    };
  }
}
