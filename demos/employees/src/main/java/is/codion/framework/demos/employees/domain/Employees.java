/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
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

  /** Entity type for the table scott.dept */
  public interface Department extends Entity {
    EntityType TYPE = DOMAIN.entityType("scott.dept", Department.class);

    /** Columns for the columns in the scott.dept table */
    Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");

    /** Bean getters and setters */
    Integer getDeptno();

    void setDeptno(Integer deptno);

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
    Column<Integer> ID = TYPE.integerColumn("id");
    Column<String> NAME = TYPE.stringColumn("ename");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Integer> MGR = TYPE.integerColumn("mgr");
    Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
    Column<BigDecimal> SALARY = TYPE.bigDecimalColumn("sal");
    Column<Double> COMMISSION = TYPE.doubleColumn("comm");
    Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");

    /** Foreign key attribute for the DEPTNO column in the table scott.emp */
    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.DEPTNO);
    /** Foreign key attribute for the MGR column in the table scott.emp */
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, Employee.ID);
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
            Department.DEPTNO.define()
                    .primaryKey()
                    .caption("Deptno.")
                    .nullable(false)
                    .beanProperty("deptno"),
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
                    .searchColumn(true)
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
            Employee.MGR.define()
                    .column(),
            Employee.MGR_FK.define()
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
            .keyGenerator(sequence("scott.emp_seq"))
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
