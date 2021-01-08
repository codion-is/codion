/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  public EmployeeEditPanelTest() {
    super(new EmployeeEditModel(EntityConnectionProvider.connectionProvider()
                    .setDomainClassName(EmpDept.class.getName())
                    .setClientTypeId(EmployeeEditPanelTest.class.getName()).setUser(UNIT_TEST_USER)),
            EmployeeEditPanel.class);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }
}
