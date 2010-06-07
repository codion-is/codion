package org.jminor.framework.client.model;

import org.jminor.common.model.User;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.client.EmpDeptAppModel;

import static org.junit.Assert.*;
import org.junit.Test;

public class EntityApplicationModelTest {

  @Test
  public void test() {
    final EntityApplicationModel model = new EmpDeptAppModel(User.UNIT_TEST_USER);
    assertEquals(1, model.getMainApplicationModels().size());
    final EntityModel deptModel = model.getMainApplicationModel(DepartmentModel.class);
    assertNotNull(deptModel);
    assertEquals(User.UNIT_TEST_USER, model.getUser());
    model.refresh();
    assertTrue(deptModel.getTableModel().getRowCount() > 0);
    assertTrue(deptModel.getDetailModel(EmployeeModel.class).getTableModel().getRowCount() > 0);
    model.getDbProvider().disconnect();
  }
}
