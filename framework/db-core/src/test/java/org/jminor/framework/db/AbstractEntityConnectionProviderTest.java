package org.jminor.framework.db;

import org.jminor.common.User;
import org.jminor.common.Util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractEntityConnectionProviderTest {

  private static final User USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final TestDomain DOMAIN = new TestDomain();

  @Test
  public void connectDisconnect() {
    final EntityConnectionProvider provider = new TestProvider().setUser(USER);
    assertEquals("description", provider.getDescription());
    assertEquals("localhost", provider.getServerHostName());
    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_LOCAL, provider.getConnectionType());
    assertEquals(provider.getDomain(), DOMAIN);
    assertEquals(USER, provider.getUser());
    assertNotNull(provider.getConditions());

    final EntityConnection connection1 = provider.getConnection();
    assertTrue(provider.isConnectionValid());
    provider.disconnect();
    assertFalse(provider.isConnectionValid());

    final EntityConnection connection2 = provider.getConnection();
    assertTrue(provider.isConnectionValid());
    assertNotEquals(connection1, connection2);

    connection2.disconnect();
    assertFalse(provider.isConnectionValid());
    final EntityConnection connection3 = provider.getConnection();
    assertNotEquals(connection2, connection3);

    provider.setUser(USER);
    assertFalse(provider.isConnected());
    assertFalse(provider.isConnectionValid());

    final EntityConnection connection4 = provider.getConnection();
    assertTrue(provider.isConnectionValid());
    assertNotEquals(connection3, connection4);

    provider.setUser(null);
    assertThrows(IllegalStateException.class, provider::getConnection);
  }

  private static final class TestProvider extends AbstractEntityConnectionProvider {

    @Override
    protected EntityConnection connect() {
      return Util.initializeProxy(EntityConnection.class, new InvocationHandler() {
        private boolean connected = true;
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
          switch (method.getName()) {
            case "equals":
              return TestProvider.this == args[0];
            case "getDomain":
              return DOMAIN;
            case "isConnected":
              return connected;
            case "disconnect": connected = false;
              break;
          }

          return null;
        }
      });
    }

    @Override
    protected void disconnect(final EntityConnection connection) {
      connection.disconnect();
    }

    @Override
    public String getConnectionType() {
      return EntityConnectionProvider.CONNECTION_TYPE_LOCAL;
    }

    @Override
    public String getDescription() {
      return "description";
    }

    @Override
    public String getServerHostName() {
      return "localhost";
    }
  }
}
