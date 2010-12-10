/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.junit.Test;

import static org.junit.Assert.fail;

public class EntitiesTest {

  @Test
  public void test() {
    final String entityID = "entityID";
    Entities.define(entityID);
    try {
      Entities.define(entityID);
      fail("Should not be able to re-define an entity");
    }
    catch (Exception e) {}
  }
}
