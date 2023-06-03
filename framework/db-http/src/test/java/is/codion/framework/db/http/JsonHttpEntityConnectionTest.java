/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;

import java.util.UUID;

public final class JsonHttpEntityConnectionTest extends AbstractHttpEntityConnectionTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public JsonHttpEntityConnectionTest() {
    super(new JsonHttpEntityConnection(TestDomain.DOMAIN.name(),
            HttpEntityConnectionProvider.HTTP_CLIENT_HOSTNAME.get(),
            UNIT_TEST_USER, "HttpJsonEntityConnectionTest", UUID.randomUUID(),
            HttpEntityConnectionProvider.HTTP_CLIENT_PORT.get(),
            HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.get()));
  }
}
