/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.domain;

import is.codion.common.item.Item;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.plugin.jasperreports.model.JasperReportWrapper;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.Entities.type;
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

  /** Entity type for the table scott.dept*/
  public interface Department {
    EntityType TYPE = type("scott.dept");
    /** Attributes for the columns in the scott.dept table*/
    Attribute<Integer> ID = TYPE.integerAttribute("deptno");
    Attribute<String> NAME = TYPE.stringAttribute("dname");
    Attribute<String> LOCATION = TYPE.stringAttribute("loc");
  }
  // end::departmentConstants[]

  // tag::employeeConstants[]
  /** Entity type for the table scott.emp*/
  public interface Employee {
    EntityType TYPE = type("scott.emp");
    /** Attributes for the columns in the scott.emp table*/
    Attribute<Integer> ID = TYPE.integerAttribute("empno");
    Attribute<String> NAME = TYPE.stringAttribute("ename");
    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Integer> MGR = TYPE.integerAttribute("mgr");
    Attribute<LocalDate> HIREDATE = TYPE.localDateAttribute("hiredate");
    Attribute<BigDecimal> SALARY = TYPE.bigDecimalAttribute("sal");
    Attribute<Double> COMMISSION = TYPE.doubleAttribute("comm");
    Attribute<Integer> DEPARTMENT = TYPE.integerAttribute("deptno");
    /**Foreign key (reference) attribute for the DEPT column in the table scott.emp*/
    Attribute<Entity> DEPARTMENT_FK = TYPE.entityAttribute("dept_fk");
    /**Foreign key (reference) attribute for the MGR column in the table scott.emp*/
    Attribute<Entity> MGR_FK = TYPE.entityAttribute("mgr_fk");
    /**Attribute for the denormalized department location property*/
    Attribute<String> DEPARTMENT_LOCATION = TYPE.stringAttribute("location");

    JasperReportWrapper EMPLOYEE_REPORT =
            classPathReport(EmpDept.class, "empdept_employees.jasper");

    List<Item<String>> JOB_VALUES = asList(
                    item("ANALYST", "Analyst"), item("CLERK", "Clerk"),
                    item("MANAGER", "Manager"), item("PRESIDENT", "President"),
                    item("SALESMAN", "Salesman"));
  }
  // end::employeeConstants[]

  // tag::constructor[]
  /** Initializes this domain model */
  public EmpDept() {
    department();
    employee();
    addReport(Employee.EMPLOYEE_REPORT);
  }
  // end::constructor[]

  // tag::defineDepartment[]
  void department() {
    /*Defining the entity Department.TYPE*/
    define(Department.TYPE,
            primaryKeyProperty(Department.ID, "Department no.")
                    .updatable(true).nullable(false).beanProperty("id"),
            columnProperty(Department.NAME, "Department name")
                    .preferredColumnWidth(120).maximumLength(14).nullable(false).beanProperty("name"),
            columnProperty(Department.LOCATION, "Location")
                    .preferredColumnWidth(150).maximumLength(13).beanProperty("location"))
            .smallDataset(true)
            .orderBy(orderBy().ascending(Department.NAME))
            .stringProvider(new StringProvider(Department.NAME))
            .beanClass(DepartmentBean.class)
            .caption("Departments");
  }
  // end::defineDepartment[]

  // tag::defineEmployee[]
  void employee() {
    /*Defining the entity Employee.TYPE*/
    define(Employee.TYPE,
            primaryKeyProperty(Employee.ID, "Employee no.").beanProperty("id"),
            columnProperty(Employee.NAME, "Name")
                    .searchProperty(true).maximumLength(10).nullable(false).beanProperty("name"),
            foreignKeyProperty(Employee.DEPARTMENT_FK, "Department", Department.TYPE,
                    columnProperty(Employee.DEPARTMENT))
                    .nullable(false).beanProperty("department"),
            valueListProperty(Employee.JOB, "Job", Employee.JOB_VALUES).beanProperty("job"),
            columnProperty(Employee.SALARY, "Salary")
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2).beanProperty("salary"),
            columnProperty(Employee.COMMISSION, "Commission")
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2).beanProperty("commission"),
            foreignKeyProperty(Employee.MGR_FK, "Manager", Employee.TYPE,
                    columnProperty(Employee.MGR)).beanProperty("manager"),
            columnProperty(Employee.HIREDATE, "Hiredate")
                    .nullable(false).beanProperty("hiredate"),
            denormalizedViewProperty(Employee.DEPARTMENT_LOCATION, Employee.DEPARTMENT_FK, Department.LOCATION, "Location")
                    .preferredColumnWidth(100))
            .keyGenerator(increment("scott.emp", Employee.ID.getName()))
            .orderBy(orderBy().ascending(Employee.DEPARTMENT, Employee.NAME))
            .stringProvider(new StringProvider(Employee.NAME))
            .beanClass(EmployeeBean.class)
            .caption("Employee")
            .colorProvider((entity, property) -> {
              if (property.is(Employee.JOB) && "MANAGER".equals(entity.get(Employee.JOB))) {
                return Color.CYAN;
              }

              return null;
            });
  }
}
// end::defineEmployee[]
