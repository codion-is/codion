/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPoolException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.server.provider.RemoteEntityConnectionProvider;

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

public class RemoteEntityConnectionImplTest {

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
    final ClientInfo info = new ClientInfo(UUID.randomUUID(), "RemoteEntityConnectionImplTestClient", new User("foo", "bar"));
    new RemoteEntityConnectionImpl(Databases.createInstance(), info, 1234, true, false);
  }

  @Test(expected = DatabaseException.class)
  public void wrongPassword() throws Exception {
    final ClientInfo info = new ClientInfo(UUID.randomUUID(), "RemoteEntityConnectionImplTestClient", new User(User.UNIT_TEST_USER.getUsername(), "xxxxx"));
    new RemoteEntityConnectionImpl(Databases.createInstance(), info, 1234, true, false);
  }

  @Test
  public void test() throws Exception {
    Registry registry = null;
    RemoteEntityConnectionImpl adapter = null;
    final String serviceName = "RemoteEntityConnectionImplTest";
    try {
      Chinook.init();
      final ClientInfo info = new ClientInfo(UUID.randomUUID(), "RemoteEntityConnectionImplTestClient", User.UNIT_TEST_USER);
      adapter = new RemoteEntityConnectionImpl(Databases.createInstance(), info, 1234, true, false);

      ServerUtil.initializeRegistry(Registry.REGISTRY_PORT);

      registry = LocateRegistry.getRegistry("localhost");
      registry.rebind(serviceName, adapter);
      final Collection<String> boundNames = Arrays.asList(registry.list());
      assertTrue(boundNames.contains(serviceName));

      final RemoteEntityConnectionImpl finalAdapter = adapter;
      final EntityConnection proxy = Util.initializeProxy(EntityConnection.class, new InvocationHandler() {
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
          final Method remoteMethod = RemoteEntityConnection.class.getMethod(method.getName(), method.getParameterTypes());
          try {
            return remoteMethod.invoke(finalAdapter, args);
          }
          catch (InvocationTargetException ie) {
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
        } catch (Exception e) {}
      }
      try {
        if (adapter != null) {
          adapter.disconnect();
        }
      }
      catch (Exception e) {}
    }
  }

  @Test(expected = ConnectionPoolException.NoConnectionAvailable.class)
  public void connectionPool() throws RemoteException, DatabaseException {
    final RemoteEntityConnectionProvider provider = new RemoteEntityConnectionProvider(User.UNIT_TEST_USER, UUID.randomUUID(), "RemoteEntityConnectionImplTest");
    final RemoteEntityConnectionProvider provider2 = new RemoteEntityConnectionProvider(User.UNIT_TEST_USER, UUID.randomUUID(), "RemoteEntityConnectionImplTest");
    try {
      EntityConnectionServerTest.getServerAdmin().setConnectionPoolEnabled(User.UNIT_TEST_USER, false);
      EntityConnectionServerTest.getServerAdmin().setConnectionPoolEnabled(User.UNIT_TEST_USER, true);
      EntityConnectionServerTest.getServerAdmin().setMinimumConnectionPoolSize(User.UNIT_TEST_USER, 1);
      EntityConnectionServerTest.getServerAdmin().setMaximumConnectionPoolSize(User.UNIT_TEST_USER, 1);

      final EntityConnection connection = provider.getConnection();

      connection.beginTransaction();
      connection.selectAll(EmpDept.T_DEPARTMENT);

      final EntityConnection connection2 = provider2.getConnection();

      connection2.selectAll(EmpDept.T_DEPARTMENT);
    }
    finally {
      provider.disconnect();
      provider2.disconnect();
    }
  }
}
