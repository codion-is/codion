/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.domain;

import is.codion.common.item.Item;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.plugin.jasperreports.model.JasperReportWrapper;

import java.awt.Color;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static java.util.Arrays.asList;

// tag::departmentConstants[]
/**
 * This class contains the specification for the EmpDept application domain model
 */
public final class EmpDept extends Domain {

  /** Entity identifier for the table scott.dept*/
  public static final String T_DEPARTMENT = "scott.dept";

  /** Attributes for the columns in the scott.dept table*/
  public static final Attribute<Integer> DEPARTMENT_ID = attribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = attribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = attribute("loc");
  // end::departmentConstants[]

  // tag::employeeConstants[]
  /** Entity identifier for the table scott.emp*/
  public static final String T_EMPLOYEE = "scott.emp";

  /** Attributes for the columns in the scott.emp table*/
  public static final Attribute<Integer> EMPLOYEE_ID = attribute("empno");
  public static final Attribute<String> EMPLOYEE_NAME = attribute("ename");
  public static final Attribute<String> EMPLOYEE_JOB = attribute("job");
  public static final Attribute<Integer> EMPLOYEE_MGR = attribute("mgr");
  public static final Attribute<LocalDate> EMPLOYEE_HIREDATE = attribute("hiredate");
  public static final Attribute<BigDecimal> EMPLOYEE_SALARY = attribute("sal");
  public static final Attribute<Double> EMPLOYEE_COMMISSION = attribute("comm");
  public static final Attribute<Integer> EMPLOYEE_DEPARTMENT = attribute("deptno");
  /**Foreign key (reference) identifier for the DEPT column in the table scott.emp*/
  public static final Attribute<Entity> EMPLOYEE_DEPARTMENT_FK = attribute("dept_fk");
  /**Foreign key (reference) identifier for the MGR column in the table scott.emp*/
  public static final Attribute<Entity> EMPLOYEE_MGR_FK = attribute("mgr_fk");
  /**Property identifier for the denormalized department location property*/
  public static final Attribute<String> EMPLOYEE_DEPARTMENT_LOCATION = attribute("location");

  public static final JasperReportWrapper EMPLOYEE_REPORT =
          classPathReport(EmpDept.class, "empdept_employees.jasper");

  public static final List<Item<String>> JOB_VALUES = asList(
          item("ANALYST", "Analyst"), item("CLERK", "Clerk"),
          item("MANAGER", "Manager"), item("PRESIDENT", "President"),
          item("SALESMAN", "Salesman"));
  // end::employeeConstants[]

  // tag::constructor[]
  /** Initializes this domain model */
  public EmpDept() {
    department();
    employee();
    addReport(EMPLOYEE_REPORT);
  }
  // end::constructor[]

  // tag::defineDepartment[]
  void department() {
    /*Defining the entity type T_DEPARTMENT*/
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, "Department no.")
                    .updatable(true).nullable(false).beanProperty("id"),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, "Department name")
                    .preferredColumnWidth(120).maximumLength(14).nullable(false).beanProperty("name"),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, "Location")
                    .preferredColumnWidth(150).maximumLength(13).beanProperty("location"))
            .smallDataset(true)
            .orderBy(orderBy().ascending(DEPARTMENT_NAME))
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .beanClass(Department.class)
            .caption("Departments");
  }
  // end::defineDepartment[]

  // tag::defineEmployee[]
  void employee() {
    /*Defining the entity type T_EMPLOYEE*/
    define(T_EMPLOYEE,
            primaryKeyProperty(EMPLOYEE_ID, Types.INTEGER, "Employee no.").beanProperty("id"),
            columnProperty(EMPLOYEE_NAME, Types.VARCHAR, "Name")
                    .searchProperty(true).maximumLength(10).nullable(false).beanProperty("name"),
            foreignKeyProperty(EMPLOYEE_DEPARTMENT_FK, "Department", T_DEPARTMENT,
                    columnProperty(EMPLOYEE_DEPARTMENT, Types.INTEGER))
                    .nullable(false).beanProperty("department"),
            valueListProperty(EMPLOYEE_JOB, Types.VARCHAR, "Job", JOB_VALUES).beanProperty("job"),
            columnProperty(EMPLOYEE_SALARY, Types.DECIMAL, "Salary")
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2).beanProperty("salary"),
            columnProperty(EMPLOYEE_COMMISSION, Types.DOUBLE, "Commission")
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2).beanProperty("commission"),
            foreignKeyProperty(EMPLOYEE_MGR_FK, "Manager", T_EMPLOYEE,
                    columnProperty(EMPLOYEE_MGR, Types.INTEGER)).beanProperty("manager"),
            columnProperty(EMPLOYEE_HIREDATE, Types.DATE, "Hiredate")
                    .nullable(false).beanProperty("hiredate"),
            denormalizedViewProperty(EMPLOYEE_DEPARTMENT_LOCATION, EMPLOYEE_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION), "Location")
                    .preferredColumnWidth(100))
            .keyGenerator(increment(T_EMPLOYEE, EMPLOYEE_ID.getName()))
            .orderBy(orderBy().ascending(EMPLOYEE_DEPARTMENT, EMPLOYEE_NAME))
            .stringProvider(new StringProvider(EMPLOYEE_NAME))
            .beanClass(Employee.class)
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
