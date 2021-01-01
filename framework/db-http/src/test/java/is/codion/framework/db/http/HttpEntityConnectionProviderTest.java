/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.EntityConnectionProviders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpEntityConnectionProviderTest {

  @Test
  public void entityConnectionProviders() {
    final String previousValue = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_HTTP);
    HttpEntityConnectionProvider.HTTP_CLIENT_PORT.set(8089);
    final EntityConnectionProvider connectionProvider = EntityConnectionProviders.connectionProvider()
            .setDomainClassName(TestDomain.class.getName()).setClientTypeId("test");
    assertEquals("HttpEntityConnectionProvider", connectionProvider.getClass().getSimpleName());
    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_HTTP, connectionProvider.getConnectionType());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(previousValue);
  }
}
