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
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.Server;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.server.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.registry.Registry;
import java.util.Collection;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultRemoteEntityConnectionTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void wrongUsername() {
    RemoteClient client = RemoteClient.remoteClient(ConnectionRequest.builder()
            .user(User.user("foo", "bar".toCharArray()))
            .clientTypeId("DefaultRemoteEntityConnectionTestClient")
            .build());
    assertThrows(DatabaseException.class, () -> new DefaultRemoteEntityConnection(DOMAIN, Database.instance(), client, 1234));
  }

  @Test
  void wrongPassword() {
    RemoteClient client = RemoteClient.remoteClient(ConnectionRequest.builder()
            .user(User.user(UNIT_TEST_USER.username(), "xxxxx".toCharArray()))
            .clientTypeId("DefaultRemoteEntityConnectionTestClient")
            .build());
    assertThrows(DatabaseException.class, () -> new DefaultRemoteEntityConnection(DOMAIN, Database.instance(), client, 1235));
  }

  @Test
  void rollbackOnClose() throws Exception {
    RemoteClient client = RemoteClient.remoteClient(ConnectionRequest.builder()
            .user(UNIT_TEST_USER)
            .clientTypeId("DefaultRemoteEntityConnectionTestClient")
            .build());
    DefaultRemoteEntityConnection connection = new DefaultRemoteEntityConnection(DOMAIN, Database.instance(), client, 1238);
    Condition condition = Condition.all(Employee.TYPE);
    connection.beginTransaction();
    connection.delete(condition);
    assertTrue(connection.select(condition).isEmpty());
    connection.close();
    connection = new DefaultRemoteEntityConnection(DOMAIN, Database.instance(), client, 1238);
    assertFalse(connection.select(condition).isEmpty());
    connection.close();
  }

  @Test
  void test() throws Exception {
    Registry registry = null;
    DefaultRemoteEntityConnection adapter = null;
    final String serviceName = "DefaultRemoteEntityConnectionTest";
    try {
      RemoteClient client = RemoteClient.remoteClient(ConnectionRequest.builder()
              .user(UNIT_TEST_USER).clientTypeId("DefaultRemoteEntityConnectionTestClient").build());
      adapter = new DefaultRemoteEntityConnection(DOMAIN, Database.instance(), client, 1238);

      registry = Server.Locator.registry();

      registry.rebind(serviceName, adapter);
      Collection<String> boundNames = asList(registry.list());
      assertTrue(boundNames.contains(serviceName));

      DefaultRemoteEntityConnection finalAdapter = adapter;
      EntityConnection proxy = (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(),
              new Class[] {EntityConnection.class}, (proxy1, method, args) -> {
                Method remoteMethod = RemoteEntityConnection.class.getMethod(method.getName(), method.getParameterTypes());
                try {
                  return remoteMethod.invoke(finalAdapter, args);
                }
                catch (InvocationTargetException e) {
                  throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
                }
              });

      Condition condition = all(Employee.TYPE);
      proxy.beginTransaction();
      proxy.select(condition);
      proxy.delete(condition);
      proxy.select(condition);
      proxy.rollbackTransaction();
      proxy.select(condition);
    }
    finally {
      if (registry != null) {
        try {
          registry.unbind(serviceName);
        }
        catch (Exception ignored) {/*ignored*/}
      }
      try {
        if (adapter != null) {
          adapter.close();
        }
      }
      catch (Exception ignored) {/*ignored*/}
    }
  }
}
