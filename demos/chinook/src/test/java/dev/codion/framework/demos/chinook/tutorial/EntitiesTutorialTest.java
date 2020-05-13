/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.tutorial;

import dev.codion.common.db.exception.DatabaseException;

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
