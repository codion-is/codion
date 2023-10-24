/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.rmi;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.rmi.TestDomain.Department;
import is.codion.framework.db.rmi.TestDomain.Employee;
import is.codion.framework.domain.DomainType;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;

import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import static is.codion.framework.domain.entity.condition.Condition.all;
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

    String serverName = configuration.serverName();
    EntityServer.startServer(configuration);
    Server<RemoteEntityConnection, EntityServerAdmin> server = (Server<RemoteEntityConnection, EntityServerAdmin>)
            LocateRegistry.getRegistry(Clients.SERVER_HOSTNAME.get(), configuration.registryPort()).lookup(serverName);
    EntityServerAdmin admin = server.serverAdmin(User.parse("scott:tiger"));

    RemoteEntityConnectionProvider provider = RemoteEntityConnectionProvider.builder()
            .hostName(Clients.SERVER_HOSTNAME.get())
            .port(configuration.port())
            .registryPort(configuration.registryPort())
            .clientTypeId("RemoteEntityConnectionProviderTest")
            .domainType(TestDomain.DOMAIN)
            .user(User.parse("scott:tiger"))
            .build();

    EntityConnection connection = provider.connection();
    connection.select(all(Department.TYPE));

    assertThrows(DatabaseException.class, () -> connection.delete(Employee.NAME.equalTo("JONES")));

    admin.shutdown();

    assertFalse(connection.connected());

    provider.close();

    assertFalse(provider.connectionValid());

    assertThrows(RuntimeException.class, provider::connection);
  }

  @Test
  void entityConnectionProviderBuilder() {
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
    try {
      EntityConnectionProvider connectionProvider = EntityConnectionProvider.builder()
              .domainType(DomainType.domainType("entityConnectionProviderBuilder"))
              .clientTypeId("test")
              .user(UNIT_TEST_USER)
              .build();
      assertTrue(connectionProvider instanceof RemoteEntityConnectionProvider);
      assertEquals(EntityConnectionProvider.CONNECTION_TYPE_REMOTE, connectionProvider.connectionType());
    }
    finally {
      EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(null);
    }
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOSTNAME.set("localhost");
    Clients.TRUSTSTORE.set("../server/src/main/config/truststore.jks");
    Clients.TRUSTSTORE_PASSWORD.set("crappypass");
    Clients.resolveTrustStore();
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    ServerConfiguration.KEYSTORE.set("../server/src/main/config/keystore.jks");
    ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .adminUser(User.parse("scott:tiger"))
            .database(Database.instance())
            .domainClassNames(singletonList("is.codion.framework.db.rmi.TestDomain"))
            .sslEnabled(true)
            .build();
  }
}
