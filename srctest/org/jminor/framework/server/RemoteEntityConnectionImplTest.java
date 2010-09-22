/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Databases;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.demos.chinook.domain.Chinook;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public class RemoteEntityConnectionImplTest {

  @Test
  public void test() throws Exception {
    Registry registry = null;
    RemoteEntityConnectionImpl adapter = null;
    final String serviceName = "RemoteEntityConnectionImplTest";
    try {
      Chinook.init();
      final ClientInfo info = new ClientInfo(UUID.randomUUID(), "RemoteEntityConnectionImplTestClient", User.UNIT_TEST_USER);
      adapter = new RemoteEntityConnectionImpl(Databases.createInstance(), info, 2222, true, false);

      Util.initializeRegistry();

      registry = LocateRegistry.getRegistry("localhost");
      registry.rebind(serviceName, adapter);
      final Collection<String> boundNames = Arrays.asList(registry.list());
      assertTrue(boundNames.contains(serviceName));

      final RemoteEntityConnectionImpl finalAdapter = adapter;
      final EntityConnection proxy = Util.initializeProxy(EntityConnection.class, new InvocationHandler() {
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
}
