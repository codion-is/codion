/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.server.EntityConnectionServerTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class EntityServerMonitorTest {

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
    final User user = new User("scott", "tiger");
    final UUID clientId = UUID.randomUUID();
    final String clientTypeId = EntityServerMonitorTest.class.getName();
    final RemoteEntityConnectionProvider connectionProvider = new RemoteEntityConnectionProvider(user, clientId, clientTypeId);
    connectionProvider.getConnection();
    final EntityServerMonitor model = new EntityServerMonitor("localhost", Configuration.getIntValue(Configuration.REGISTRY_PORT));
    model.refresh();
    final HostMonitor hostMonitor = model.getHostMonitors().iterator().next();
    assertEquals("localhost", hostMonitor.getHostName());
    hostMonitor.refresh();
    final ServerMonitor serverMonitor = hostMonitor.getServerMonitors().iterator().next();
    assertNotNull(serverMonitor);
    final ClientUserMonitor clientUserMonitor = serverMonitor.getClientMonitor();
    clientUserMonitor.refresh();
    assertEquals(1, clientUserMonitor.getUserListModel().size());
    assertEquals(1, clientUserMonitor.getClientTypeListModel().size());
    final ClientMonitor clientMonitor = clientUserMonitor.getClientTypeListModel().firstElement();
    assertEquals(clientTypeId, clientMonitor.getClientTypeID());
    clientMonitor.refresh();
    assertEquals(1, clientMonitor.getClientInstanceListModel().size());
    final ClientInstanceMonitor clientInstanceMonitor = clientMonitor.getClientInstanceListModel().firstElement();
    final ClientInfo clientInfo = clientInstanceMonitor.getClientInfo();
    assertEquals(clientId, clientInfo.getClientID());
    assertEquals(user, clientInfo.getUser());

    clientInstanceMonitor.disconnect();//disconnects the client

    clientMonitor.refresh();
    assertTrue(clientMonitor.getClientInstanceListModel().isEmpty());
    clientUserMonitor.refresh();
    assertTrue(clientUserMonitor.getUserListModel().isEmpty());
    assertTrue(clientUserMonitor.getClientTypeListModel().isEmpty());

    serverMonitor.shutdown();
  }
}
