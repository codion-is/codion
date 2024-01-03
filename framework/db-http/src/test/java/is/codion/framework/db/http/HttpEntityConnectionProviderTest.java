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
package is.codion.framework.db.http;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpEntityConnectionProviderTest {

  @Test
  void entityConnectionProviderBuilder() {
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_HTTP);
    try {
      EntityConnectionProvider connectionProvider = EntityConnectionProvider.builder()
              .domainType(TestDomain.DOMAIN)
              .clientTypeId("test")
              .user(User.parse("scott:tiger"))
              .build();
      assertTrue(connectionProvider instanceof HttpEntityConnectionProvider);
      assertEquals(EntityConnectionProvider.CONNECTION_TYPE_HTTP, connectionProvider.connectionType());
    }
    finally {
      EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(null);
    }
  }
}
