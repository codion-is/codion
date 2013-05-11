package org.jminor.framework.demos.empdept.testing;

import org.jminor.common.model.User;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanelTestUnit;
import org.jminor.framework.demos.empdept.beans.EmployeeEditModel;
import org.jminor.framework.demos.empdept.beans.ui.EmployeeEditPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

public class EmployeeEditPanelTest extends EntityEditPanelTestUnit {

  public EmployeeEditPanelTest() {
    super(EmployeeEditPanel.class, EmpDept.T_EMPLOYEE, User.UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }

  @Override
  protected EntityEditModel createEditModel() {
    return new EmployeeEditModel(getConnectionProvider());
  }
}
