package org.jminor.framework.server;

import org.jminor.common.db.User;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.server.provider.EntityDbRemoteProvider;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class EntityDbRemoteServerTest {

  private static SecurityManager defaultManager;

  @BeforeClass
  public static void setUp() {
    defaultManager = System.getSecurityManager();
    System.setProperty(Configuration.SERVER_PORT, "2222");
    System.setProperty(Configuration.SERVER_DB_PORT, "2223");
    System.setProperty(Configuration.SERVER_HOST_NAME, "localhost");
    System.setProperty(Configuration.SERVER_CONNECTION_SSL_ENABLED, "true");
    System.setProperty("java.rmi.server.hostname", "localhost");
    System.setProperty("java.security.policy", "resources/security/all_permissions.policy");
    System.setProperty("javax.net.ssl.trustStore", "resources/security/JMinorClientTruststore");
    System.setProperty("javax.net.ssl.keyStore", "resources/security/JMinorServerKeystore");
    System.setProperty("javax.net.ssl.keyStorePassword", "jminor");
  }

  @AfterClass
  public static void tearDown() {
    System.setSecurityManager(defaultManager);
  }

  @Test
  public void test() throws Exception {
    final EntityDbRemoteServer server = new EntityDbRemoteServer(DatabaseProvider.createInstance());
    final EntityDbRemoteProvider providerOne = new EntityDbRemoteProvider(new User("scott", "tiger"),
            "UnitTestConnection0", "EntityDbRemoteServerTest");
    final EntityDb remoteDbOne = providerOne.getEntityDb();
    assertTrue(remoteDbOne.isConnectionValid());
    assertEquals(1, server.getConnectionCount());

    final EntityDbRemoteProvider providerTwo = new EntityDbRemoteProvider(new User("scott", "tiger"),
            "UnitTestConnection1", "EntityDbRemoteServerTest");
    final EntityDb remoteDbTwo = providerTwo.getEntityDb();
    server.setLoggingOn("UnitTestConnection1", true);
    assertTrue(server.isLoggingOn("UnitTestConnection1"));
    assertTrue(remoteDbTwo.isConnectionValid());
    assertEquals(2, server.getConnectionCount());

    providerOne.disconnect();
    assertEquals(1, server.getConnectionCount());

    providerTwo.disconnect();
    assertEquals(0, server.getConnectionCount());

    server.shutdown();
  }
}
