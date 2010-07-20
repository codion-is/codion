/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.domain;

import org.jminor.common.model.IdSource;
import org.jminor.common.model.Version;
import org.jminor.common.model.valuemap.StringProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Properties;

import java.awt.Color;
import java.sql.Types;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class contains the specification for the EmpDept application domain model
 */
public class EmpDept {

  private static final ResourceBundle bundle =
          ResourceBundle.getBundle("org.jminor.framework.demos.empdept.domain.EmpDept", Locale.getDefault());

  public static final Version version = new Version("EmpDept", "v1.0");

  /**Used for i18n*/
  public static final String DEPARTMENT = "department";
  public static final String EMPLOYEE = "employee";
  public static final String NONE = "none";
  public static final String EMPLOYEE_REPORT = "employee_report";
  public static final String IMPORT_JSON = "import_json";

  /**Entity identifier for the table scott.dept*/
  public static final String T_DEPARTMENT = "department" + version;

  /**Property identifier for the DEPTNO column in the table scott.dept*/
  public static final String DEPARTMENT_ID = "deptno";
  /**Property identifier for the DNAME column in the table scott.dept*/
  public static final String DEPARTMENT_NAME = "dname";
  /**Property identifier for the LOC column in the table scott.dept*/
  public static final String DEPARTMENT_LOCATION = "loc";

  /**Entity identifier for the table scott.emp*/
  public static final String T_EMPLOYEE = "employee" + version;

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
    EntityRepository.add(Entities.define(T_DEPARTMENT, "scott.dept",
            Properties.primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, getString(DEPARTMENT_ID))
                    .setNullable(false),
            Properties.columnProperty(DEPARTMENT_NAME, Types.VARCHAR, getString(DEPARTMENT_NAME))
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false),
            Properties.columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, getString(DEPARTMENT_LOCATION))
                    .setPreferredColumnWidth(150).setMaxLength(13))
            .setIdSource(IdSource.NONE)
            .setOrderByClause(DEPARTMENT_NAME)
            .setStringProvider(new StringProvider<String>(DEPARTMENT_NAME))
            .setCaption(getString(EMPLOYEE)));

    /*Defining the entity type T_EMPLOYEE*/
    EntityRepository.add(Entities.define(T_EMPLOYEE, "scott.emp",
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
                    EntityRepository.getProperty(T_DEPARTMENT, DEPARTMENT_LOCATION),
                    getString(DEPARTMENT_LOCATION)).setPreferredColumnWidth(100))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setOrderByClause(EMPLOYEE_DEPARTMENT + ", " + EMPLOYEE_NAME)
            .setStringProvider(new StringProvider<String>(EMPLOYEE_NAME))
            .setRowColoring(true)
            .setCaption(getString(DEPARTMENT)));

    /*Set a Proxy implementation to provide a custom background color for managers*/
    EntityRepository.setProxy(T_EMPLOYEE, new EntityRepository.Proxy() {
      @Override
      public Color getBackgroundColor(final Entity entity) {
        if (!entity.isValueNull(EMPLOYEE_JOB) && entity.getStringValue(EMPLOYEE_JOB).equals("MANAGER")) {
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
