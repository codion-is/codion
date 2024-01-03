/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.tutorial;

import is.codion.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

/**
 * Just making sure the tutorials are runnable
 */
public final class EntitiesTutorialTest {

  @Test
  void test() throws DatabaseException {
    EntitiesTutorial.main(new String[0]);
  }
}
