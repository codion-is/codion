/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.demos.empdept.model.EmployeeEditModel;
import is.codion.swing.framework.ui.test.EntityEditPanelTestUnit;

import org.junit.jupiter.api.Test;

public class EmployeeEditPanelTest extends EntityEditPanelTestUnit {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public EmployeeEditPanelTest() {
    super(new EmployeeEditModel(EntityConnectionProvider.builder()
                    .domainClassName(EmpDept.class.getName())
                    .clientTypeId(EmployeeEditPanelTest.class.getName())
                    .user(UNIT_TEST_USER)
                    .build()),
            EmployeeEditPanel.class);
  }

  @Test
  void initializePanel() throws Exception {
    testInitializePanel();
  }
}
