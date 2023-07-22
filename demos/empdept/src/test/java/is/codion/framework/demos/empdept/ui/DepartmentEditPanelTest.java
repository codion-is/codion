/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.test.EntityEditPanelTestUnit;

import org.junit.jupiter.api.Test;

public class DepartmentEditPanelTest extends EntityEditPanelTestUnit {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public DepartmentEditPanelTest() {
    super(new SwingEntityEditModel(Department.TYPE, EntityConnectionProvider.builder()
                    .domainType(EmpDept.DOMAIN)
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
