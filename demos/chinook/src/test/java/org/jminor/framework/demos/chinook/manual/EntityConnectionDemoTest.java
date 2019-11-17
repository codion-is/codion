/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.manual;

import org.jminor.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

/**
 * Just making sure the tutorials are runnable
 */
public final class EntityConnectionDemoTest {

  @Test
  public void test() throws DatabaseException {
    EntityConnectionDemo.main(new String[0]);
  }
}
