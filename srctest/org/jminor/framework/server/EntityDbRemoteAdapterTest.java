/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.tools.testing.EntityTestUnit;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public class EntityDbRemoteAdapterTest {

  @Test
  public void test() throws Exception {
    Chinook.init();
    final ClientInfo info = new ClientInfo(UUID.randomUUID(), "EntityDbRemoteAdapterTestClient", User.UNIT_TEST_USER);
    final EntityDbRemoteAdapter adapter = new EntityDbRemoteAdapter(DatabaseProvider.createInstance(),
            info, 2222, true, false);

    Util.initializeRegistry();

    final Registry registry = LocateRegistry.getRegistry("localhost");
    final String serviceName = "EntityDbRemoteAdapterTest";
    registry.rebind(serviceName, adapter);
    final Collection<String> boundNames = Arrays.asList(registry.list());
    assertTrue(boundNames.contains(serviceName));

    final EntityDb proxy = Util.initializeProxy(EntityDb.class, new InvocationHandler() {
      public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        final Method remoteMethod = EntityDbRemote.class.getMethod(method.getName(), method.getParameterTypes());
        try {
          return remoteMethod.invoke(adapter, args);
        }
        catch (InvocationTargetException ie) {
          throw (Exception) ie.getTargetException();
        }
      }
    });

    final EntityTestUnit testUnit = new EntityTestUnit() {
      @Override
      protected EntityDbProvider initializeDbConnectionProvider() throws CancelException {
        return new EntityDbProvider() {
          public EntityDb getEntityDb() {
            return proxy;
          }

          public String getDescription() {return null;}
          public boolean isConnected() {return false;}
          public void disconnect() {}
          public void setUser(User user) {}
          public User getUser() {return null;}
        };
      }

      protected void loadDomainModel() {
        Chinook.init();
      }
    };

    testUnit.setUp();
    testUnit.testEntity(Chinook.T_INVOICELINE);

    registry.unbind(serviceName);
    adapter.disconnect();
  }
}
