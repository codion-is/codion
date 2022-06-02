/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.domain;

import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.plugin.jasperreports.model.JRReportType;
import is.codion.plugin.jasperreports.model.JasperReports;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.property.Properties.*;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static java.util.Arrays.asList;

// tag::departmentConstants[]
/**
 * This class contains the specification for the EmpDept application domain model
 */
public final class EmpDept extends DefaultDomain {

  /** The domain type identifying this domain model*/
  static final DomainType DOMAIN = domainType(EmpDept.class);

  /** Entity type for the table scott.dept*/
  public interface Department extends Entity {
    EntityType TYPE = DOMAIN.entityType("scott.dept", Department.class);

    /** Attributes for the columns in the scott.dept table*/
    Attribute<Integer> ID = TYPE.integerAttribute("deptno");
    Attribute<String> NAME = TYPE.stringAttribute("dname");
    Attribute<String> LOCATION = TYPE.stringAttribute("loc");

    /** Bean getters and setters */
    Integer getId();
    void setId(Integer id);
    String getName();
    void setName(String name);
    String getLocation();
    void setLocation(String location);
  }
  // end::departmentConstants[]

  // tag::employeeConstants[]
  /** Entity type for the table scott.emp*/
  public interface Employee extends Entity {
    EntityType TYPE = DOMAIN.entityType("scott.emp", Employee.class);

    /** Attributes for the columns in the scott.emp table*/
    Attribute<Integer> ID = TYPE.integerAttribute("empno");
    Attribute<String> NAME = TYPE.stringAttribute("ename");
    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Integer> MGR = TYPE.integerAttribute("mgr");
    Attribute<LocalDate> HIREDATE = TYPE.localDateAttribute("hiredate");
    Attribute<BigDecimal> SALARY = TYPE.bigDecimalAttribute("sal");
    Attribute<Double> COMMISSION = TYPE.doubleAttribute("comm");
    Attribute<Integer> DEPARTMENT = TYPE.integerAttribute("deptno");

    /**Foreign key (reference) attribute for the DEPTNO column in the table scott.emp*/
    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
    /**Foreign key (reference) attribute for the MGR column in the table scott.emp*/
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, Employee.ID);
    /**Attribute for the denormalized department location property*/
    Attribute<String> DEPARTMENT_LOCATION = TYPE.stringAttribute("location");

    JRReportType EMPLOYEE_REPORT = JasperReports.reportType("employee_report");

    List<Item<String>> JOB_VALUES = asList(
                    item("ANALYST", "Analyst"), item("CLERK", "Clerk"),
                    item("MANAGER", "Manager"), item("PRESIDENT", "President"),
                    item("SALESMAN", "Salesman"));

    /** Bean getters and setters */
    Integer getId();
    void setId(Integer id);
    String getName();
    void setName(String name);
    String getJob();
    void setJob(String job);
    Employee getManager();
    void setManager(Employee manager);
    LocalDate getHiredate();
    void setHiredate(LocalDate hiredate);
    BigDecimal getSalary();
    void setSalary(BigDecimal salary);
    Double getCommission();
    void setCommission( Double commission);
    Department getDepartment();
    void setDepartment(Department department);
  }
  // end::employeeConstants[]

  // tag::constructor[]
  /** Initializes this domain model */
  public EmpDept() {
    super(DOMAIN);
    department();
    employee();
  }
  // end::constructor[]

  // tag::defineDepartment[]
  void department() {
    /*Defining the entity Department.TYPE*/
    define(Department.TYPE,
            primaryKeyProperty(Department.ID, "Department no.")
                    .updatable(true)
                    .nullable(false)
                    .beanProperty("id"),
            columnProperty(Department.NAME, "Department name")
                    .preferredColumnWidth(120)
                    .maximumLength(14)
                    .nullable(false)
                    .beanProperty("name"),
            columnProperty(Department.LOCATION, "Location")
                    .preferredColumnWidth(150)
                    .maximumLength(13)
                    .beanProperty("location"))
            .smallDataset(true)
            .orderByAscending(Department.NAME)
            .stringFactory(Department.NAME)
            .caption("Departments");
  }
  // end::defineDepartment[]

  // tag::defineEmployee[]
  void employee() {
    /*Defining the entity Employee.TYPE*/
    define(Employee.TYPE,
            primaryKeyProperty(Employee.ID, "Employee no.")
                    .beanProperty("id"),
            columnProperty(Employee.NAME, "Name")
                    .searchProperty(true)
                    .maximumLength(10)
                    .nullable(false)
                    .beanProperty("name"),
            columnProperty(Employee.DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(Employee.DEPARTMENT_FK, "Department")
                    .beanProperty("department"),
            itemProperty(Employee.JOB, "Job", Employee.JOB_VALUES)
                    .beanProperty("job"),
            columnProperty(Employee.SALARY, "Salary")
                    .nullable(false)
                    .valueRange(900, 10000)
                    .maximumFractionDigits(2)
                    .beanProperty("salary"),
            columnProperty(Employee.COMMISSION, "Commission")
                    .valueRange(100, 2000)
                    .maximumFractionDigits(2)
                    .beanProperty("commission"),
            columnProperty(Employee.MGR),
            foreignKeyProperty(Employee.MGR_FK, "Manager")
                    .beanProperty("manager"),
            columnProperty(Employee.HIREDATE, "Hiredate")
                    .nullable(false)
                    .beanProperty("hiredate")
                    .localeDateTimePattern(LocaleDateTimePattern.builder()
                            .delimiterDash()
                            .yearTwoDigits()
                            .build()),
            denormalizedViewProperty(Employee.DEPARTMENT_LOCATION, "Location",
                    Employee.DEPARTMENT_FK, Department.LOCATION)
                    .preferredColumnWidth(100))
            .keyGenerator(increment("scott.emp", Employee.ID.getName()))
            .orderByAscending(Employee.DEPARTMENT, Employee.NAME)
            .stringFactory(Employee.NAME)
            .caption("Employee")
            .backgroundColorProvider((entity, attribute) -> {
              if (attribute.equals(Employee.JOB) && "MANAGER".equals(entity.get(Employee.JOB))) {
                return Color.CYAN;
              }

              return null;
            })
            .foregroundColorProvider((entity, attribute) -> {
              if (attribute.equals(Employee.SALARY) && entity.get(Employee.SALARY).doubleValue() < 1300) {
                return Color.RED;
              }

              return null;
            });

    defineReport(Employee.EMPLOYEE_REPORT, classPathReport(EmpDept.class, "empdept_employees.jasper"));
  }
}
// end::defineEmployee[]
