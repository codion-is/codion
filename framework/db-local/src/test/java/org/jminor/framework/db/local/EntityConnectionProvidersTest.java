/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.framework.db.EntityConnectionProviders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 14:30:18
 */
public class EntityConnectionProvidersTest {

  @Test
  public void test() {
    assertNotNull(EntityConnectionProviders.connectionProvider(TestDomain.class.getName(), "test"));
  }
}
