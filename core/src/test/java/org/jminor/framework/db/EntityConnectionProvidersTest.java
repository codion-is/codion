/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.Database;
import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.model.User;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 14:30:18
 */
public class EntityConnectionProvidersTest {

  public static final EntityConnectionProvider CONNECTION_PROVIDER = createTestConnectionProvider();

  public static EntityConnectionProvider createTestConnectionProvider() {
    final String type = System.getProperty(Database.DATABASE_TYPE);
    final String host = System.getProperty(Database.DATABASE_HOST);
    final String port = System.getProperty(Database.DATABASE_PORT, "1234");
    final String sid = System.getProperty(Database.DATABASE_SID, "sid");
    final String embedded = System.getProperty(Database.DATABASE_EMBEDDED, "false");
    final String embeddedInMemory = System.getProperty(Database.DATABASE_EMBEDDED_IN_MEMORY, "false");
    final String initScript = System.getProperty(H2Database.DATABASE_INIT_SCRIPT);
    try {
      System.setProperty(Database.DATABASE_TYPE, type == null ? Database.H2 : type);
      System.setProperty(Database.DATABASE_HOST, host == null ? "h2db/h2" : host);
      System.setProperty(Database.DATABASE_PORT, port);
      System.setProperty(Database.DATABASE_SID, sid);
      System.setProperty(Database.DATABASE_EMBEDDED, embedded == null ? "true" : embedded);
      System.setProperty(Database.DATABASE_EMBEDDED_IN_MEMORY, embeddedInMemory == null ? "true" : embeddedInMemory);
      System.setProperty(H2Database.DATABASE_INIT_SCRIPT, initScript == null ? "demos/src/main/sql/create_h2_db.sql" : initScript);

      return EntityConnectionProviders.connectionProvider(User.UNIT_TEST_USER, "test");
    }
    finally {
      setSystemProperties(type, host, port, sid, embedded, embeddedInMemory, initScript);
    }
  }

  @Test
  public void testWrapper() {
    final EntityConnection connection = CONNECTION_PROVIDER.getConnection();
    final EntityConnectionProvider connectionWrapper = EntityConnectionProviders.connectionProvider(connection);
    assertTrue(connectionWrapper.isConnected());
    assertFalse(connectionWrapper.getConnectedObserver().isActive());
    assertTrue(connection == connectionWrapper.getConnection());
  }

  @Test
  public void testRemoteLocal() throws Exception {
    final String connectionType = Configuration.getStringValue(Configuration.CLIENT_CONNECTION_TYPE);
    try {
      Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, Configuration.CONNECTION_TYPE_LOCAL);
      EntityConnectionProvider connectionProvider = createTestConnectionProvider();
      assertTrue(connectionProvider instanceof LocalEntityConnectionProvider);

      Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, Configuration.CONNECTION_TYPE_REMOTE);
      connectionProvider = EntityConnectionProviders.connectionProvider(User.UNIT_TEST_USER, "test");
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

  private static void setSystemProperties(final String type, final String host, final String port, final String sid,
                                          final String embedded, final String embeddedInMemory, final String initScript) {
    if (type != null) {
      System.setProperty(Database.DATABASE_TYPE, type);
    }
    if (host != null) {
      System.setProperty(Database.DATABASE_HOST, host);
    }
    if (port != null) {
      System.setProperty(Database.DATABASE_PORT, port);
    }
    if (sid != null) {
      System.setProperty(Database.DATABASE_SID, sid);
    }
    if (embedded != null) {
      System.setProperty(Database.DATABASE_EMBEDDED, embedded);
    }
    if (embeddedInMemory != null) {
      System.setProperty(Database.DATABASE_EMBEDDED_IN_MEMORY, embeddedInMemory);
    }
    if (initScript != null) {
      System.setProperty(H2Database.DATABASE_INIT_SCRIPT, initScript);
    }
  }
}
