package org.jminor.framework.demos.empdept.model;

import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.framework.model.Entity;
import org.jminor.framework.testing.EntityTestUnit;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

public class TestEmpDeptModel extends EntityTestUnit {

  public void testDepartment() throws Exception {
    testEntity(EmpDept.T_DEPARTMENT);
  }

  public void testEmployee() throws Exception {
    testEntity(EmpDept.T_EMPLOYEE);
  }

  protected User getTestUser() throws UserCancelException {
    return new User("scott", "tiger");
  }

  protected void loadDomainModel() {
    new EmpDept();
  }

  protected HashMap<String, Entity> initializeReferenceEntities(final Collection<String> entityIDs) throws Exception {
    final HashMap<String, Entity> ret = new HashMap<String, Entity>();
    if (entityIDs.contains(EmpDept.T_DEPARTMENT))
      ret.put(EmpDept.T_DEPARTMENT, initialize(getDepartment()));
    if (entityIDs.contains(EmpDept.T_EMPLOYEE))
      ret.put(EmpDept.T_EMPLOYEE, initialize(getEmployee(ret.get(EmpDept.T_DEPARTMENT))));

    return ret;
  }

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
      final Entity department = getReferencedEntities().get(EmpDept.T_DEPARTMENT);
      final Entity manager = getReferencedEntities().get(EmpDept.T_EMPLOYEE);
      ret.setValue(EmpDept.EMPLOYEE_DEPARTMENT_REF, department);
      ret.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
      ret.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
      ret.setValue(EmpDept.EMPLOYEE_JOB, "Slacker");
      ret.setValue(EmpDept.EMPLOYEE_MGR_REF, manager);
      ret.setValue(EmpDept.EMPLOYEE_NAME, "Darri");
      ret.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

      return ret;
    }

    return null;
  }

  protected void modifyEntity(final Entity testEntity) {
    if (testEntity.getEntityID().equals(EmpDept.T_DEPARTMENT)) {
      testEntity.setValue(EmpDept.DEPARTMENT_LOCATION, "N/A");
      testEntity.setValue(EmpDept.DEPARTMENT_NAME, "Huh");
    }
    else if (testEntity.getEntityID().equals(EmpDept.T_EMPLOYEE)) {
      testEntity.setValue(EmpDept.EMPLOYEE_NAME, "N/A");
      testEntity.setValue(EmpDept.EMPLOYEE_SALARY, 10000d);
    }
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
    ret.setValue(EmpDept.DEPARTMENT_ID, 98);
    ret.setValue(EmpDept.DEPARTMENT_LOCATION, "Abyss");
    ret.setValue(EmpDept.DEPARTMENT_NAME, "Marketing");

    return ret;
  }
}
