/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.user.User;

import java.util.UUID;

public final class JsonHttpEntityConnectionTest extends AbstractHttpEntityConnectionTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public JsonHttpEntityConnectionTest() {
    super(new DefaultHttpEntityConnection((AbstractHttpEntityConnection.DefaultBuilder) new AbstractHttpEntityConnection.DefaultBuilder()
            .domainType(TestDomain.DOMAIN)
            .user(UNIT_TEST_USER)
            .json(true)
            .clientTypeId("HttpJsonEntityConnectionTest")
            .clientId(UUID.randomUUID()), createConnectionManager()));
  }
}
