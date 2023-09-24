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
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;

import java.util.UUID;

public final class HttpEntityConnectionJdkTest extends AbstractHttpEntityConnectionTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public HttpEntityConnectionJdkTest() {
    super(new HttpEntityConnectionJdk(TestDomain.DOMAIN.name(),
          HttpEntityConnection.HOSTNAME.get(),
          HttpEntityConnection.PORT.get(),
          HttpEntityConnection.SECURE_PORT.get(),
          HttpEntityConnection.SECURE.get(),
          UNIT_TEST_USER, "HttpEntityConnectionJdkTest", UUID.randomUUID()));
  }
}
