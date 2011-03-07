/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.framework.server.RemoteEntityServerTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.registry.Registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EntityServerMonitorTest {

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
    final EntityServerMonitor model = new EntityServerMonitor("localhost", new int[] {Registry.REGISTRY_PORT});
    model.refresh();
    final HostMonitor hostMonitor = model.getHostMonitors().iterator().next();
    assertEquals("localhost", hostMonitor.getHostName());
    hostMonitor.refresh();
    final ServerMonitor serverMonitor = hostMonitor.getServerMonitors().iterator().next();
    assertNotNull(serverMonitor);
    final ClientUserMonitor clientUserMonitor = serverMonitor.getClientMonitor();
    clientUserMonitor.refresh();

    serverMonitor.shutdown();
  }
}
