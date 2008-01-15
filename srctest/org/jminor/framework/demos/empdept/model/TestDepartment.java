/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.model;

import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityTestUnit;

import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 13.1.2008
 * Time: 23:05:05
 */
public class TestDepartment extends EntityTestUnit {

  public TestDepartment() {
    super("TestDepartment", new EmpDeptTestFixture(), EmpDept.T_DEPARTMENT);
  }

  protected void modifyEntity(final Entity testEntity) {
    testEntity.setValue(EmpDept.DEPARTMENT_LOCATION, "N/A");
    testEntity.setValue(EmpDept.DEPARTMENT_NAME, "Huh");
  }

  protected List<Entity> initTestEntities() {
    final Entity ret = new Entity(EmpDept.T_DEPARTMENT);
    ret.setValue(EmpDept.DEPARTMENT_ID, 99);
    ret.setValue(EmpDept.DEPARTMENT_LOCATION, "Limbo");
    ret.setValue(EmpDept.DEPARTMENT_NAME, "Judgment");

    return Arrays.asList(ret);
  }
}
