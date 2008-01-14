/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.model;

import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.framework.model.AbstractEntityTestFixture;
import org.jminor.framework.model.Entity;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

/**
 * User: Björn Darri
 * Date: 13.1.2008
 * Time: 23:06:05
 */
public class EmpDeptTestFixture extends AbstractEntityTestFixture {

  public EmpDeptTestFixture() {
    new EmpDept();
  }

  public HashMap<String, Entity> initializeReferenceEntities(final Collection<String> entityIDs) throws Exception {
    final HashMap<String, Entity> ret = new HashMap<String, Entity>();
    if (entityIDs.contains(EmpDept.T_DEPARTMENT))
      ret.put(EmpDept.T_DEPARTMENT, initialize(getDepartment()));
    if (entityIDs.contains(EmpDept.T_EMPLOYEE))
      ret.put(EmpDept.T_EMPLOYEE, initialize(getEmployee(ret.get(EmpDept.T_DEPARTMENT))));

    return ret;
  }

  private Entity getEmployee(final Entity department) throws Exception {
    final Entity ret = new Entity(EmpDept.T_EMPLOYEE);
    ret.setValue(EmpDept.EMPLOYEE_DEPARTMENT_REF, department);
    ret.setValue(EmpDept.EMPLOYEE_DEPARTMENT, department.getValue(EmpDept.DEPARTMENT_ID));
    ret.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
    ret.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
    ret.setValue(EmpDept.EMPLOYEE_JOB, "SrSlacker");
    ret.setValue(EmpDept.EMPLOYEE_NAME, "Björn");
    ret.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

    return ret;
  }

  private Entity getDepartment() throws Exception {
    final Entity ret = new Entity(EmpDept.T_DEPARTMENT);
    ret.setValue(EmpDept.DEPARTMENT_ID, 1000);
    ret.setValue(EmpDept.DEPARTMENT_LOCATION, "Abyss");
    ret.setValue(EmpDept.DEPARTMENT_NAME, "Marketing");

    return ret;
  }

  public User getTestUser() throws UserCancelException {
    return new User("scott", "tiger");
  }
}
