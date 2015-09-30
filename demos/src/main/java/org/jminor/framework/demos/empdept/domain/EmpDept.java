/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.domain;

import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import java.awt.Color;
import java.sql.Types;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class contains the specification for the EmpDept application domain model
 */
public class EmpDept {

  private EmpDept() {}
  public static void init() {}

  private static final ResourceBundle bundle =
          ResourceBundle.getBundle("org.jminor.framework.demos.empdept.domain.EmpDept", Locale.getDefault());

  /**Used for i18n*/
  public static final String DEPARTMENT = "department";
  public static final String EMPLOYEE = "employee";
  public static final String NONE = "none";
  public static final String EMPLOYEE_REPORT = "employee_report";
  public static final String IMPORT_JSON = "import_json";

  /**Entity identifier for the table scott.dept*/
  public static final String T_DEPARTMENT = "scott.dept";

  /**Property identifiers for the columns in the scott.dept table*/
  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";

  /**Entity identifier for the table scott.emp*/
  public static final String T_EMPLOYEE = "scott.emp";

  /**Property identifiers for the columns in the scott.emp table*/
  public static final String EMPLOYEE_ID = "empno";
  public static final String EMPLOYEE_NAME = "ename";
  public static final String EMPLOYEE_JOB = "job";
  public static final String EMPLOYEE_MGR = "mgr";
  public static final String EMPLOYEE_HIREDATE = "hiredate";
  public static final String EMPLOYEE_SALARY = "sal";
  public static final String EMPLOYEE_COMMISSION = "comm";
  public static final String EMPLOYEE_DEPARTMENT = "deptno";
  /**Foreign key (reference) identifier for the DEPT column in the table scott.emp*/
  public static final String EMPLOYEE_DEPARTMENT_FK = "dept_fk";
  /**Foreign key (reference) identifier for the MGR column in the table scott.emp*/
  public static final String EMPLOYEE_MGR_FK = "mgr_fk";
  /**Property identifier for the denormalized department location property*/
  public static final String EMPLOYEE_DEPARTMENT_LOCATION = "location";

  static {
    /*Defining the entity type T_DEPARTMENT*/
    Entities.define(T_DEPARTMENT,
            Properties.primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, getString(DEPARTMENT_ID))
                    .setUpdatable(true).setNullable(false),
            Properties.columnProperty(DEPARTMENT_NAME, Types.VARCHAR, getString(DEPARTMENT_NAME))
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false),
            Properties.columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, getString(DEPARTMENT_LOCATION))
                    .setPreferredColumnWidth(150).setMaxLength(13))
            .setSmallDataset(true)
            .setOrderByClause(DEPARTMENT_NAME)
            .setStringProvider(new Entities.StringProvider(DEPARTMENT_NAME))
            .setCaption(getString(DEPARTMENT));

    /*Defining the entity type T_EMPLOYEE*/
    Entities.define(T_EMPLOYEE,
            Properties.primaryKeyProperty(EMPLOYEE_ID, Types.INTEGER, getString(EMPLOYEE_ID)),
            Properties.columnProperty(EMPLOYEE_NAME, Types.VARCHAR, getString(EMPLOYEE_NAME))
                    .setMaxLength(10).setNullable(false),
            Properties.foreignKeyProperty(EMPLOYEE_DEPARTMENT_FK, getString(EMPLOYEE_DEPARTMENT_FK), T_DEPARTMENT,
                    Properties.columnProperty(EMPLOYEE_DEPARTMENT))
                    .setNullable(false),
            Properties.columnProperty(EMPLOYEE_JOB, Types.VARCHAR, getString(EMPLOYEE_JOB))
                    .setMaxLength(9),
            Properties.columnProperty(EMPLOYEE_SALARY, Types.DOUBLE, getString(EMPLOYEE_SALARY))
                    .setNullable(false).setMin(1000).setMax(10000).setMaximumFractionDigits(2),
            Properties.columnProperty(EMPLOYEE_COMMISSION, Types.DOUBLE, getString(EMPLOYEE_COMMISSION))
                    .setMin(100).setMax(2000).setMaximumFractionDigits(2),
            Properties.foreignKeyProperty(EMPLOYEE_MGR_FK, getString(EMPLOYEE_MGR_FK), T_EMPLOYEE,
                    Properties.columnProperty(EMPLOYEE_MGR)),
            Properties.columnProperty(EMPLOYEE_HIREDATE, Types.DATE, getString(EMPLOYEE_HIREDATE))
                    .setNullable(false),
            Properties.denormalizedViewProperty(EMPLOYEE_DEPARTMENT_LOCATION, EMPLOYEE_DEPARTMENT_FK,
                    Entities.getProperty(T_DEPARTMENT, DEPARTMENT_LOCATION),
                    getString(DEPARTMENT_LOCATION)).setPreferredColumnWidth(100))
            .setKeyGenerator(Entities.incrementKeyGenerator(T_EMPLOYEE, EMPLOYEE_ID))
            .setOrderByClause(EMPLOYEE_DEPARTMENT + ", " + EMPLOYEE_NAME)
            .setStringProvider(new Entities.StringProvider(EMPLOYEE_NAME))
            .setCaption(getString(EMPLOYEE))
            .setBackgroundColorProvider(new Entity.BackgroundColorProvider() {
              /*provide a custom background color for managers*/
              @Override
              public Color getBackgroundColor(final Entity entity, final Property property) {
                if (property.is(EMPLOYEE_JOB) && "MANAGER".equals(entity.getValue(EMPLOYEE_JOB))) {
                  return Color.CYAN;
                }

                return null;
              }
            });
  }

  public static String getString(final String key) {
    return bundle.getString(key);
  }
}
