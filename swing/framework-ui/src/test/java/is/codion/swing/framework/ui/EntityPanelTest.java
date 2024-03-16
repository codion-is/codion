/*
 * Copyright (c) 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EntityPanelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void addDetailPanel() {
    SwingEntityModel deptModel = new SwingEntityModel(Department.TYPE, CONNECTION_PROVIDER);
    SwingEntityModel empModel = new SwingEntityModel(Employee.TYPE, CONNECTION_PROVIDER);
    deptModel.addDetailModel(empModel);

    EntityPanel deptPanel = new EntityPanel(deptModel);
    EntityPanel empPanel = new EntityPanel(empModel);

    deptPanel.addDetailPanel(empPanel);
    assertThrows(IllegalArgumentException.class, () -> deptPanel.addDetailPanel(empPanel));

    deptPanel.initialize();
    assertThrows(IllegalStateException.class, () -> deptPanel.addDetailPanels(empPanel));
  }
}
