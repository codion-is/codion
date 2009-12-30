/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.domain;

import org.jminor.common.db.IdSource;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
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
          ResourceBundle.getBundle("org.jminor.framework.demos.empdept.domain.EmpDept", Locale.getDefault());

  /**Used for i18n*/
  public static final String NONE = "none";
  public static final String EMPLOYEE_SALARY_VALIDATION = "employee_salary_validation";
  public static final String EMPLOYEE_COMMISSION_VALIDATION = "employee_commission_validation";
  public static final String EMPLOYEE_REPORT = "employee_report";

  /**Entity identifier for the table scott.dept*/
  public static final String T_DEPARTMENT = "department";

  /**Property identifier for the DEPTNO column in the table scott.dept*/
  public static final String DEPARTMENT_ID = "deptno";
  /**Property identifier for the DNAME column in the table scott.dept*/
  public static final String DEPARTMENT_NAME = "dname";
  /**Property identifier for the LOC column in the table scott.dept*/
  public static final String DEPARTMENT_LOCATION = "loc";

  /**Entity identifier for the table scott.emp*/
  public static final String T_EMPLOYEE = "employee";

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
    /*Defining the entity type T_DEPARTMENT*/
    EntityRepository.add(new EntityDefinition(T_DEPARTMENT, "scott.dept",
            new Property.PrimaryKeyProperty(DEPARTMENT_ID, Type.INT, getString(DEPARTMENT_ID))
                    .setNullable(false),
            new Property(DEPARTMENT_NAME, Type.STRING, getString(DEPARTMENT_NAME))
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false),
            new Property(DEPARTMENT_LOCATION, Type.STRING, getString(DEPARTMENT_LOCATION))
                    .setPreferredColumnWidth(150).setMaxLength(13)).setOrderByClause(DEPARTMENT_NAME));

    /*Set a Proxy implementation to provide toString values for the entities*/
    Entity.setProxy(T_DEPARTMENT, new Entity.Proxy() {
      @Override
      public String toString(final Entity entity) {
          return entity.getStringValue(DEPARTMENT_NAME);
      }
    });

    /*Defining the entity type T_EMPLOYEE*/
    EntityRepository.add(new EntityDefinition(T_EMPLOYEE, "scott.emp",
            new Property.PrimaryKeyProperty(EMPLOYEE_ID, Type.INT, getString(EMPLOYEE_ID)),
            new Property(EMPLOYEE_NAME, Type.STRING, getString(EMPLOYEE_NAME))
                    .setMaxLength(10).setNullable(false),
            new Property.ForeignKeyProperty(EMPLOYEE_DEPARTMENT_FK, getString(EMPLOYEE_DEPARTMENT_FK), T_DEPARTMENT,
                    new Property(EMPLOYEE_DEPARTMENT))
                    .setNullable(false),
            new Property(EMPLOYEE_JOB, Type.STRING, getString(EMPLOYEE_JOB))
                    .setMaxLength(9),
            new Property(EMPLOYEE_SALARY, Type.DOUBLE, getString(EMPLOYEE_SALARY))
                    .setNullable(false),
            new Property(EMPLOYEE_COMMISSION, Type.DOUBLE, getString(EMPLOYEE_COMMISSION)),
            new Property.ForeignKeyProperty(EMPLOYEE_MGR_FK, getString(EMPLOYEE_MGR_FK), T_EMPLOYEE,
                    new Property(EMPLOYEE_MGR)),
            new Property(EMPLOYEE_HIREDATE, Type.DATE, getString(EMPLOYEE_HIREDATE))
                    .setNullable(false),
            new Property.DenormalizedViewProperty(EMPLOYEE_DEPARTMENT_LOCATION, EMPLOYEE_DEPARTMENT_FK,
                    EntityRepository.getProperty(T_DEPARTMENT, DEPARTMENT_LOCATION),
                    getString(DEPARTMENT_LOCATION)).setPreferredColumnWidth(100))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setOrderByClause(EMPLOYEE_DEPARTMENT + ", " + EMPLOYEE_NAME));

    /*Set a Proxy implementation to provide toString values for the entities
    * and custom background color for managers*/
    Entity.setProxy(T_EMPLOYEE, new Entity.Proxy() {
      @Override
      public String toString(final Entity entity) {
        return entity.getStringValue(EMPLOYEE_NAME);
      }

      @Override
      public Color getBackgroundColor(final Entity entity) {
        if (entity.getStringValue(EMPLOYEE_JOB).equals("MANAGER"))
          return Color.CYAN;

        return super.getBackgroundColor(entity);
      }
    });
  }

  public static String getString(final String key) {
    return bundle.getString(key);
  }
}
