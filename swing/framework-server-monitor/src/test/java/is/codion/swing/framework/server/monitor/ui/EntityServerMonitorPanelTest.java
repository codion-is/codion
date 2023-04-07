/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
            EntityServerMonitorTest.CONFIGURATION.registryPort(), EntityServerMonitorTest.CONFIGURATION.adminUser()));
    ServerMonitor serverMonitor = panel.model().hostMonitors().iterator().next().serverMonitors().iterator().next();
    serverMonitor.updateIntervalValue().set(1);
    DatabaseMonitor databaseMonitor = serverMonitor.databaseMonitor();
    databaseMonitor.updateIntervalValue().set(1);
    ConnectionPoolMonitor poolMonitor = databaseMonitor.connectionPoolMonitor().connectionPoolInstanceMonitors().iterator().next();
    poolMonitor.collectSnapshotStatisticsState().set(true);
    poolMonitor.collectCheckOutTimesState().set(true);
    poolMonitor.updateIntervalValue().set(1);

    Thread.sleep(1000);
    poolMonitor.resetStatistics();
    poolMonitor.clearStatistics();
    serverMonitor.shutdown();
    Thread.sleep(500);
  }
}
