/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.tutorial;

import org.jminor.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

/**
 * Just making sure the tutorials are runnable
 */
public final class EntitiesTutorialTest {

  @Test
  public void test() throws DatabaseException {
    EntitiesTutorial.main(new String[0]);
  }
}
