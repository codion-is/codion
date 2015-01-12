/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.tools.MethodLogger;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientUtil;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.demos.chinook.domain.Chinook;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
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

  @BeforeClass
  public static void setUp() throws Exception {
    EntityConnectionServerTest.setUp();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    EntityConnectionServerTest.tearDown();
  }

  @Test(expected = DatabaseException.class)
  public void wrongUsername() throws Exception {
    final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectInfo(new User("foo", "bar"), UUID.randomUUID(), "RemoteEntityConnectionImplTestClient"));
    new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1234, true, false);
  }

  @Test(expected = DatabaseException.class)
  public void wrongPassword() throws Exception {
    final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectInfo(new User(User.UNIT_TEST_USER.getUsername(), "xxxxx"), UUID.randomUUID(), "RemoteEntityConnectionImplTestClient"));
    new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1234, true, false);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void setMethodLogger() throws DatabaseException, RemoteException {
    DefaultRemoteEntityConnection connection = null;
    try {
      final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectInfo(User.UNIT_TEST_USER, UUID.randomUUID(), "RemoteEntityConnectionImplTestClient"));
      connection = new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1234, true, false);
      connection.setMethodLogger(new MethodLogger(10));
    }
    finally {
      try {
        if (connection != null) {
          connection.disconnect();
        }
      }
      catch (final Exception ignored) {}
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getDatabaseConnection() throws DatabaseException, RemoteException {
    DefaultRemoteEntityConnection connection = null;
    try {
      final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectInfo(User.UNIT_TEST_USER, UUID.randomUUID(), "RemoteEntityConnectionImplTestClient"));
      connection = new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1234, true, false);
      connection.getDatabaseConnection();
    }
    finally {
      try {
        if (connection != null) {
          connection.disconnect();
        }
      }
      catch (final Exception ignored) {}
    }
  }

  @Test
  public void test() throws Exception {
    Registry registry = null;
    DefaultRemoteEntityConnection adapter = null;
    final String serviceName = "DefaultRemoteEntityConnectionTest";
    try {
      Chinook.init();
      final ClientInfo info = ServerUtil.clientInfo(ClientUtil.connectInfo(User.UNIT_TEST_USER, UUID.randomUUID(), "RemoteEntityConnectionImplTestClient"));
      adapter = new DefaultRemoteEntityConnection(Databases.createInstance(), info, 1234, true, false);

      ServerUtil.initializeRegistry(Registry.REGISTRY_PORT);

      registry = LocateRegistry.getRegistry("localhost");
      registry.rebind(serviceName, adapter);
      final Collection<String> boundNames = Arrays.asList(registry.list());
      assertTrue(boundNames.contains(serviceName));

      final DefaultRemoteEntityConnection finalAdapter = adapter;
      final EntityConnection proxy = Util.initializeProxy(EntityConnection.class, new InvocationHandler() {
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
          final Method remoteMethod = RemoteEntityConnection.class.getMethod(method.getName(), method.getParameterTypes());
          try {
            return remoteMethod.invoke(finalAdapter, args);
          }
          catch (final InvocationTargetException ie) {
            throw (Exception) ie.getTargetException();
          }
        }
      });

      proxy.selectAll(Chinook.T_INVOICELINE);
    }
    finally {
      if (registry != null) {
        try {
          registry.unbind(serviceName);
        } catch (final Exception ignored) {}
      }
      try {
        if (adapter != null) {
          adapter.disconnect();
        }
      }
      catch (final Exception ignored) {}
    }
  }
}
