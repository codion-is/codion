/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.domain;

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
 * This class contains the specification for the EmpDept application domain model
 */
public final class EmpDept extends DefaultDomain {

  /** The domain type identifying this domain model */
  public static final DomainType DOMAIN = domainType(EmpDept.class);

  /** Entity type for the table scott.dept */
  public interface Department extends Entity {
    EntityType TYPE = DOMAIN.entityType("scott.dept", Department.class);

    /** Columns for the columns in the scott.dept table */
    Column<Integer> ID = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");

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

  /** Entity type for the table scott.emp */
  public interface Employee extends Entity {
    EntityType TYPE = DOMAIN.entityType("scott.emp", Employee.class);

    /** Columns for the columns in the scott.emp table */
    Column<Integer> ID = TYPE.integerColumn("empno");
    Column<String> NAME = TYPE.stringColumn("ename");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Integer> MGR = TYPE.integerColumn("mgr");
    Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
    Column<BigDecimal> SALARY = TYPE.bigDecimalColumn("sal");
    Column<Double> COMMISSION = TYPE.doubleColumn("comm");
    Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");

    /** Foreign key attribute for the DEPTNO column in the table scott.emp */
    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
    /** Foreign key attribute for the MGR column in the table scott.emp */
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, Employee.ID);
    /** Attribute for the denormalized department location property */
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

    void setCommission(Double commission);

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
    add(Department.TYPE.define(
            Department.ID
                    .primaryKeyColumn()
                    .caption("Department no.")
                    .updatable(true)
                    .nullable(false)
                    .beanProperty("id"),
            Department.NAME
                    .column()
                    .caption("Department name")
                    .maximumLength(14)
                    .nullable(false)
                    .beanProperty("name"),
            Department.LOCATION
                    .column()
                    .caption("Location")
                    .maximumLength(13)
                    .beanProperty("location"))
            .smallDataset(true)
            .orderBy(ascending(Department.NAME))
            .stringFactory(Department.NAME)
            .caption("Departments"));
  }
  // end::defineDepartment[]

  // tag::defineEmployee[]
  void employee() {
    /*Defining the entity Employee.TYPE*/
    add(Employee.TYPE.define(
            Employee.ID
                    .primaryKeyColumn()
                    .caption("Employee no.")
                    .beanProperty("id"),
            Employee.NAME
                    .column()
                    .caption("Name")
                    .searchColumn(true)
                    .maximumLength(10)
                    .nullable(false)
                    .beanProperty("name"),
            Employee.DEPARTMENT
                    .column()
                    .nullable(false),
            Employee.DEPARTMENT_FK
                    .foreignKey()
                    .caption("Department")
                    .beanProperty("department"),
            Employee.JOB
                    .itemColumn(Employee.JOB_VALUES)
                    .caption("Job")
                    .beanProperty("job"),
            Employee.SALARY
                    .column()
                    .caption("Salary")
                    .nullable(false)
                    .valueRange(900, 10000)
                    .maximumFractionDigits(2)
                    .beanProperty("salary"),
            Employee.COMMISSION
                    .column()
                    .caption("Commission")
                    .valueRange(100, 2000)
                    .maximumFractionDigits(2)
                    .beanProperty("commission"),
            Employee.MGR
                    .column(),
            Employee.MGR_FK
                    .foreignKey()
                    .caption("Manager")
                    .beanProperty("manager"),
            Employee.HIREDATE
                    .column()
                    .caption("Hiredate")
                    .nullable(false)
                    .beanProperty("hiredate")
                    .localeDateTimePattern(LocaleDateTimePattern.builder()
                            .delimiterDash()
                            .yearTwoDigits()
                            .build()),
            Employee.DEPARTMENT_LOCATION
                    .denormalizedAttribute(Employee.DEPARTMENT_FK, Department.LOCATION)
                    .caption("Location"))
            .keyGenerator(sequence("scott.emp_seq"))
            .orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
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
            }));

    add(Employee.EMPLOYEE_REPORT, classPathReport(EmpDept.class, "empdept_employees.jasper"));
  }
}
// end::defineEmployee[]
