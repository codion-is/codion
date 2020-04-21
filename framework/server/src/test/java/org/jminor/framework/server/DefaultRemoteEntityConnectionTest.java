/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.database.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.remote.client.Clients;
import org.jminor.common.remote.server.RemoteClient;
import org.jminor.common.remote.server.Servers;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.remote.RemoteEntityConnection;
import org.jminor.framework.domain.Domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultRemoteEntityConnectionTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  @Test
  public void wrongUsername() throws Exception {
    final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(Users.user("foo", "bar".toCharArray()), UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    assertThrows(DatabaseException.class, () -> new DefaultRemoteEntityConnection(DOMAIN, Databases.getInstance(), client, 1234));
  }

  @Test
  public void wrongPassword() throws Exception {
    final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(Users.user(UNIT_TEST_USER.getUsername(), "xxxxx".toCharArray()), UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    assertThrows(DatabaseException.class, () -> new DefaultRemoteEntityConnection(DOMAIN, Databases.getInstance(), client, 1235));
  }

  @Test
  public void rollbackOnDisconnect() throws Exception {
    final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    DefaultRemoteEntityConnection connection = new DefaultRemoteEntityConnection(DOMAIN, Databases.getInstance(), client, 1238);
    final EntitySelectCondition condition = Conditions.selectCondition(TestDomain.T_EMP);
    connection.beginTransaction();
    connection.delete(condition);
    assertTrue(connection.select(condition).isEmpty());
    connection.disconnect();
    connection = new DefaultRemoteEntityConnection(DOMAIN, Databases.getInstance(), client, 1238);
    assertTrue(connection.select(condition).size() > 0);
    connection.disconnect();
  }

  @Test
  public void test() throws Exception {
    Registry registry = null;
    DefaultRemoteEntityConnection adapter = null;
    final String serviceName = "DefaultRemoteEntityConnectionTest";
    try {
      final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
      adapter = new DefaultRemoteEntityConnection(DOMAIN, Databases.getInstance(), client, 1238);

      Servers.initializeRegistry(Registry.REGISTRY_PORT);

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

      final EntitySelectCondition condition = Conditions.selectCondition(TestDomain.T_EMP);
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
          adapter.disconnect();
        }
      }
      catch (final Exception ignored) {/*ignored*/}
    }
  }
}
