/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LocalEntityConnectionProviderTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() {
    LocalEntityConnectionProvider provider = LocalEntityConnectionProvider.builder()
            .user(UNIT_TEST_USER)
            .domain(new TestDomain())
            .build();

    assertNotNull(provider.database());

    EntityConnection firstConnection = provider.connection();
    assertNotNull(firstConnection);
    assertTrue(firstConnection.connected());
    provider.close();

    EntityConnection secondConnection = provider.connection();
    assertNotNull(secondConnection);
    assertNotSame(firstConnection, secondConnection);
    assertTrue(secondConnection.connected());
    provider.close();
  }

  @Test
  void entityConnectionProviderBuilder() {
    String previousValue = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
    EntityConnectionProvider connectionProvider = EntityConnectionProvider.builder()
            .domainType(TestDomain.DOMAIN)
            .user(User.parse("scott:tiger"))
            .build();
    assertTrue(connectionProvider instanceof LocalEntityConnectionProvider);
    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_LOCAL, connectionProvider.connectionType());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(previousValue);
  }
}
