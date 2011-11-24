package org.jminor.framework.client.model;

import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DefaultEntityModelProviderTest {

  public DefaultEntityModelProviderTest() {
    EmpDept.init();
  }

  @Test
  public void testDetailModelProvider() {
    final EntityModelProvider employeeModelProvider = new DefaultEntityModelProvider(EmpDept.T_EMPLOYEE);
    final EntityModelProvider departmentModelProvider = new DefaultEntityModelProvider(EmpDept.T_DEPARTMENT);
    departmentModelProvider.addDetailModelProvider(employeeModelProvider);

    final EntityModel departmentModel = departmentModelProvider.initializeModel(EntityConnectionImplTest.DB_PROVIDER, false);
    assertTrue(departmentModel.containsDetailModel(EmpDept.T_EMPLOYEE));
  }
}
