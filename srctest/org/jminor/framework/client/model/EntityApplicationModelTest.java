/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.User;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.*;
import org.junit.Test;

public class EntityApplicationModelTest {

  @Test
  public void test() {
    final EntityApplicationModel model = new EntityApplicationModel(EntityDbConnectionTest.DB_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        new EmpDept();
      }
    };
    final EntityModel deptModel = model.getMainApplicationModel(EmpDept.T_DEPARTMENT);
    deptModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().setQueryCriteriaRequired(false);
    assertEquals(1, model.getMainApplicationModels().size());
    assertNotNull(deptModel);
    assertEquals(User.UNIT_TEST_USER, model.getUser());
    model.refresh();
    assertTrue(deptModel.getTableModel().getRowCount() > 0);
    assertTrue(deptModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().getRowCount() > 0);
    model.getDbProvider().disconnect();
  }
}
