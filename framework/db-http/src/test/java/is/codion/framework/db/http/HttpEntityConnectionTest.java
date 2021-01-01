/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;
import is.codion.common.user.Users;

import java.util.UUID;

public final class HttpEntityConnectionTest extends AbstractHttpEntityConnectionTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  public HttpEntityConnectionTest() {
    super(new HttpEntityConnection(TestDomain.DOMAIN.getName(),
          HttpEntityConnectionProvider.HTTP_CLIENT_HOST_NAME.get(),
          HttpEntityConnectionProvider.HTTP_CLIENT_PORT.get(),
          HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.get(),
          UNIT_TEST_USER, "HttpEntityConnectionTest", UUID.randomUUID(),
          createConnectionManager()));
  }
}
