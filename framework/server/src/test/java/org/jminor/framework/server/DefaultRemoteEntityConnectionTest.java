/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.DatabaseConnectionProvider;
import org.jminor.common.db.DatabaseConnections;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.remote.Clients;
import org.jminor.common.remote.RemoteClient;
import org.jminor.common.remote.Servers;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.remote.RemoteEntityConnection;
import org.jminor.framework.domain.Domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultRemoteEntityConnectionTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  @Test
  public void wrongUsername() throws Exception {
    final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(new User("foo", "bar".toCharArray()), UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    assertThrows(DatabaseException.class, () -> new DefaultRemoteEntityConnection(DOMAIN, Databases.getInstance(), client, 1234, true));
  }

  @Test
  public void wrongPassword() throws Exception {
    final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(new User(UNIT_TEST_USER.getUsername(), "xxxxx".toCharArray()), UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    assertThrows(DatabaseException.class, () -> new DefaultRemoteEntityConnection(DOMAIN, Databases.getInstance(), client, 1235, true));
  }

  @Test
  public void rollbackOnDisconnect() throws Exception {
    final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    DefaultRemoteEntityConnection connection = new DefaultRemoteEntityConnection(DOMAIN, Databases.getInstance(), client, 1238, true);
    final EntitySelectCondition condition = Conditions.entitySelectCondition(TestDomain.T_EMP);
    connection.beginTransaction();
    connection.delete(condition);
    assertTrue(connection.selectMany(condition).isEmpty());
    connection.disconnect();
    connection = new DefaultRemoteEntityConnection(DOMAIN, Databases.getInstance(), client, 1238, true);
    assertTrue(connection.selectMany(condition).size() > 0);
    connection.disconnect();
  }

  @Test
  public void pooledTransaction() throws Exception {
    final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    final Database database = Databases.getInstance();
    final DatabaseConnectionProvider connectionProvider = new DatabaseConnectionProvider() {
      @Override
      public Database getDatabase() {
        return database;
      }
      @Override
      public DatabaseConnection createConnection() throws DatabaseException {
        return DatabaseConnections.createConnection(database, getUser());
      }
      @Override
      public void destroyConnection(final DatabaseConnection connection) {
        connection.disconnect();
      }
      @Override
      public User getUser() {
        return UNIT_TEST_USER;
      }
    };
    final ConnectionPool connectionPool = ConnectionPools.createDefaultConnectionPool(connectionProvider);
    final DefaultRemoteEntityConnection connection = new DefaultRemoteEntityConnection(DOMAIN, connectionPool, client, 1238, true);
    final EntitySelectCondition condition = Conditions.entitySelectCondition(TestDomain.T_EMP);
    connection.beginTransaction();
    connection.selectMany(condition);
    connection.delete(condition);
    connection.selectMany(condition);
    connection.rollbackTransaction();
    connection.selectMany(condition);
  }

  @Test
  public void test() throws Exception {
    Registry registry = null;
    DefaultRemoteEntityConnection adapter = null;
    final String serviceName = "DefaultRemoteEntityConnectionTest";
    try {
      final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
      adapter = new DefaultRemoteEntityConnection(DOMAIN, Databases.getInstance(), client, 1238, true);

      Servers.initializeRegistry(Registry.REGISTRY_PORT);

      registry = LocateRegistry.getRegistry("localhost");
      registry.rebind(serviceName, adapter);
      final Collection<String> boundNames = asList(registry.list());
      assertTrue(boundNames.contains(serviceName));

      final DefaultRemoteEntityConnection finalAdapter = adapter;
      final EntityConnection proxy = Util.initializeProxy(EntityConnection.class, (proxy1, method, args) -> {
        final Method remoteMethod = RemoteEntityConnection.class.getMethod(method.getName(), method.getParameterTypes());
        try {
          return remoteMethod.invoke(finalAdapter, args);
        }
        catch (final InvocationTargetException e) {
          throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        }
      });

      proxy.selectMany(Conditions.entitySelectCondition(TestDomain.T_EMP));
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
