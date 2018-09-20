package org.jminor.framework.db;

import org.jminor.common.StateObserver;
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
    final TestProvider provider = new TestProvider(USER);
    assertEquals("description", provider.getDescription());
    assertEquals("localhost", provider.getServerHostName());
    assertEquals(EntityConnection.Type.LOCAL, provider.getConnectionType());
    assertEquals(provider.getDomain(), DOMAIN);
    assertEquals(USER, provider.getUser());
    assertNotNull(provider.getConditions());

    final StateObserver connectedObserver = provider.getConnectedObserver();

    final EntityConnection connection1 = provider.getConnection();
    assertTrue(connectedObserver.isActive());
    provider.disconnect();
    assertFalse(connectedObserver.isActive());

    final EntityConnection connection2 = provider.getConnection();
    assertTrue(connectedObserver.isActive());
    assertNotEquals(connection1, connection2);

    connection2.disconnect();
    final EntityConnection connection3 = provider.getConnection();
    assertNotEquals(connection2, connection3);

    provider.setUser(USER);
    assertFalse(provider.isConnected());

    final EntityConnection connection4 = provider.getConnection();
    assertTrue(connectedObserver.isActive());
    assertNotEquals(connection3, connection4);

    provider.setUser(null);
    assertThrows(IllegalStateException.class, provider::getConnection);
  }

  private static final class TestProvider extends AbstractEntityConnectionProvider {

    public TestProvider(final User user) {
      super(user, true);
    }

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
    public EntityConnection.Type getConnectionType() {
      return EntityConnection.Type.LOCAL;
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
