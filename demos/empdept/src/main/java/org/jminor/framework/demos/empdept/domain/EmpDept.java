/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.domain;

import org.jminor.common.item.Item;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.StringProvider;
import org.jminor.plugin.jasperreports.model.JasperReportWrapper;

import java.awt.Color;
import java.sql.Types;
import java.util.List;

import static java.util.Arrays.asList;
import static org.jminor.common.item.Items.item;
import static org.jminor.framework.domain.entity.KeyGenerators.increment;
import static org.jminor.framework.domain.entity.OrderBy.orderBy;
import static org.jminor.framework.domain.property.Properties.*;
import static org.jminor.plugin.jasperreports.model.JasperReports.classPathReport;

// tag::departmentConstants[]
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
// end::departmentConstants[]

// tag::employeeConstants[]
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

  public static final JasperReportWrapper EMPLOYEE_REPORT =
          classPathReport(EmpDept.class, "empdept_employees.jasper");

  public static final List<Item> JOB_VALUES = asList(
          item("ANALYST", "Analyst"), item("CLERK", "Clerk"),
          item("MANAGER", "Manager"), item("PRESIDENT", "President"),
          item("SALESMAN", "Salesman"));
// end::employeeConstants[]

// tag::constructor[]
  /** Initializes this domain model */
  public EmpDept() {
    department();
    employee();
  }
// end::constructor[]

// tag::defineDepartment[]
  void department() {
    /*Defining the entity type T_DEPARTMENT*/
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, "Department no.")
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, "Department name")
                    .preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, "Location")
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .orderBy(orderBy().ascending(DEPARTMENT_NAME))
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .caption("Departments");
  }
// end::defineDepartment[]

// tag::defineEmployee[]
  void employee() {
    /*Defining the entity type T_EMPLOYEE*/
    define(T_EMPLOYEE,
            primaryKeyProperty(EMPLOYEE_ID, Types.INTEGER, "Employee no."),
            columnProperty(EMPLOYEE_NAME, Types.VARCHAR, "Name")
                    .maximumLength(10).nullable(false),
            foreignKeyProperty(EMPLOYEE_DEPARTMENT_FK, "Department", T_DEPARTMENT,
                    columnProperty(EMPLOYEE_DEPARTMENT))
                    .nullable(false),
            valueListProperty(EMPLOYEE_JOB, Types.VARCHAR, "Job", JOB_VALUES),
            columnProperty(EMPLOYEE_SALARY, Types.DECIMAL, "Salary")
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2),
            columnProperty(EMPLOYEE_COMMISSION, Types.DOUBLE, "Commission")
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2),
            foreignKeyProperty(EMPLOYEE_MGR_FK, "Manager", T_EMPLOYEE,
                    columnProperty(EMPLOYEE_MGR)),
            columnProperty(EMPLOYEE_HIREDATE, Types.DATE, "Hiredate")
                    .nullable(false),
            denormalizedViewProperty(EMPLOYEE_DEPARTMENT_LOCATION, EMPLOYEE_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION), "Location")
                    .preferredColumnWidth(100))
            .keyGenerator(increment(T_EMPLOYEE, EMPLOYEE_ID))
            .orderBy(orderBy().ascending(EMPLOYEE_DEPARTMENT, EMPLOYEE_NAME))
            .searchPropertyIds(EMPLOYEE_NAME)
            .stringProvider(new StringProvider(EMPLOYEE_NAME))
            .caption("Employee")
            .colorProvider((entity, property) -> {
              if (property.is(EMPLOYEE_JOB) && "MANAGER".equals(entity.get(EMPLOYEE_JOB))) {
                return Color.CYAN;
              }

              return null;
            });
  }
}
// end::defineEmployee[]
