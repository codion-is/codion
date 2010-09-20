/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.framework.server.RemoteEntityServerTest;
import org.jminor.framework.server.monitor.ConnectionPoolMonitor;
import org.jminor.framework.server.monitor.DatabaseMonitor;
import org.jminor.framework.server.monitor.MonitorModel;
import org.jminor.framework.server.monitor.ServerMonitor;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MonitorPanelTest {

  @BeforeClass
  public static void setUp() throws Exception {
    RemoteEntityServerTest.setUp();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    RemoteEntityServerTest.tearDown();
  }

  @Test
  public void test() throws Exception {
    final EntityServerMonitorPanel panel = new EntityServerMonitorPanel(new MonitorModel("localhost"));
    final ServerMonitor serverMonitor = panel.getModel().getHostMonitors().iterator().next().getServerMonitors().iterator().next();
    serverMonitor.setStatsUpdateInterval(350);
    final DatabaseMonitor databaseMonitor = serverMonitor.getDatabaseMonitor();
    databaseMonitor.setStatsUpdateInterval(350);
    final ConnectionPoolMonitor poolMonitor = databaseMonitor.getConnectionPoolMonitor().getConnectionPoolInstanceMonitors().iterator().next();
    poolMonitor.setCollectFineGrainedStats(true);
    poolMonitor.setStatsUpdateInterval(300);

    Thread.sleep(1000);
    poolMonitor.resetStats();
    poolMonitor.resetInPoolStats();

    poolMonitor.shutdown();
    Thread.sleep(500);

    serverMonitor.shutdownServer();
  }
}
