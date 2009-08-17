/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.model;

import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.testing.EntityTestUnit;

import java.util.Collection;
import java.util.Date;

public class EmpDeptTest extends EntityTestUnit {

  public void testDepartment() throws Exception {
    testEntity(EmpDept.T_DEPARTMENT);
  }

  public void testEmployee() throws Exception {
    testEntity(EmpDept.T_EMPLOYEE);
  }

  @Override
  protected User getTestUser() throws UserCancelException {
    return new User("scott", "tiger");
  }

  @Override
  protected void loadDomainModel() {
    new EmpDept();
  }

  @Override
  protected void initializeReferenceEntities(final Collection<String> entityIDs) throws Exception {
    if (entityIDs.contains(EmpDept.T_DEPARTMENT)) {
      final Entity dept = new Entity(EmpDept.T_DEPARTMENT);
      dept.setValue(EmpDept.DEPARTMENT_ID, 98);
      dept.setValue(EmpDept.DEPARTMENT_LOCATION, "Abyss");
      dept.setValue(EmpDept.DEPARTMENT_NAME, "Marketing");

      setReferenceEntity(EmpDept.T_DEPARTMENT, dept);
    }
    if (entityIDs.contains(EmpDept.T_EMPLOYEE)) {
      final Entity department = getReferenceEntity(EmpDept.T_DEPARTMENT);
      final Entity emp = new Entity(EmpDept.T_EMPLOYEE);
      emp.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
      emp.setValue(EmpDept.EMPLOYEE_DEPARTMENT, department.getValue(EmpDept.DEPARTMENT_ID));
      emp.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
      emp.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
      emp.setValue(EmpDept.EMPLOYEE_JOB, "SrSlacker");
      emp.setValue(EmpDept.EMPLOYEE_NAME, "Björn");
      emp.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

      setReferenceEntity(EmpDept.T_EMPLOYEE, emp);
    }
  }

  @Override
  protected Entity initializeTestEntity(final String entityID) {
    if (entityID.equals(EmpDept.T_DEPARTMENT)) {
      final Entity ret = new Entity(EmpDept.T_DEPARTMENT);
      ret.setValue(EmpDept.DEPARTMENT_ID, 99);
      ret.setValue(EmpDept.DEPARTMENT_LOCATION, "Limbo");
      ret.setValue(EmpDept.DEPARTMENT_NAME, "Judgment");

      return ret;
    }
    else if (entityID.equals(EmpDept.T_EMPLOYEE)) {
      final Entity ret = new Entity(EmpDept.T_EMPLOYEE);
      final Entity department = getReferenceEntity(EmpDept.T_DEPARTMENT);
      final Entity manager = getReferenceEntity(EmpDept.T_EMPLOYEE);
      ret.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
      ret.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
      ret.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
      ret.setValue(EmpDept.EMPLOYEE_JOB, "Slacker");
      ret.setValue(EmpDept.EMPLOYEE_MGR_FK, manager);
      ret.setValue(EmpDept.EMPLOYEE_NAME, "Darri");
      ret.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

      return ret;
    }

    return null;
  }

  @Override
  protected void modifyEntity(final Entity testEntity) {
    if (testEntity.is(EmpDept.T_DEPARTMENT)) {
      testEntity.setValue(EmpDept.DEPARTMENT_LOCATION, "N/A");
      testEntity.setValue(EmpDept.DEPARTMENT_NAME, "Huh");
    }
    else if (testEntity.is(EmpDept.T_EMPLOYEE)) {
      testEntity.setValue(EmpDept.EMPLOYEE_NAME, "N/A");
      testEntity.setValue(EmpDept.EMPLOYEE_SALARY, 10000d);
    }
  }
}
