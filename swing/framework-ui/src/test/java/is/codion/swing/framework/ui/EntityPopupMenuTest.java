/*
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

public final class EntityPopupMenuTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() throws DatabaseException {
    try (EntityConnectionProvider connectionProvider = LocalEntityConnectionProvider.builder()
            .domain(new TestDomain())
            .user(UNIT_TEST_USER)
            .build()) {
      Entity blake = connectionProvider.connection().selectSingle(Employee.NAME.equalTo("BLAKE"));
      blake.put(Employee.NAME, "a really long name aaaaaaaaaaaaaaaaaaaaaaaaaa");
      blake.put(Employee.SALARY, 100d);

      new EntityPopupMenu(blake, connectionProvider.connection());
    }
  }
}
