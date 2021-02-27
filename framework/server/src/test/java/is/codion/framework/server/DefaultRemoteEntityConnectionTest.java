/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.Server;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.domain.Domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultRemoteEntityConnectionTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  public void wrongUsername() throws Exception {
    final RemoteClient client = RemoteClient.remoteClient(ConnectionRequest.builder()
            .user(User.user("foo", "bar".toCharArray())).clientTypeId("DefaultRemoteEntityConnectionTestClient").build());
    assertThrows(DatabaseException.class, () -> new DefaultRemoteEntityConnection(DOMAIN, DatabaseFactory.getDatabase(), client, 1234));
  }

  @Test
  public void wrongPassword() throws Exception {
    final RemoteClient client = RemoteClient.remoteClient(ConnectionRequest.builder()
            .user(User.user(UNIT_TEST_USER.getUsername(), "xxxxx".toCharArray())).clientTypeId("DefaultRemoteEntityConnectionTestClient").build());
    assertThrows(DatabaseException.class, () -> new DefaultRemoteEntityConnection(DOMAIN, DatabaseFactory.getDatabase(), client, 1235));
  }

  @Test
  public void rollbackOnClose() throws Exception {
    final RemoteClient client = RemoteClient.remoteClient(ConnectionRequest.builder()
            .user(UNIT_TEST_USER).clientTypeId("DefaultRemoteEntityConnectionTestClient").build());
    DefaultRemoteEntityConnection connection = new DefaultRemoteEntityConnection(DOMAIN, DatabaseFactory.getDatabase(), client, 1238);
    final Condition condition = Conditions.condition(TestDomain.T_EMP);
    connection.beginTransaction();
    connection.delete(condition);
    assertTrue(connection.select(condition).isEmpty());
    connection.close();
    connection = new DefaultRemoteEntityConnection(DOMAIN, DatabaseFactory.getDatabase(), client, 1238);
    assertTrue(connection.select(condition).size() > 0);
    connection.close();
  }

  @Test
  public void test() throws Exception {
    Registry registry = null;
    DefaultRemoteEntityConnection adapter = null;
    final String serviceName = "DefaultRemoteEntityConnectionTest";
    try {
      final RemoteClient client = RemoteClient.remoteClient(ConnectionRequest.builder()
              .user(UNIT_TEST_USER).clientTypeId("DefaultRemoteEntityConnectionTestClient").build());
      adapter = new DefaultRemoteEntityConnection(DOMAIN, DatabaseFactory.getDatabase(), client, 1238);

      Server.Locator.locator().initializeRegistry(Registry.REGISTRY_PORT);

      registry = LocateRegistry.getRegistry("localhost");
      registry.rebind(serviceName, adapter);
      final Collection<String> boundNames = asList(registry.list());
      assertTrue(boundNames.contains(serviceName));

      final DefaultRemoteEntityConnection finalAdapter = adapter;
      final EntityConnection proxy = (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(),
              new Class[] {EntityConnection.class}, (proxy1, method, args) -> {
                final Method remoteMethod = RemoteEntityConnection.class.getMethod(method.getName(), method.getParameterTypes());
                try {
                  return remoteMethod.invoke(finalAdapter, args);
                }
                catch (final InvocationTargetException e) {
                  throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
                }
              });

      final Condition condition = Conditions.condition(TestDomain.T_EMP);
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
        catch (final Exception ignored) {/*ignored*/}
      }
      try {
        if (adapter != null) {
          adapter.close();
        }
      }
      catch (final Exception ignored) {/*ignored*/}
    }
  }
}
