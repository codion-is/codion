/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.domain.Entities;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 14:30:18
 */
public class EntityConnectionProvidersTest {

  private static final Entities ENTITIES = new TestDomain();

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  @Test
  public void test() {
    assertNotNull(EntityConnectionProviders.connectionProvider(ENTITIES, UNIT_TEST_USER, "test"));
  }
}
