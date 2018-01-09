/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.common.server.Server;
import org.jminor.swing.framework.server.monitor.ConnectionPoolMonitor;
import org.jminor.swing.framework.server.monitor.DatabaseMonitor;
import org.jminor.swing.framework.server.monitor.EntityServerMonitor;
import org.jminor.swing.framework.server.monitor.EntityServerMonitorTest;
import org.jminor.swing.framework.server.monitor.ServerMonitor;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EntityServerMonitorPanelTest {

  @BeforeClass
  public static void setUp() throws Exception {
    EntityServerMonitorTest.setUp();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    EntityServerMonitorTest.tearDown();
  }

  @Test
  public void test() throws Exception {
    final EntityServerMonitorPanel panel = new EntityServerMonitorPanel(new EntityServerMonitor("localhost",
            Server.REGISTRY_PORT.get()));
    final ServerMonitor serverMonitor = panel.getModel().getHostMonitors().iterator().next().getServerMonitors().iterator().next();
    serverMonitor.getUpdateScheduler().setInterval(350);
    final DatabaseMonitor databaseMonitor = serverMonitor.getDatabaseMonitor();
    databaseMonitor.getUpdateScheduler().setInterval(350);
    final ConnectionPoolMonitor poolMonitor = databaseMonitor.getConnectionPoolMonitor().getConnectionPoolInstanceMonitors().iterator().next();
    poolMonitor.setCollectFineGrainedStatistics(true);
    poolMonitor.getUpdateScheduler().setInterval(300);

    Thread.sleep(1000);
    poolMonitor.resetStatistics();
    poolMonitor.resetInPoolStatistics();
    serverMonitor.shutdown();
    Thread.sleep(500);
  }
}
