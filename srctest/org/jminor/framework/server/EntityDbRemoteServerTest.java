/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.DatabaseStatistics;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.LogEntry;
import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
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
import java.util.UUID;

public class EntityDbRemoteServerTest {

  private static SecurityManager defaultManager;
  private static EntityDbRemoteServer server;
  private static EntityDbRemoteServerAdmin admin;

  @BeforeClass
  public static void setUp() throws Exception {
    EmpDept.init();
    Petstore.init();
    defaultManager = System.getSecurityManager();
    Configuration.class.getName();
    Configuration.setValue(Configuration.SERVER_PORT, "2222");
    Configuration.setValue(Configuration.SERVER_DB_PORT, "2223");
    Configuration.setValue(Configuration.SERVER_ADMIN_PORT, "3334");
    Configuration.setValue(Configuration.SERVER_HOST_NAME, "localhost");
    Configuration.setValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL, User.UNIT_TEST_USER.getUsername());
    Configuration.setValue(Configuration.SERVER_CONNECTION_SSL_ENABLED, true);
    Configuration.setValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES, "org.jminor.framework.demos.empdept.domain.EmpDept,org.jminor.framework.demos.petstore.domain.Petstore");
    Configuration.setValue("java.rmi.server.hostname", "localhost");
    Configuration.setValue("java.security.policy", "resources/security/all_permissions.policy");
    Configuration.setValue("javax.net.ssl.trustStore", "resources/security/JMinorClientTruststore");
    Configuration.setValue("javax.net.ssl.keyStore", "resources/security/JMinorServerKeystore");
    Configuration.setValue("javax.net.ssl.keyStorePassword", "jminor");
    if (server != null) {
      throw new RuntimeException("Server not torn down after last run");
    }
    try {
      server = new EntityDbRemoteServer(DatabaseProvider.createInstance());
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
    if (admin != null) {
      throw new RuntimeException("Server admin not torn down after last run");
    }
    admin = new EntityDbRemoteServerAdmin(server, EntityDbRemoteServer.SSL_CONNECTION_ENABLED);
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

  @Test
  public void test() throws Exception {
    final EntityDbRemoteProvider providerOne = new EntityDbRemoteProvider(User.UNIT_TEST_USER,
            UUID.randomUUID(), getClass().getSimpleName());
    final EntityDb remoteDbOne = providerOne.getEntityDb();
    assertTrue(remoteDbOne.isConnectionValid());
    assertEquals(1, server.getConnectionCount());

    final EntityDbRemoteProvider providerTwo = new EntityDbRemoteProvider(User.UNIT_TEST_USER,
            UUID.randomUUID(), getClass().getSimpleName());
    final EntityDb remoteDbTwo = providerTwo.getEntityDb();
    server.setLoggingOn(providerTwo.getClientID(), true);
    assertTrue(server.isLoggingOn(providerTwo.getClientID()));
    assertTrue(remoteDbTwo.isConnectionValid());
    assertEquals(2, server.getConnectionCount());

    final Collection<ClientInfo> clients = admin.getClients(new User(User.UNIT_TEST_USER.getUsername(), null));
    assertEquals(2, clients.size());

    providerTwo.getEntityDb().selectAll(EmpDept.T_EMPLOYEE);

    final DatabaseStatistics stats = admin.getDatabaseStatistics();
    assertNotNull(stats.getTimestamp());
    assertNotNull(stats.getQueriesPerSecond());

    final ServerLog log = server.getServerLog(providerTwo.getClientID());
    assertEquals("selectAll", log.getLastAccessedMethod());
    assertEquals("selectAll", log.getLastExitedMethod());
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
  }
}
