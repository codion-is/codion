/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.model;

import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityTestUnit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 13.1.2008
 * Time: 23:05:05
 */
public class TestEmployee extends EntityTestUnit {

  public TestEmployee() {
    super("TestEmployee", new EmpDeptTestFixture(), EmpDept.T_EMPLOYEE);
  }

  protected void modifyEntity(final Entity testEntity) {
    testEntity.setValue(EmpDept.EMPLOYEE_NAME, "N/A");
    testEntity.setValue(EmpDept.EMPLOYEE_SALARY, 10000d);
  }

  protected List<Entity> initializeTestEntities() {
    final Entity ret = new Entity(EmpDept.T_EMPLOYEE);
    final Entity department = getReferencedEntities().get(EmpDept.T_DEPARTMENT);
    final Entity manager = getReferencedEntities().get(EmpDept.T_EMPLOYEE);
    ret.setValue(EmpDept.EMPLOYEE_DEPARTMENT_REF, department);
    ret.setValue(EmpDept.EMPLOYEE_DEPARTMENT, department.getValue(EmpDept.DEPARTMENT_ID));
    ret.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
    ret.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
    ret.setValue(EmpDept.EMPLOYEE_JOB, "Slacker");
    ret.setValue(EmpDept.EMPLOYEE_MGR_REF, manager);
    ret.setValue(EmpDept.EMPLOYEE_MGR, manager.getValue(EmpDept.EMPLOYEE_ID));
    ret.setValue(EmpDept.EMPLOYEE_NAME, "Darri");
    ret.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

    return Arrays.asList(ret);
  }

  protected Collection<String> getReferenceEntityIDs() {
    return Arrays.asList(EmpDept.T_DEPARTMENT, EmpDept.T_EMPLOYEE);
  }
}