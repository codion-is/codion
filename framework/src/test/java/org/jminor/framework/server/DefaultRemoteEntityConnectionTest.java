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
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientUtil;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.RemoteEntityConnection;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;

import org.junit.BeforeClass;
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

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  @BeforeClass
  public static void setUp() throws Exception {
    TestDomain.init();
  }

  @Test(expected = DatabaseException.class)
  public void wrongUsername() throws Exception {
    final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectionInfo(new User("foo", "bar"), UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1234, true, false);
  }

  @Test(expected = DatabaseException.class)
  public void wrongPassword() throws Exception {
    final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectionInfo(new User(UNIT_TEST_USER.getUsername(), "xxxxx"), UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1235, true, false);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void setMethodLogger() throws DatabaseException, RemoteException {
    DefaultRemoteEntityConnection connection = null;
    try {
      final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
      connection = new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1236, true, false);
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
      final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
      connection = new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1237, true, false);
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
    final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    DefaultRemoteEntityConnection connection = new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1238, true, false);
    final EntitySelectCondition condition = EntityConditions.selectCondition(TestDomain.T_EMP);
    connection.beginTransaction();
    connection.delete(condition);
    assertTrue(connection.selectMany(condition).isEmpty());
    connection.disconnect();
    connection = new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1238, true, false);
    assertTrue(connection.selectMany(condition).size() > 0);
    connection.disconnect();
  }

  @Test
  public void pooledTransaction() throws Exception {
    final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
    final Database database = Databases.createInstance();
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
    final DefaultRemoteEntityConnection connection = new DefaultRemoteEntityConnection(connectionPool, connectionProvider.getDatabase(), info, 1238, true, false);
    final EntitySelectCondition condition = EntityConditions.selectCondition(TestDomain.T_EMP);
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
      TestDomain.init();
      final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), "DefaultRemoteEntityConnectionTestClient"));
      adapter = new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1238, true, false);

      ServerUtil.initializeRegistry(Registry.REGISTRY_PORT);

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
        catch (final InvocationTargetException ie) {
          throw (Exception) ie.getTargetException();
        }
      });

      proxy.selectMany(EntityConditions.selectCondition(TestDomain.T_EMP));
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
