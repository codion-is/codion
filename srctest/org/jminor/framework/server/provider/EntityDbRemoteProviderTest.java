/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.provider;

import org.jminor.common.model.User;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.server.EntityDbRemoteServerTest;

import static junit.framework.Assert.assertFalse;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * User: Bjorn Darri
 * Date: 1.4.2010
 * Time: 22:44:24
 */
public class EntityDbRemoteProviderTest {

  @BeforeClass
  public static void setUp() throws Exception {
    EntityDbRemoteServerTest.setUp();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    EntityDbRemoteServerTest.tearDown();
  }

  @Test
  public void test() throws Exception {
    final EntityDbRemoteProvider provider = new EntityDbRemoteProvider(User.UNIT_TEST_USER, "TestClientID", "TestClient");
    final EntityDb db = provider.getEntityDb();
    assertNotNull(db);
    assertTrue(db.isConnected());
    assertTrue(db.isConnectionValid());
    provider.disconnect();

    final EntityDb db2 = provider.getEntityDb();
    assertNotNull(db2);
    assertFalse(db == db2);
    assertTrue(db2.isConnected());
    assertTrue(db2.isConnectionValid());
    provider.disconnect();
  }
}
