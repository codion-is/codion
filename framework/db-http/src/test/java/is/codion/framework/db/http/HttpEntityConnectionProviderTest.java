/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
    String previousValue = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_HTTP);
    HttpEntityConnection.PORT.set(8089);
    EntityConnectionProvider connectionProvider = EntityConnectionProvider.builder()
            .domainClassName(TestDomain.class.getName())
            .clientTypeId("test")
            .user(User.parse("scott:tiger"))
            .build();
    assertTrue(connectionProvider instanceof HttpEntityConnectionProvider);
    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_HTTP, connectionProvider.connectionType());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(previousValue);
  }
}
