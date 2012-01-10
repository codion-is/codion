/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.framework.server.EntityConnectionServerTest;
import org.jminor.framework.server.monitor.ConnectionPoolMonitor;
import org.jminor.framework.server.monitor.DatabaseMonitor;
import org.jminor.framework.server.monitor.EntityServerMonitor;
import org.jminor.framework.server.monitor.ServerMonitor;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.registry.Registry;

public class EntityServerMonitorPanelTest {

  @BeforeClass
  public static void setUp() throws Exception {
    EntityConnectionServerTest.setUp();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    EntityConnectionServerTest.tearDown();
  }

  @Test
  public void test() throws Exception {
    final EntityServerMonitorPanel panel = new EntityServerMonitorPanel(new EntityServerMonitor("localhost", new int[] {Registry.REGISTRY_PORT}));
    final ServerMonitor serverMonitor = panel.getModel().getHostMonitors().iterator().next().getServerMonitors().iterator().next();
    serverMonitor.setStatsUpdateInterval(350);
    final DatabaseMonitor databaseMonitor = serverMonitor.getDatabaseMonitor();
    databaseMonitor.setStatsUpdateInterval(350);
    final ConnectionPoolMonitor poolMonitor = databaseMonitor.getConnectionPoolMonitor().getConnectionPoolInstanceMonitors().iterator().next();
    poolMonitor.setCollectFineGrainedStatistics(true);
    poolMonitor.setStatisticsUpdateInterval(300);

    Thread.sleep(1000);
    poolMonitor.resetStatistics();
    poolMonitor.resetInPoolStatistics();

    poolMonitor.shutdown();
    Thread.sleep(500);

    serverMonitor.shutdownServer();
  }
}
