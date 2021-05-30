/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LocalEntityConnectionProviderTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() {
    final Database database = DatabaseFactory.getDatabase();
    final LocalEntityConnectionProvider provider = new LocalEntityConnectionProvider(database);
    provider.setUser(UNIT_TEST_USER).setDomainClassName(TestDomain.class.getName());

    assertNotNull(new LocalEntityConnectionProvider().getDatabase());

    final EntityConnection firstConnection = provider.getConnection();
    assertNotNull(firstConnection);
    assertTrue(firstConnection.isConnected());
    provider.close();

    final EntityConnection secondConnection = provider.getConnection();
    assertNotNull(secondConnection);
    assertNotSame(firstConnection, secondConnection);
    assertTrue(secondConnection.isConnected());
    provider.close();
  }

  @Test
  void entityConnectionProviders() {
    final String previousValue = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
    final EntityConnectionProvider connectionProvider = EntityConnectionProvider.connectionProvider()
            .setDomainClassName(TestDomain.class.getName()).setClientTypeId("test");
    assertEquals("LocalEntityConnectionProvider", connectionProvider.getClass().getSimpleName());
    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_LOCAL, connectionProvider.getConnectionType());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(previousValue);
  }
}
