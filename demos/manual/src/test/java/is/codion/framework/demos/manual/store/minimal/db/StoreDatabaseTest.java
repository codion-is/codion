/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.db;

import is.codion.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

public final class StoreDatabaseTest {

  @Test
  void run() throws DatabaseException, SQLException {
    StoreDatabase.storeEntityConnection();
  }
}
