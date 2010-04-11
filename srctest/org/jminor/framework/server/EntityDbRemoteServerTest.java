/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.DatabaseStatistics;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.common.server.ServerLogEntry;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.server.provider.EntityDbRemoteProvider;

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

public class EntityDbRemoteServerTest {

  private static SecurityManager defaultManager;
  private static EntityDbRemoteServer server;
  private static EntityDbRemoteServerAdmin admin;

  @BeforeClass
  public static void setUp() throws Exception {
    new EmpDept();
    new Petstore();
    defaultManager = System.getSecurityManager();
    System.setProperty(Configuration.SERVER_PORT, "2222");
    System.setProperty(Configuration.SERVER_DB_PORT, "2223");
    System.setProperty(Configuration.SERVER_ADMIN_PORT, "3334");
    System.setProperty(Configuration.SERVER_HOST_NAME, "localhost");
    System.setProperty(Configuration.SERVER_CONNECTION_SSL_ENABLED, "true");
    System.setProperty(Configuration.SERVER_DOMAIN_MODEL_CLASSES, "org.jminor.framework.demos.empdept.domain.EmpDept,org.jminor.framework.demos.petstore.domain.Petstore");
    System.setProperty("java.rmi.server.hostname", "localhost");
    System.setProperty("java.security.policy", "resources/security/all_permissions.policy");
    System.setProperty("javax.net.ssl.trustStore", "resources/security/JMinorClientTruststore");
    System.setProperty("javax.net.ssl.keyStore", "resources/security/JMinorServerKeystore");
    System.setProperty("javax.net.ssl.keyStorePassword", "jminor");
    if (server != null)
      throw new RuntimeException("Server not torn down after last run");
    server = new EntityDbRemoteServer(DatabaseProvider.createInstance());
    if (admin != null)
      throw new RuntimeException("Server admin not torn down after last run");
    admin = new EntityDbRemoteServerAdmin(server, EntityDbRemoteServer.SSL_CONNECTION_ENABLED);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (admin != null)
      admin.shutdown();
    Thread.sleep(100);
    admin = null;
    server = null;
    System.setSecurityManager(defaultManager);
  }

  @Test
  public void test() throws Exception {
    final EntityDbRemoteProvider providerOne = new EntityDbRemoteProvider(User.UNIT_TEST_USER,
            "UnitTestConnection0", "EntityDbRemoteServerTest");
    final EntityDb remoteDbOne = providerOne.getEntityDb();
    assertTrue(remoteDbOne.isConnectionValid());
    assertEquals(1, server.getConnectionCount());

    final EntityDbRemoteProvider providerTwo = new EntityDbRemoteProvider(User.UNIT_TEST_USER,
            "UnitTestConnection1", "EntityDbRemoteServerTest");
    final EntityDb remoteDbTwo = providerTwo.getEntityDb();
    server.setLoggingOn("UnitTestConnection1", true);
    assertTrue(server.isLoggingOn("UnitTestConnection1"));
    assertTrue(remoteDbTwo.isConnectionValid());
    assertEquals(2, server.getConnectionCount());

    final Collection<ClientInfo> clients = admin.getClients(new User(User.UNIT_TEST_USER.getUsername(), null));
    assertEquals(2, clients.size());

    providerTwo.getEntityDb().selectAll(EmpDept.T_EMPLOYEE);

    final DatabaseStatistics stats = admin.getDatabaseStatistics();
    assertNotNull(stats.getTimestamp());
    assertNotNull(stats.getQueriesPerSecond());
    assertNotNull(stats.getCachedQueriesPerSecond());

    final ServerLog log = server.getServerLog("UnitTestConnection1");
    assertEquals("selectAll", log.getLastAccessedMethod());
    assertEquals("selectAll", log.getLastExitedMethod());
    assertTrue(log.getLastDelta() >= 0);
    assertNotNull(log.getConnectionCreationDate());
    assertNotNull(log.getLastAccessDate());
    assertNotNull(log.getLastAccessDateFormatted());
    assertNotNull(log.getLastAccessMessage());
    assertNotNull(log.getLastExitDateFormatted());
    assertNotNull(log.getLastExitDate());

    final ServerLogEntry entry = log.getLog().get(0);
    assertEquals("selectAll", entry.getMethod());
    assertNotNull(entry.getEntryKey());
    assertNotNull(entry.getDelta());
    assertNotNull(entry.getEntryTimeFormatted());
    assertNotNull(entry.getExitTimeFormatted());

    providerOne.disconnect();
    assertEquals(1, server.getConnectionCount());

    providerTwo.disconnect();
    assertEquals(0, server.getConnectionCount());
  }
}
