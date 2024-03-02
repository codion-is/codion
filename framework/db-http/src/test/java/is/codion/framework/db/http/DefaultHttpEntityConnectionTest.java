/*
 * Copyright (c) 2017 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;

import java.util.UUID;

public final class DefaultHttpEntityConnectionTest extends AbstractHttpEntityConnectionTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public DefaultHttpEntityConnectionTest() {
    super(new DefaultHttpEntityConnection((AbstractHttpEntityConnection.DefaultBuilder) new AbstractHttpEntityConnection.DefaultBuilder()
            .domainType(TestDomain.DOMAIN)
            .user(UNIT_TEST_USER)
            .clientTypeId("HttpEntityConnectionTest")
            .clientId(UUID.randomUUID()), createConnectionManager()));
  }
}
