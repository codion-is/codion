/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnectionProviders;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 14:30:18
 */
public class EntityConnectionProvidersTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  @Test
  public void test() {
    assertNotNull(EntityConnectionProviders.connectionProvider(TestDomain.class.getName(), UNIT_TEST_USER, "test"));
  }
}
