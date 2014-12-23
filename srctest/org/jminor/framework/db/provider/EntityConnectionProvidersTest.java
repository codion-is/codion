/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.User;
import org.jminor.framework.Configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 14:30:18
 */
public class EntityConnectionProvidersTest {

  @Test
  public void test() throws Exception {
    final String connectionType = Configuration.getStringValue(Configuration.CLIENT_CONNECTION_TYPE);
    try {
      Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, Configuration.CONNECTION_TYPE_LOCAL);
      EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(User.UNIT_TEST_USER, "test");
      assertTrue(connectionProvider instanceof LocalEntityConnectionProvider);

      Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, Configuration.CONNECTION_TYPE_REMOTE);
      connectionProvider = EntityConnectionProviders.createConnectionProvider(User.UNIT_TEST_USER, "test");
      assertEquals("RemoteEntityConnectionProvider", connectionProvider.getClass().getSimpleName());
    }
    finally {
      if (connectionType != null) {
        Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, connectionType);
      }
      else {
        Configuration.clearValue(Configuration.CLIENT_CONNECTION_TYPE);
      }
    }
  }
}
