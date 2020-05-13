/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db.rmi;

import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.EntityConnectionProviders;
import dev.codion.framework.domain.Domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * User: Bjorn Darri
 * Date: 1.4.2010
 * Time: 22:44:24
 */
public class RemoteEntityConnectionProviderTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  public void entityConnectionProviders() {
    final String previousValue = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
    final EntityConnectionProvider connectionProvider = EntityConnectionProviders.connectionProvider()
            .setDomainClassName(Domain.class.getName()).setClientTypeId("test").setUser(UNIT_TEST_USER);
    assertEquals("RemoteEntityConnectionProvider", connectionProvider.getClass().getSimpleName());
    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_REMOTE, connectionProvider.getConnectionType());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(previousValue);
  }
}
