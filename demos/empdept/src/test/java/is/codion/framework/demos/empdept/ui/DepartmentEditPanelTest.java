/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProviders;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.test.EntityEditPanelTestUnit;

import org.junit.jupiter.api.Test;

public class DepartmentEditPanelTest extends EntityEditPanelTestUnit {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  public DepartmentEditPanelTest() {
    super(new SwingEntityEditModel(EmpDept.Department.TYPE,
                    EntityConnectionProviders.connectionProvider().setDomainClassName(EmpDept.class.getName())
                            .setClientTypeId(DepartmentEditPanelTest.class.getName()).setUser(UNIT_TEST_USER)),
            DepartmentEditPanel.class);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }
}
