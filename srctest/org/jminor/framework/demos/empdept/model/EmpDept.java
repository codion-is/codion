/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.model;

import org.jminor.common.db.IdSource;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityProxy;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

/**
 * This class contains the specification for the EmpDept application domain model
 */
public class EmpDept {

  /**Entity identifier for the table scott.dept*/
  public static final String T_DEPARTMENT = "scott.dept";

  /**Property identifier for the DEPTNO column in the table scott.dept*/
  public static final String DEPARTMENT_ID = "deptno";
  /**Property identifier for the DNAME column in the table scott.dept*/
  public static final String DEPARTMENT_NAME = "dname";
  /**Property identifier for the LOC column in the table scott.dept*/
  public static final String DEPARTMENT_LOCATION = "loc";

  /**Entity identifier for the table scott.emp*/
  public static final String T_EMPLOYEE = "scott.emp";

  /**Property identifier for the EMPNO column in the table scott.emp*/
  public static final String EMPLOYEE_ID = "empno";
  /**Property identifier for the ENANE column in the table scott.emp*/
  public static final String EMPLOYEE_NAME = "ename";
  /**Property identifier for the JOB column in the table scott.emp*/
  public static final String EMPLOYEE_JOB = "job";
  /**Property identifier for the MGR column in the table scott.emp*/
  public static final String EMPLOYEE_MGR = "mgr";
  /**Foreign key (reference) identifier for the MGR column in the table scott.emp*/
  public static final String EMPLOYEE_MGR_REF = "mgr_ref";
  /**Property identifier for the HIREDATE column in the table scott.emp*/
  public static final String EMPLOYEE_HIREDATE = "hiredate";
  /**Property identifier for the SAL column in the table scott.emp*/
  public static final String EMPLOYEE_SALARY = "sal";
  /**Property identifier for the COMM column in the table scott.emp*/
  public static final String EMPLOYEE_COMMISSION = "comm";
  /**Property identifier for the DEPTNO column in the table scott.emp*/
  public static final String EMPLOYEE_DEPARTMENT = "deptno";
  /**Foreign key (reference) identifier for the DEPT column in the table scott.emp*/
  public static final String EMPLOYEE_DEPARTMENT_REF = "dept_ref";

  static {
    /*Initalizing the entity type T_DEPARTMENT*/
    EntityRepository.get().initialize(T_DEPARTMENT, IdSource.ID_NONE, null, DEPARTMENT_NAME,
            new Property.PrimaryKeyProperty(DEPARTMENT_ID, Type.INT, "Dept.no "),
            new Property(DEPARTMENT_NAME, Type.STRING, "Name", false, false, 120),
            new Property(DEPARTMENT_LOCATION, Type.STRING, "Location", false, false, 150));

    /*Initalizing the entity type T_EMPLOYEE*/
    EntityRepository.get().initialize(T_EMPLOYEE, IdSource.ID_MAX_PLUS_ONE, null,
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
            new Property.DenormalizedViewProperty(EmpDept.DEPARTMENT_LOCATION, EMPLOYEE_DEPARTMENT_REF,
                    EntityRepository.get().getProperty(T_DEPARTMENT, EmpDept.DEPARTMENT_LOCATION),
                    "Dept. location", 100));

    /*Set a EntityProxy implementation which provides toString values for the entities*/
    EntityProxy.setDefaultEntityProxy(new EntityProxy() {
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
