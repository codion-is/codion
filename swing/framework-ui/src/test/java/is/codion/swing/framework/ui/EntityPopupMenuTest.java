/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

public final class EntityPopupMenuTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  void test() throws DatabaseException {
    Entity blake = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.EMP_NAME, "BLAKE");
    Entity trevor = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.EMP_NAME, "TREVOR");
    blake.put(TestDomain.EMP_ID, -1);
    blake.put(TestDomain.EMP_NAME, "NEWSTEAD");
    blake.put(TestDomain.EMP_MGR_FK, trevor);

    new EntityPopupMenu(blake, CONNECTION_PROVIDER);
  }
}
