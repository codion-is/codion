/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
