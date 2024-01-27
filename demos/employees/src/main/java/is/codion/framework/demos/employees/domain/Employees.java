/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.domain;

import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.plugin.jasperreports.JRReportType;
import is.codion.plugin.jasperreports.JasperReports;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.sequence;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.plugin.jasperreports.JasperReports.classPathReport;
import static java.util.Arrays.asList;

// tag::departmentConstants[]
/**
 * This class contains the specification for the Employees application domain model
 */
public final class Employees extends DefaultDomain {

  /** The domain type identifying this domain model */
  public static final DomainType DOMAIN = domainType(Employees.class);

  /** Entity type for the table employees.department */
  public interface Department extends Entity {
    EntityType TYPE = DOMAIN.entityType("employees.department", Department.class);

    /** Columns for the columns in the employees.department table */
    Column<Integer> DEPARTMENT_NO = TYPE.integerColumn("department_no");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<String> LOCATION = TYPE.stringColumn("location");

    /** Bean getters and setters */
    Integer getDepartmentNo();

    void setDepartmentNo(Integer departmentNo);

    String getName();

    void setName(String name);

    String getLocation();

    void setLocation(String location);
  }
  // end::departmentConstants[]

  // tag::employeeConstants[]
  /** Entity type for the table employees.employee */
  public interface Employee extends Entity {
    EntityType TYPE = DOMAIN.entityType("employees.employee", Employee.class);

    /** Columns for the columns in the employees.employee table */
    Column<Integer> ID = TYPE.integerColumn("id");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Integer> MANAGER_ID = TYPE.integerColumn("manager_id");
    Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
    Column<BigDecimal> SALARY = TYPE.bigDecimalColumn("salary");
    Column<Double> COMMISSION = TYPE.doubleColumn("commission");
    Column<Integer> DEPARTMENT = TYPE.integerColumn("department_no");

    /** Foreign key attribute for the DEPTNO column in the table employees.employee */
    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("department_no_fk", DEPARTMENT, Department.DEPARTMENT_NO);
    /** Foreign key attribute for the MGR column in the table employees.employee */
    ForeignKey MANAGER_FK = TYPE.foreignKey("manager_fk", MANAGER_ID, Employee.ID);
    /** Attribute for the denormalized department location property */
    Attribute<String> DEPARTMENT_LOCATION = TYPE.stringAttribute("location");

    JRReportType EMPLOYEE_REPORT = JasperReports.reportType("employee_report");

    List<Item<String>> JOB_VALUES = asList(
            item("Analyst"), item("Clerk"),
            item("Manager"), item("President"),
            item("Salesman"));

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

    void setCommission(Double commission);

    Department getDepartment();

    void setDepartment(Department department);
  }
  // end::employeeConstants[]

  // tag::constructor[]
  /** Initializes this domain model */
  public Employees() {
    super(DOMAIN);
    department();
    employee();
  }
  // end::constructor[]

  // tag::defineDepartment[]
  void department() {
    /*Defining the entity Department.TYPE*/
    add(Department.TYPE.define(
            Department.DEPARTMENT_NO.define()
                    .primaryKey()
                    .caption("No.")
                    .nullable(false)
                    .beanProperty("departmentNo"),
            Department.NAME.define()
                    .column()
                    .caption("Name")
                    .maximumLength(14)
                    .nullable(false)
                    .beanProperty("name"),
            Department.LOCATION.define()
                    .column()
                    .caption("Location")
                    .maximumLength(13)
                    .beanProperty("location"))
            .smallDataset(true)
            .orderBy(ascending(Department.NAME))
            .stringFactory(Department.NAME)
            .caption("Department"));
  }
  // end::defineDepartment[]

  // tag::defineEmployee[]
  void employee() {
    /*Defining the entity Employee.TYPE*/
    add(Employee.TYPE.define(
            Employee.ID.define()
                    .primaryKey()
                    .beanProperty("id"),
            Employee.NAME.define()
                    .column()
                    .caption("Name")
                    .searchable(true)
                    .maximumLength(10)
                    .nullable(false)
                    .beanProperty("name"),
            Employee.DEPARTMENT.define()
                    .column()
                    .nullable(false),
            Employee.DEPARTMENT_FK.define()
                    .foreignKey()
                    .caption("Department")
                    .beanProperty("department"),
            Employee.JOB.define()
                    .column()
                    .caption("Job")
                    .items(Employee.JOB_VALUES)
                    .beanProperty("job"),
            Employee.SALARY.define()
                    .column()
                    .caption("Salary")
                    .nullable(false)
                    .valueRange(900, 10000)
                    .maximumFractionDigits(2)
                    .beanProperty("salary"),
            Employee.COMMISSION.define()
                    .column()
                    .caption("Commission")
                    .valueRange(100, 2000)
                    .maximumFractionDigits(2)
                    .beanProperty("commission"),
            Employee.MANAGER_ID.define()
                    .column(),
            Employee.MANAGER_FK.define()
                    .foreignKey()
                    .caption("Manager")
                    .beanProperty("manager"),
            Employee.HIREDATE.define()
                    .column()
                    .caption("Hiredate")
                    .nullable(false)
                    .beanProperty("hiredate")
                    .localeDateTimePattern(LocaleDateTimePattern.builder()
                            .delimiterDash()
                            .yearFourDigits()
                            .build()),
            Employee.DEPARTMENT_LOCATION.define()
                    .denormalized(Employee.DEPARTMENT_FK, Department.LOCATION)
                    .caption("Location"))
            .keyGenerator(sequence("employees.employee_seq"))
            .orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
            .stringFactory(Employee.NAME)
            .caption("Employee")
            .backgroundColorProvider((entity, attribute) -> {
              if (attribute.equals(Employee.JOB) && "Manager".equals(entity.get(Employee.JOB))) {
                return Color.CYAN;
              }

              return null;
            })
            .foregroundColorProvider((entity, attribute) -> {
              if (attribute.equals(Employee.SALARY) && entity.get(Employee.SALARY).doubleValue() < 1300) {
                return Color.RED;
              }

              return null;
            }));

    add(Employee.EMPLOYEE_REPORT, classPathReport(Employees.class, "employees.jasper"));
  }
}
// end::defineEmployee[]
