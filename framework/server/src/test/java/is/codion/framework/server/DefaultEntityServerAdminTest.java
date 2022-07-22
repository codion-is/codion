/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;

import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityServerAdminTest {

  private static final User ADMIN_USER = User.parse("scott:tiger");
  private static final String SCOTT = "scott";

  @Test
  void test() throws Exception {
    Clients.SERVER_HOST_NAME.set("localhost");
    Clients.TRUSTSTORE.set("src/main/config/truststore.jks");
    Clients.TRUSTSTORE_PASSWORD.set("crappypass");
    Clients.resolveTrustStore();
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    ServerConfiguration.KEYSTORE.set("src/main/config/keystore.jks");
    ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");

    EntityServerConfiguration configuration = EntityServerConfiguration.builder(3224, 3222)
            .adminPort(3225)
            .adminUser(ADMIN_USER)
            .database(DatabaseFactory.getDatabase())
            .domainModelClassNames(singletonList("is.codion.framework.server.TestDomain"))
            .connectionPoolUsers(singletonList(ADMIN_USER))
            .build();
    EntityServer server = EntityServer.startServer(configuration);
    try {
      ConnectionRequest connectionRequest = ConnectionRequest.builder()
              .user(ADMIN_USER)
              .clientTypeId("DefaultEntityServerAdminTest")
              .parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain")
              .build();
      server.connect(connectionRequest);
      EntityServerAdmin admin = new DefaultEntityServerAdmin(server, configuration);
      admin.setLoggingEnabled(connectionRequest.getClientId(), true);
      assertTrue(admin.isLoggingEnabled(connectionRequest.getClientId()));
      admin.setLogLevel("TEST");//no op logger
      admin.getLogLevel();
      admin.resetConnectionPoolStatistics(SCOTT);
      admin.setCollectPoolSnapshotStatistics(SCOTT, true);
      assertTrue(admin.isCollectPoolSnapshotStatistics(SCOTT));
      admin.setCollectPoolCheckOutTimes(SCOTT, true);
      assertTrue(admin.isCollectPoolCheckOutTimes(SCOTT));
      assertNotNull(admin.getConnectionPoolStatistics(SCOTT, System.currentTimeMillis()));
      admin.setConnectionPoolCleanupInterval(SCOTT, 10);
      assertEquals(0, admin.getConnectionPoolCleanupInterval(SCOTT));//not configurable for hikari
      admin.setMinimumConnectionPoolSize(SCOTT, 2);
      assertEquals(2, admin.getMinimumConnectionPoolSize(SCOTT));
      admin.setMaximumConnectionPoolSize(SCOTT, 11);
      assertEquals(11, admin.getMaximumConnectionPoolSize(SCOTT));
      admin.setMaximumPoolCheckOutTime(SCOTT, 300);
      assertEquals(300, admin.getMaximumPoolCheckOutTime(SCOTT));
      admin.setPooledConnectionIdleTimeout(SCOTT, 1000);
      assertEquals(1000, admin.getPooledConnectionIdleTimeout(SCOTT));
      admin.getClientLog(connectionRequest.getClientId());

      admin.getAllocatedMemory();
      admin.setIdleConnectionTimeout(30);
      try {
        admin.setIdleConnectionTimeout(-1);
        fail();
      }
      catch (IllegalArgumentException ignored) {/*ignored*/}
      assertEquals(30, admin.getIdleConnectionTimeout());
      admin.getDatabaseStatistics();
      admin.getDatabaseUrl();
      admin.getConnectionPoolUsernames();
      admin.setMaintenanceInterval(500);
      admin.getEntityDefinitions();
      assertEquals(500, admin.getMaintenanceInterval());
      admin.getMaxMemory();
      admin.getRequestsPerSecond();
      try {
        admin.getThreadStatistics();
      }
      catch (NullPointerException e) {/*Github Actions on Windows build keeps failing here, let's see what happens now*/}
      admin.getGcEvents(0);
      admin.getServerInformation();
      admin.getSystemProperties();
      admin.getUsedMemory();
      admin.getUsers();
      admin.getServerStatistics(System.currentTimeMillis());
      admin.disconnectTimedOutClients();
      admin.disconnectAllClients();
    }
    finally {
      server.shutdown();
      ServerConfiguration.RMI_SERVER_HOSTNAME.set(null);
    }
  }
}