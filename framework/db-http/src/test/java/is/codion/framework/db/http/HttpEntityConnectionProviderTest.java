/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.framework.db.EntityConnectionProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpEntityConnectionProviderTest {

  @Test
  void entityConnectionProviders() {
    String previousValue = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_HTTP);
    HttpEntityConnectionProvider.HTTP_CLIENT_PORT.set(8089);
    EntityConnectionProvider connectionProvider = EntityConnectionProvider.connectionProvider()
            .setDomainClassName(TestDomain.class.getName()).setClientTypeId("test");
    assertEquals("HttpEntityConnectionProvider", connectionProvider.getClass().getSimpleName());
    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_HTTP, connectionProvider.getConnectionType());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(previousValue);
  }
}
