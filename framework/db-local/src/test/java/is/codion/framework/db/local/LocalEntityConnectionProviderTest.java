/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
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
