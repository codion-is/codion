/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.demos.empdept.model;

import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityProxy;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

public class EmpDept {

  public static final String T_DEPARTMENT = "scott.dept";

  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";

  public static final String T_EMPLOYEE = "scott.emp";

  public static final String EMPLOYEE_ID = "empno";
  public static final String EMPLOYEE_NAME = "ename";
  public static final String EMPLOYEE_JOB = "job";
  public static final String EMPLOYEE_MGR = "mgr";
  public static final String EMPLOYEE_MGR_REF = "mgr_ref";
  public static final String EMPLOYEE_HIREDATE = "hiredate";
  public static final String EMPLOYEE_SALARY = "sal";
  public static final String EMPLOYEE_COMMISSION = "comm";
  public static final String EMPLOYEE_DEPARTMENT = "deptno";
  public static final String EMPLOYEE_DEPARTMENT_REF = "dept_ref";

  static {
    Entity.repository.initialize(T_DEPARTMENT, EntityRepository.ID_NONE, null, DEPARTMENT_NAME,
            new Property.PrimaryKeyProperty(DEPARTMENT_ID, Type.INT, "Dept.no "),
            new Property(DEPARTMENT_NAME, Type.STRING, "Name", false, false, 120),
            new Property(DEPARTMENT_LOCATION, Type.STRING, "Location", false, false, 150));

    Entity.repository.initialize(T_EMPLOYEE, EntityRepository.ID_MAX_PLUS_ONE, null,
            EMPLOYEE_DEPARTMENT + ", " + EMPLOYEE_NAME,
            new Property.PrimaryKeyProperty(EMPLOYEE_ID, Type.INT, "Employee No"),
            new Property(EMPLOYEE_NAME, Type.STRING, "Name"),
            new Property.EntityProperty(EMPLOYEE_DEPARTMENT_REF, "Department", T_DEPARTMENT,
                    new Property(EMPLOYEE_DEPARTMENT)),
            new Property(EMPLOYEE_JOB, Type.STRING, "Job"),
            new Property(EMPLOYEE_SALARY, Type.DOUBLE, "Salary"),
            new Property(EMPLOYEE_COMMISSION, Type.DOUBLE, "Commission"),
            new Property.EntityProperty(EMPLOYEE_MGR_REF, "Manager", T_EMPLOYEE,
                    new Property(EMPLOYEE_MGR)),
            new Property(EMPLOYEE_HIREDATE, Type.SHORT_DATE, "Hiredate"),
            new Property.DenormalizedViewProperty(EmpDept.DEPARTMENT_LOCATION, T_DEPARTMENT, EmpDept.DEPARTMENT_LOCATION,
                    "Dept. location", 100));

    Entity.repository.setDefaultEntityProxy(new EntityProxy() {
      public String toString(final Entity entity) {
        if (entity.getEntityID().equals(T_DEPARTMENT))
          return entity.getStringValue(DEPARTMENT_NAME);
        else if (entity.getEntityID().equals(T_EMPLOYEE))
          return entity.getStringValue(EMPLOYEE_NAME)
                  + ", " + entity.getStringValue(EMPLOYEE_JOB)
                  + ", " + entity.getEntityValue(EMPLOYEE_DEPARTMENT_REF);

        return super.toString(entity);
      }
    });
  }
}
