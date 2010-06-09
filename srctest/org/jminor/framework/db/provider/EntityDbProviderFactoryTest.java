/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.User;
import org.jminor.framework.Configuration;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 14:30:18
 */
public class EntityDbProviderFactoryTest {

  @Test
  public void test() throws Exception {
    final String connectionType = System.getProperty(Configuration.CLIENT_CONNECTION_TYPE);
    try {
      System.setProperty(Configuration.CLIENT_CONNECTION_TYPE, Configuration.CONNECTION_TYPE_LOCAL);
      EntityDbProvider dbProvider = EntityDbProviderFactory.createEntityDbProvider(User.UNIT_TEST_USER, "test");
      assertTrue(dbProvider instanceof EntityDbLocalProvider);

      System.setProperty(Configuration.CLIENT_CONNECTION_TYPE, Configuration.CONNECTION_TYPE_REMOTE);
      dbProvider = EntityDbProviderFactory.createEntityDbProvider(User.UNIT_TEST_USER, "test");
      assertTrue(dbProvider.getClass().getSimpleName().equals("EntityDbRemoteProvider"));
    }
    finally {
      if (connectionType != null) {
        System.setProperty(Configuration.CLIENT_CONNECTION_TYPE, connectionType);
      }
      else {
        System.clearProperty(Configuration.CLIENT_CONNECTION_TYPE);
      }
    }
  }
}
