/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.domain;

import is.codion.common.item.Item;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.EntityAttribute;
import is.codion.plugin.jasperreports.model.JasperReportWrapper;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static java.sql.Types.*;
import static java.util.Arrays.asList;

// tag::departmentConstants[]
/**
 * This class contains the specification for the EmpDept application domain model
 */
public final class EmpDept extends Domain {

  /** Entity identifier for the table scott.dept*/
  public static final String T_DEPARTMENT = "scott.dept";

  /** Attributes for the columns in the scott.dept table*/
  public static final Attribute<Integer> DEPARTMENT_ID = attribute("deptno", INTEGER);
  public static final Attribute<String> DEPARTMENT_NAME = attribute("dname", VARCHAR);
  public static final Attribute<String> DEPARTMENT_LOCATION = attribute("loc", VARCHAR);
  // end::departmentConstants[]

  // tag::employeeConstants[]
  /** Entity identifier for the table scott.emp*/
  public static final String T_EMPLOYEE = "scott.emp";

  /** Attributes for the columns in the scott.emp table*/
  public static final Attribute<Integer> EMPLOYEE_ID = attribute("empno", INTEGER);
  public static final Attribute<String> EMPLOYEE_NAME = attribute("ename", VARCHAR);
  public static final Attribute<String> EMPLOYEE_JOB = attribute("job", VARCHAR);
  public static final Attribute<Integer> EMPLOYEE_MGR = attribute("mgr", INTEGER);
  public static final Attribute<LocalDate> EMPLOYEE_HIREDATE = attribute("hiredate", DATE);
  public static final Attribute<BigDecimal> EMPLOYEE_SALARY = attribute("sal", DECIMAL);
  public static final Attribute<Double> EMPLOYEE_COMMISSION = attribute("comm", DOUBLE);
  public static final Attribute<Integer> EMPLOYEE_DEPARTMENT = attribute("deptno", INTEGER);
  /**Foreign key (reference) identifier for the DEPT column in the table scott.emp*/
  public static final EntityAttribute EMPLOYEE_DEPARTMENT_FK = entityAttribute("dept_fk");
  /**Foreign key (reference) identifier for the MGR column in the table scott.emp*/
  public static final EntityAttribute EMPLOYEE_MGR_FK = entityAttribute("mgr_fk");
  /**Property identifier for the denormalized department location property*/
  public static final Attribute<String> EMPLOYEE_DEPARTMENT_LOCATION = attribute("location", VARCHAR);

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
            primaryKeyProperty(DEPARTMENT_ID, "Department no.")
                    .updatable(true).nullable(false).beanProperty("id"),
            columnProperty(DEPARTMENT_NAME, "Department name")
                    .preferredColumnWidth(120).maximumLength(14).nullable(false).beanProperty("name"),
            columnProperty(DEPARTMENT_LOCATION, "Location")
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
            primaryKeyProperty(EMPLOYEE_ID, "Employee no.").beanProperty("id"),
            columnProperty(EMPLOYEE_NAME, "Name")
                    .searchProperty(true).maximumLength(10).nullable(false).beanProperty("name"),
            foreignKeyProperty(EMPLOYEE_DEPARTMENT_FK, "Department", T_DEPARTMENT,
                    columnProperty(EMPLOYEE_DEPARTMENT))
                    .nullable(false).beanProperty("department"),
            valueListProperty(EMPLOYEE_JOB, "Job", JOB_VALUES).beanProperty("job"),
            columnProperty(EMPLOYEE_SALARY, "Salary")
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2).beanProperty("salary"),
            columnProperty(EMPLOYEE_COMMISSION, "Commission")
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2).beanProperty("commission"),
            foreignKeyProperty(EMPLOYEE_MGR_FK, "Manager", T_EMPLOYEE,
                    columnProperty(EMPLOYEE_MGR)).beanProperty("manager"),
            columnProperty(EMPLOYEE_HIREDATE, "Hiredate")
                    .nullable(false).beanProperty("hiredate"),
            denormalizedViewProperty(EMPLOYEE_DEPARTMENT_LOCATION, EMPLOYEE_DEPARTMENT_FK, DEPARTMENT_LOCATION, "Location")
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
