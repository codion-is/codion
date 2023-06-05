/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
          HttpEntityConnection.SECURE.get(),
          UNIT_TEST_USER, "HttpEntityConnectionJdkTest", UUID.randomUUID()));
  }
}
