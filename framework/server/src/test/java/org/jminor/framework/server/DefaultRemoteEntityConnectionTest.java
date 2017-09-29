/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.MethodLogger;
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
import org.jminor.common.server.Clients;
import org.jminor.common.server.RemoteClient;
import org.jminor.common.server.Servers;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.remote.RemoteEntityConnection;
import org.jminor.framework.domain.Entities;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class DefaultRemoteEntityConnectionTest {

  private static final Entities ENTITIES = new TestDomain();
  private static final EntityConditions ENTITY_CONDITIONS = new EntityConditions(ENTITIES);

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  @Test(expected = DatabaseException.class)
  public void wrongUsername() throws Exception {
    final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(new User("foo", "bar"), UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    new DefaultRemoteEntityConnection(ENTITIES, Databases.getInstance(), client, 1234, true);
  }

  @Test(expected = DatabaseException.class)
  public void wrongPassword() throws Exception {
    final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(new User(UNIT_TEST_USER.getUsername(), "xxxxx"), UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    new DefaultRemoteEntityConnection(ENTITIES, Databases.getInstance(), client, 1235, true);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void setMethodLogger() throws DatabaseException, RemoteException {
    DefaultRemoteEntityConnection connection = null;
    try {
      final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
      connection = new DefaultRemoteEntityConnection(ENTITIES, Databases.getInstance(), client, 1236, true);
      connection.setMethodLogger(new MethodLogger(10, false));
    }
    finally {
      try {
        if (connection != null) {
          connection.disconnect();
        }
      }
      catch (final Exception ignored) {/*ignored*/}
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getDatabaseConnection() throws DatabaseException, RemoteException {
    DefaultRemoteEntityConnection connection = null;
    try {
      final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
      connection = new DefaultRemoteEntityConnection(ENTITIES, Databases.getInstance(), client, 1237, true);
      connection.getDatabaseConnection();
    }
    finally {
      try {
        if (connection != null) {
          connection.disconnect();
        }
      }
      catch (final Exception ignored) {/*ignored*/}
    }
  }

  @Test
  public void rollbackOnDisconnect() throws Exception {
    final RemoteClient client = Servers.remoteClient(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    DefaultRemoteEntityConnection connection = new DefaultRemoteEntityConnection(ENTITIES, Databases.getInstance(), client, 1238, true);
    final EntitySelectCondition condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP);
    connection.beginTransaction();
    connection.delete(condition);
    assertTrue(connection.selectMany(condition).isEmpty());
    connection.disconnect();
    connection = new DefaultRemoteEntityConnection(ENTITIES, Databases.getInstance(), client, 1238, true);
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
    final DefaultRemoteEntityConnection connection = new DefaultRemoteEntityConnection(ENTITIES, connectionPool, client, 1238, true);
    final EntitySelectCondition condition = ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP);
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
      adapter = new DefaultRemoteEntityConnection(ENTITIES, Databases.getInstance(), client, 1238, true);

      Servers.initializeRegistry(Registry.REGISTRY_PORT);

      registry = LocateRegistry.getRegistry("localhost");
      registry.rebind(serviceName, adapter);
      final Collection<String> boundNames = Arrays.asList(registry.list());
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

      proxy.selectMany(ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP));
    }
    finally {
      if (registry != null) {
        try {
          registry.unbind(serviceName);
        } catch (final Exception ignored) {/*ignored*/}
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
