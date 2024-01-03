/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.test.EntityEditPanelTestUnit;

import org.junit.jupiter.api.Test;

public class DepartmentEditPanelTest extends EntityEditPanelTestUnit {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public DepartmentEditPanelTest() {
    super(new SwingEntityEditModel(Department.TYPE, EntityConnectionProvider.builder()
                    .domainType(Employees.DOMAIN)
                    .clientTypeId(DepartmentEditPanel.class.getName())
                    .user(UNIT_TEST_USER)
                    .build()),
            DepartmentEditPanel.class);
  }

  @Test
  void initialize() throws Exception {
    testInitialize();
  }
}
