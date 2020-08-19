/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.Users;

import org.junit.jupiter.api.Test;

public final class DatabaseExplorerModelTest {

  @Test
  public void test() throws DatabaseException {
    new DatabaseExplorerModel(Databases.getInstance(), Users.parseUser("scott:tiger"));
  }
}
