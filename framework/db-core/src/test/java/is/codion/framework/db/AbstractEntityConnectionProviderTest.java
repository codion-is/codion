package is.codion.framework.db;

import is.codion.common.user.User;
import is.codion.framework.domain.entity.Entities;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractEntityConnectionProviderTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Entities ENTITIES = new TestDomain().getEntities();

  @Test
  void connectClose() {
    EntityConnectionProvider provider = new TestProvider().setUser(UNIT_TEST_USER);
    assertEquals("description", provider.getDescription());
    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_LOCAL, provider.getConnectionType());
    assertEquals(provider.getEntities(), ENTITIES);
    assertEquals(UNIT_TEST_USER, provider.getUser());

    EntityConnection connection1 = provider.getConnection();
    assertTrue(provider.isConnectionValid());
    provider.close();
    assertFalse(provider.isConnectionValid());

    EntityConnection connection2 = provider.getConnection();
    assertTrue(provider.isConnectionValid());
    assertNotEquals(connection1, connection2);

    connection2.close();
    assertFalse(provider.isConnectionValid());
    EntityConnection connection3 = provider.getConnection();
    assertNotEquals(connection2, connection3);

    provider.setUser(UNIT_TEST_USER);
    assertFalse(provider.isConnected());
    assertFalse(provider.isConnectionValid());

    EntityConnection connection4 = provider.getConnection();
    assertTrue(provider.isConnectionValid());
    assertNotEquals(connection3, connection4);

    provider.setUser(null);
    assertThrows(IllegalStateException.class, provider::getConnection);

    assertThrows(IllegalArgumentException.class, () -> provider.setClientId(null));
    assertThrows(IllegalArgumentException.class, () -> provider.setDomainClassName(null));
  }

  private static final class TestProvider extends AbstractEntityConnectionProvider {

    @Override
    protected EntityConnection connect() {
      return (EntityConnection) Proxy.newProxyInstance(EntityConnection.class.getClassLoader(), new Class[] {EntityConnection.class}, new InvocationHandler() {
        private boolean connected = true;
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
          switch (method.getName()) {
            case "equals":
              return TestProvider.this == args[0];
            case "getEntities":
              return ENTITIES;
            case "isConnected":
              return connected;
            case "close":
              connected = false;
              break;
          }

          return null;
        }
      });
    }

    @Override
    protected void close(final EntityConnection connection) {
      connection.close();
    }

    @Override
    public String getConnectionType() {
      return EntityConnectionProvider.CONNECTION_TYPE_LOCAL;
    }

    @Override
    public String getDescription() {
      return "description";
    }
  }
}
