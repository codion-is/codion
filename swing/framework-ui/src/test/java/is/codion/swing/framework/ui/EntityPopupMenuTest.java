/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

public final class EntityPopupMenuTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void test() throws DatabaseException {
    Entity blake = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.EMP_NAME, "BLAKE");

    new EntityPopupMenu(blake, CONNECTION_PROVIDER.getConnection());
  }
}
