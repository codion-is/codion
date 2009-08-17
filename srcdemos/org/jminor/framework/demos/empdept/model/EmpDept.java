/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.model;

import org.jminor.common.db.IdSource;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityProxy;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import java.awt.Color;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class contains the specification for the EmpDept application domain model
 */
public class EmpDept {

  private static final ResourceBundle bundle =
          ResourceBundle.getBundle("org.jminor.framework.demos.empdept.model.EmpDept", Locale.getDefault());

  /**Used for i18n*/
  public static final String NONE = "none";

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
  public static final String EMPLOYEE_MGR_FK = "mgr_fk";
  /**Property identifier for the HIREDATE column in the table scott.emp*/
  public static final String EMPLOYEE_HIREDATE = "hiredate";
  /**Property identifier for the SAL column in the table scott.emp*/
  public static final String EMPLOYEE_SALARY = "sal";
  /**Property identifier for the COMM column in the table scott.emp*/
  public static final String EMPLOYEE_COMMISSION = "comm";
  /**Property identifier for the DEPTNO column in the table scott.emp*/
  public static final String EMPLOYEE_DEPARTMENT = "deptno";
  /**Foreign key (reference) identifier for the DEPT column in the table scott.emp*/
  public static final String EMPLOYEE_DEPARTMENT_FK = "dept_fk";
  /**Property identifier for the denormalized department location property*/
  public static final String EMPLOYEE_DEPARTMENT_LOCATION = "location";

  static {
    /*Initalizing the entity type T_DEPARTMENT*/
    EntityRepository.get().initialize(T_DEPARTMENT, IdSource.NONE, null, DEPARTMENT_NAME,
            new Property.PrimaryKeyProperty(DEPARTMENT_ID, Type.INT, getString(DEPARTMENT_ID)),
            new Property(DEPARTMENT_NAME, Type.STRING, getString(DEPARTMENT_NAME), false, false, 120),
            new Property(DEPARTMENT_LOCATION, Type.STRING, getString(DEPARTMENT_LOCATION), false, false, 150));

    /*Initalizing the entity type T_EMPLOYEE*/
    EntityRepository.get().initialize(T_EMPLOYEE, IdSource.MAX_PLUS_ONE, null,
            EMPLOYEE_DEPARTMENT + ", " + EMPLOYEE_NAME,
            new Property.PrimaryKeyProperty(EMPLOYEE_ID, Type.INT, getString(EMPLOYEE_ID)),
            new Property(EMPLOYEE_NAME, Type.STRING, getString(EMPLOYEE_NAME)),
            new Property.ForeignKeyProperty(EMPLOYEE_DEPARTMENT_FK, getString(EMPLOYEE_DEPARTMENT_FK), T_DEPARTMENT,
                    new Property(EMPLOYEE_DEPARTMENT)),
            new Property(EMPLOYEE_JOB, Type.STRING, getString(EMPLOYEE_JOB)),
            new Property(EMPLOYEE_SALARY, Type.DOUBLE, getString(EMPLOYEE_SALARY)),
            new Property(EMPLOYEE_COMMISSION, Type.DOUBLE, getString(EMPLOYEE_COMMISSION)),
            new Property.ForeignKeyProperty(EMPLOYEE_MGR_FK, getString(EMPLOYEE_MGR_FK), T_EMPLOYEE,
                    new Property(EMPLOYEE_MGR)),
            new Property(EMPLOYEE_HIREDATE, Type.SHORT_DATE, getString(EMPLOYEE_HIREDATE)),
            new Property.DenormalizedViewProperty(EMPLOYEE_DEPARTMENT_LOCATION, EMPLOYEE_DEPARTMENT_FK,
                    EntityRepository.get().getProperty(T_DEPARTMENT, DEPARTMENT_LOCATION),
                    getString(DEPARTMENT_LOCATION), 100));

    /*Set a EntityProxy implementation to provide toString values for the entities
    * and custom background color for managers*/
    EntityProxy.setDefaultEntityProxy(new EntityProxy() {
      @Override
      public String toString(final Entity entity) {
        if (entity.is(T_DEPARTMENT))
          return entity.getStringValue(DEPARTMENT_NAME);
        else if (entity.is(T_EMPLOYEE))
          return entity.getStringValue(EMPLOYEE_NAME)
                  + ", " + entity.getStringValue(EMPLOYEE_JOB)
                  + ", " + entity.getEntityValue(EMPLOYEE_DEPARTMENT_FK);

        return super.toString(entity);
      }

      @Override
      public Color getBackgroundColor(final Entity entity) {
        if (entity.is(T_EMPLOYEE)) {
          if (entity.getStringValue(EMPLOYEE_JOB).equals("MANAGER"))
            return Color.CYAN;
        }

        return super.getBackgroundColor(entity);
      }
    });
  }

  public static String getString(final String key) {
    return bundle.getString(key);
  }
}
