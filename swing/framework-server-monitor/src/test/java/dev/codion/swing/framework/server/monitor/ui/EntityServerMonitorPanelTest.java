/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.server.monitor.ui;

import dev.codion.swing.framework.server.monitor.ConnectionPoolMonitor;
import dev.codion.swing.framework.server.monitor.DatabaseMonitor;
import dev.codion.swing.framework.server.monitor.EntityServerMonitor;
import dev.codion.swing.framework.server.monitor.EntityServerMonitorTest;
import dev.codion.swing.framework.server.monitor.ServerMonitor;

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
  public void test() throws Exception {
    final EntityServerMonitorPanel panel = new EntityServerMonitorPanel(new EntityServerMonitor("localhost",
            EntityServerMonitorTest.CONFIGURATION.getRegistryPort(), EntityServerMonitorTest.CONFIGURATION.getAdminUser()));
    final ServerMonitor serverMonitor = panel.getModel().getHostMonitors().iterator().next().getServerMonitors().iterator().next();
    serverMonitor.getUpdateIntervalValue().set(1);
    final DatabaseMonitor databaseMonitor = serverMonitor.getDatabaseMonitor();
    databaseMonitor.getUpdateIntervalValue().set(1);
    final ConnectionPoolMonitor poolMonitor = databaseMonitor.getConnectionPoolMonitor().getConnectionPoolInstanceMonitors().iterator().next();
    poolMonitor.setCollectSnapshotStatistics(true);
    poolMonitor.getUpdateIntervalValue().set(1);

    Thread.sleep(1000);
    poolMonitor.clearStatistics();
    poolMonitor.clearInPoolStatistics();
    serverMonitor.shutdown();
    Thread.sleep(500);
  }
}
