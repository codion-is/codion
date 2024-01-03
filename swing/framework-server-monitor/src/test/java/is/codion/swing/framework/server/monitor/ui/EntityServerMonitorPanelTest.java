/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
    serverMonitor.updateInterval().set(1);
    DatabaseMonitor databaseMonitor = serverMonitor.databaseMonitor();
    databaseMonitor.updateInterval().set(1);
    ConnectionPoolMonitor poolMonitor = databaseMonitor.connectionPoolMonitor().connectionPoolInstanceMonitors().iterator().next();
    poolMonitor.collectSnapshotStatistics().set(true);
    poolMonitor.collectCheckOutTimes().set(true);
    poolMonitor.updateInterval().set(1);

    Thread.sleep(1000);
    poolMonitor.resetStatistics();
    poolMonitor.clearStatistics();
    serverMonitor.shutdown();
    Thread.sleep(500);
  }
}
