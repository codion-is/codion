/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;

import java.util.UUID;

public final class DefaultHttpEntityConnectionTest extends AbstractHttpEntityConnectionTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public DefaultHttpEntityConnectionTest() {
    super(new DefaultHttpEntityConnection(TestDomain.DOMAIN.name(),
            HttpEntityConnectionProvider.HTTP_CLIENT_HOSTNAME.get(),
            UNIT_TEST_USER, "HttpEntityConnectionTest", UUID.randomUUID(),
            HttpEntityConnectionProvider.HTTP_CLIENT_PORT.get(),
            HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.get()));
  }
}
