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
 * Copyright (c) 2017 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;

import java.util.UUID;

public final class DefaultHttpEntityConnectionTest extends AbstractHttpEntityConnectionTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public DefaultHttpEntityConnectionTest() {
    super(HttpEntityConnection.builder()
            .json(false)
            .domainType(TestDomain.DOMAIN)
            .user(UNIT_TEST_USER)
            .clientTypeId("DefaultHttpEntityConnectionTest")
            .clientId(UUID.randomUUID())
            .build());
  }
}
