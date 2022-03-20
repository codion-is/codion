/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.rmi;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;

import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.db.condition.Conditions.where;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Bjorn Darri
 * Date: 1.4.2010
 * Time: 22:44:24
 */
public class RemoteEntityConnectionProviderTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() throws DatabaseException, RemoteException, ServerAuthenticationException, NotBoundException {
    EntityServerConfiguration configuration = configure();

    String serverName = configuration.getServerName();
    EntityServer.startServer(configuration);
    Server<RemoteEntityConnection, EntityServerAdmin> server = (Server<RemoteEntityConnection, EntityServerAdmin>)
            LocateRegistry.getRegistry(Clients.SERVER_HOST_NAME.get(), configuration.getRegistryPort()).lookup(serverName);
    EntityServerAdmin admin = server.getServerAdmin(User.parse("scott:tiger"));

    RemoteEntityConnectionProvider provider = RemoteEntityConnectionProvider.builder()
            .serverHostName(Clients.SERVER_HOST_NAME.get())
            .serverPort(configuration.getServerPort())
            .registryPort(configuration.getRegistryPort())
            .clientTypeId("RemoteEntityConnectionProviderTest")
            .domainClassName("is.codion.framework.db.rmi.TestDomain")
            .user(User.parse("scott:tiger"))
            .build();

    EntityConnection connection = provider.getConnection();
    connection.select(condition(TestDomain.T_DEPARTMENT));

    assertThrows(DatabaseException.class, () -> connection.delete(where(TestDomain.EMP_NAME).equalTo("JONES")));

    admin.shutdown();

    assertFalse(connection.isConnected());

    provider.close();

    assertTrue(provider.isConnected());

    assertThrows(RuntimeException.class, provider::getConnection);
  }

  @Test
  void entityConnectionProviderBuilder() {
    String previousValue = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
    EntityConnectionProvider connectionProvider = EntityConnectionProvider.builder()
            .domainClassName(Domain.class.getName())
            .clientTypeId("test")
            .user(UNIT_TEST_USER)
            .build();
    assertTrue(connectionProvider instanceof RemoteEntityConnectionProvider);
    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_REMOTE, connectionProvider.getConnectionType());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(previousValue);
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOST_NAME.set("localhost");
    Clients.TRUSTSTORE.set("../server/src/main/config/truststore.jks");
    Clients.TRUSTSTORE_PASSWORD.set("crappypass");
    Clients.resolveTrustStore();
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    ServerConfiguration.KEYSTORE.set("../server/src/main/config/keystore.jks");
    ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .adminUser(User.parse("scott:tiger"))
            .database(DatabaseFactory.getDatabase())
            .domainModelClassNames(singletonList("is.codion.framework.db.rmi.TestDomain"))
            .sslEnabled(true)
            .build();
  }
}
