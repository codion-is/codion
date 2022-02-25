/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.swing.framework.server.monitor.ConnectionPoolMonitor;
import is.codion.swing.framework.server.monitor.DatabaseMonitor;
import is.codion.swing.framework.server.monitor.EntityServerMonitor;
import is.codion.swing.framework.server.monitor.EntityServerMonitorTest;
import is.codion.swing.framework.server.monitor.ServerMonitor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EntityServerMonitorPanelTest {

  @BeforeAll
  public static void setUp() throws Exception {
    EntityServerMonitorTest.setUp();
  }

  @AfterAll
  public static void tearDown() throws Exception {
    EntityServerMonitorTest.tearDown();
  }

  @Test
  void test() throws Exception {
    EntityServerMonitorPanel panel = new EntityServerMonitorPanel(new EntityServerMonitor("localhost",
            EntityServerMonitorTest.CONFIGURATION.getRegistryPort(), EntityServerMonitorTest.CONFIGURATION.getAdminUser()));
    ServerMonitor serverMonitor = panel.getModel().getHostMonitors().iterator().next().getServerMonitors().iterator().next();
    serverMonitor.getUpdateIntervalValue().set(1);
    DatabaseMonitor databaseMonitor = serverMonitor.getDatabaseMonitor();
    databaseMonitor.getUpdateIntervalValue().set(1);
    ConnectionPoolMonitor poolMonitor = databaseMonitor.getConnectionPoolMonitor().getConnectionPoolInstanceMonitors().iterator().next();
    poolMonitor.getCollectSnapshotStatisticsState().set(true);
    poolMonitor.getCollectCheckOutTimesState().set(true);
    poolMonitor.getUpdateIntervalValue().set(1);

    Thread.sleep(1000);
    poolMonitor.clearStatistics();
    poolMonitor.clearInPoolStatistics();
    serverMonitor.shutdown();
    Thread.sleep(500);
  }
}
