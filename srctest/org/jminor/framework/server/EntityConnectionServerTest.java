/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.model.LogEntry;
import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.server.provider.RemoteEntityConnectionProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.*;

public class EntityConnectionServerTest {

  private static SecurityManager defaultManager;
  private static EntityConnectionServer server;
  private static EntityConnectionServerAdminImpl admin;

  @BeforeClass
  public static void setUp() throws Exception {
    defaultManager = System.getSecurityManager();
    Configuration.init();
    Configuration.setValue(Configuration.SERVER_HOST_NAME, "localhost");
    Configuration.setValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL, User.UNIT_TEST_USER.getUsername() + ":" + User.UNIT_TEST_USER.getPassword());
    Configuration.setValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES, "org.jminor.framework.demos.empdept.domain.EmpDept");
    Configuration.setValue("java.rmi.server.hostname", "localhost");
    Configuration.setValue("java.security.policy", "resources/security/all_permissions.policy");
    Configuration.setValue("javax.net.ssl.trustStore", "resources/security/JMinorClientTruststore");
    Configuration.setValue("javax.net.ssl.keyStore", "resources/security/JMinorServerKeystore");
    Configuration.setValue("javax.net.ssl.keyStorePassword", "jminor");
    final String testServerName = Configuration.getStringValue(Configuration.SERVER_NAME_PREFIX) + " unit test server";
    if (server != null) {
      throw new RuntimeException("Server not torn down after last run");
    }
    try {
      server = new EntityConnectionServer(testServerName, 2222, 1099, Databases.createInstance(), true, -1);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
    if (admin != null) {
      throw new RuntimeException("Server admin not torn down after last run");
    }
    admin = new EntityConnectionServerAdminImpl(server, 3334);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (admin != null) {
      admin.shutdown();
    }
    Thread.sleep(300);
    admin = null;
    server = null;
    System.setSecurityManager(defaultManager);
  }

  @Test(expected = RuntimeException.class)
  public void testWrongPassword() throws Exception {
    new RemoteEntityConnectionProvider(new User(User.UNIT_TEST_USER.getUsername(), "foobar"), UUID.randomUUID(), getClass().getSimpleName()).getConnection();
  }

  @Test
  public void test() throws Exception {
    final RemoteEntityConnectionProvider providerOne = new RemoteEntityConnectionProvider(User.UNIT_TEST_USER,
            UUID.randomUUID(), getClass().getSimpleName());
    final EntityConnection remoteDbOne = providerOne.getConnection();
    assertTrue(remoteDbOne.isValid());
    assertEquals(1, server.getConnectionCount());

    final RemoteEntityConnectionProvider providerTwo = new RemoteEntityConnectionProvider(User.UNIT_TEST_USER,
            UUID.randomUUID(), getClass().getSimpleName());
    final EntityConnection remoteDbTwo = providerTwo.getConnection();
    server.setLoggingEnabled(providerTwo.getClientID(), true);
    assertTrue(server.isLoggingEnabled(providerTwo.getClientID()));
    assertTrue(remoteDbTwo.isValid());
    assertEquals(2, server.getConnectionCount());

    final Collection<ClientInfo> clients = admin.getClients(new User(User.UNIT_TEST_USER.getUsername(), null));
    assertEquals(2, clients.size());

    providerTwo.getConnection().selectAll(EmpDept.T_EMPLOYEE);

    final Database.Statistics stats = admin.getDatabaseStatistics();
    assertNotNull(stats.getTimestamp());
    assertNotNull(stats.getQueriesPerSecond());

    final ServerLog log = server.getServerLog(providerTwo.getClientID());
    assertEquals("returnConnection", log.getLastAccessedMethod());
    assertEquals("returnConnection", log.getLastExitedMethod());
    assertTrue(log.getLastDelta() >= 0);
    assertNotNull(log.getConnectionCreationDate());
    assertNotNull(log.getLastAccessDate());
    assertNotNull(log.getLastAccessDateFormatted());
    assertNotNull(log.getLastAccessMessage());
    assertNotNull(log.getLastExitDateFormatted());
    assertNotNull(log.getLastExitDate());

    final LogEntry entry = log.getLog().get(0);
    assertEquals("getConnection", entry.getMethod());
    assertNotNull(entry.getDelta());
    assertNotNull(entry.getEntryTimeFormatted());
    assertNotNull(entry.getExitTimeFormatted());

    providerOne.disconnect();
    assertEquals(1, server.getConnectionCount());

    providerTwo.disconnect();
    assertEquals(0, server.getConnectionCount());

    server.setConnectionLimit(1);
    providerOne.getConnection();
    try {
      providerTwo.getConnection();
      fail("Server should be full");
    }
    catch (RuntimeException e) {}

    assertEquals(1, server.getConnectionCount());
    server.setConnectionLimit(2);
    providerTwo.getConnection();
    assertEquals(2, server.getConnectionCount());

    providerOne.disconnect();
    assertEquals(1, server.getConnectionCount());
    providerTwo.disconnect();
    assertEquals(0, server.getConnectionCount());
  }
}
