/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.test.EntityEditPanelTestUnit;

import org.junit.jupiter.api.Test;

public class DepartmentEditPanelTest extends EntityEditPanelTestUnit {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public DepartmentEditPanelTest() {
    super(new SwingEntityEditModel(EmpDept.Department.TYPE,
                    EntityConnectionProvider.connectionProvider().setDomainClassName(EmpDept.class.getName())
                            .setClientTypeId(DepartmentEditPanelTest.class.getName()).setUser(UNIT_TEST_USER)),
            DepartmentEditPanel.class);
  }

  @Test
  void initializePanel() throws Exception {
    testInitializePanel();
  }
}
