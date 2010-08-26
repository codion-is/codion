/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.User;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.*;
import org.junit.Test;

public final class DefaultEntityApplicationModelTest {

  @Test
  public void test() {
    final DefaultEntityApplicationModel model = new DefaultEntityApplicationModel(EntityDbConnectionTest.DB_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    model.addMainApplicationModels(new EmpModel(model.getDbProvider()));
    final EntityModel deptModel = model.getMainApplicationModel(EmpDept.T_DEPARTMENT);
    final EntityModel empModel = model.getMainApplicationModel(EmpModel.class);
    assertNotNull(empModel);
    deptModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().setQueryCriteriaRequired(false);
    assertEquals(2, model.getMainApplicationModels().size());
    assertNotNull(deptModel);
    assertEquals(User.UNIT_TEST_USER, model.getUser());
    model.refresh();
    assertTrue(deptModel.getTableModel().getRowCount() > 0);
    assertTrue(deptModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().getRowCount() > 0);
    model.logout();
    assertFalse(model.getDbProvider().isConnected());
    model.login(User.UNIT_TEST_USER);
  }

  private static class EmpModel extends DefaultEntityModel {
    private EmpModel(final EntityDbProvider dbProvider) {
      super(EmpDept.T_EMPLOYEE, dbProvider);
    }
  }
}
