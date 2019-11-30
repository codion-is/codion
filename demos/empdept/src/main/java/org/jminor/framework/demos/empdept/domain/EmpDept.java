/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.domain;

import org.jminor.common.Item;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.StringProvider;

import java.awt.Color;
import java.sql.Types;
import java.util.List;

import static java.util.Arrays.asList;
import static org.jminor.framework.domain.KeyGenerators.increment;
import static org.jminor.framework.domain.property.Properties.*;

/**
 * This class contains the specification for the EmpDept application domain model
 */
public final class EmpDept extends Domain {

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

  public static final List<Item> JOB_VALUES = asList(
          new Item("ANALYST"), new Item("CLERK"), new Item("MANAGER"),
          new Item("PRESIDENT"), new Item("SALESMAN"));

  /** Initializes this domain model */
  public EmpDept() {
    department();
    employee();
  }

  void department() {
    /*Defining the entity type T_DEPARTMENT*/
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, "Department no.")
                    .setUpdatable(true).setNullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, "Department name")
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, "Location")
                    .setPreferredColumnWidth(150).setMaxLength(13))
            .setSmallDataset(true)
            .setOrderBy(orderBy().ascending(DEPARTMENT_NAME))
            .setStringProvider(new StringProvider(DEPARTMENT_NAME))
            .setCaption("Departments");
  }

  void employee() {
    /*Defining the entity type T_EMPLOYEE*/
    define(T_EMPLOYEE,
            primaryKeyProperty(EMPLOYEE_ID, Types.INTEGER, "Employee no."),
            columnProperty(EMPLOYEE_NAME, Types.VARCHAR, "Name")
                    .setMaxLength(10).setNullable(false),
            foreignKeyProperty(EMPLOYEE_DEPARTMENT_FK, "Department", T_DEPARTMENT,
                    columnProperty(EMPLOYEE_DEPARTMENT))
                    .setNullable(false),
            valueListProperty(EMPLOYEE_JOB, Types.VARCHAR, "Job", JOB_VALUES),
            columnProperty(EMPLOYEE_SALARY, Types.DECIMAL, "Salary")
                    .setNullable(false).setMin(1000).setMax(10000).setMaximumFractionDigits(2),
            columnProperty(EMPLOYEE_COMMISSION, Types.DOUBLE, "Commission")
                    .setMin(100).setMax(2000).setMaximumFractionDigits(2),
            foreignKeyProperty(EMPLOYEE_MGR_FK, "Manager", T_EMPLOYEE,
                    columnProperty(EMPLOYEE_MGR)),
            columnProperty(EMPLOYEE_HIREDATE, Types.DATE, "Hiredate")
                    .setNullable(false),
            denormalizedViewProperty(EMPLOYEE_DEPARTMENT_LOCATION, EMPLOYEE_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION), "Location")
                    .setPreferredColumnWidth(100))
            .setKeyGenerator(increment(T_EMPLOYEE, EMPLOYEE_ID))
            .setOrderBy(orderBy().ascending(EMPLOYEE_DEPARTMENT, EMPLOYEE_NAME))
            .setSearchPropertyIds(EMPLOYEE_NAME)
            .setStringProvider(new StringProvider(EMPLOYEE_NAME))
            .setCaption("Employee")
            .setColorProvider((entity, property) -> {
              if (property.is(EMPLOYEE_JOB) && "MANAGER".equals(entity.get(EMPLOYEE_JOB))) {
                return Color.CYAN;
              }

              return null;
            });
  }
}
