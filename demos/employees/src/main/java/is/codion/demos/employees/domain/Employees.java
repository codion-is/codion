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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.employees.domain;

import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.common.utilities.item.Item;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.plugin.jasperreports.JRReportType;
import is.codion.plugin.jasperreports.JasperReports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;
import static is.codion.plugin.jasperreports.JasperReports.classPathReport;

// tag::departmentConstants[]
// This class contains the specification for the Employees application domain model
public final class Employees extends DomainModel {

	// The domain type identifying this domain model
	public static final DomainType DOMAIN = domainType(Employees.class);

	// Entity type for the table employees.department
	public interface Department {
		EntityType TYPE = DOMAIN.entityType("employees.department");

		// Columns for the columns in the employees.department table
		Column<Integer> DEPARTMENT_NO = TYPE.integerColumn("department_no");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> LOCATION = TYPE.stringColumn("location");
	}
	// end::departmentConstants[]

	// tag::employeeConstants[]
	// Entity type for the table employees.employee
	public interface Employee {
		EntityType TYPE = DOMAIN.entityType("employees.employee");

		// Columns for the columns in the employees.employee table
		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<Integer> JOB = TYPE.integerColumn("job");
		Column<Integer> MANAGER_ID = TYPE.integerColumn("manager_id");
		Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
		Column<BigDecimal> SALARY = TYPE.bigDecimalColumn("salary");
		Column<Double> COMMISSION = TYPE.doubleColumn("commission");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("department_no");

		// Foreign key attribute for the DEPTNO column in the table employees.employee
		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("department_no_fk", DEPARTMENT, Department.DEPARTMENT_NO);
		// Foreign key attribute for the MGR column in the table employees.employee
		ForeignKey MANAGER_FK = TYPE.foreignKey("manager_fk", MANAGER_ID, Employee.ID);
		// Attribute for the denormalized department location property
		Attribute<String> DEPARTMENT_LOCATION = TYPE.stringAttribute("location");

		JRReportType EMPLOYEE_REPORT = JasperReports.reportType("employee_report");

		// Constants for the allowed JOB values
		int PRESIDENT = 1;
		int MANAGER = 2;
		int ANALYST = 3;
		int SALESMAN = 4;
		int CLERK = 5;

		List<Item<Integer>> JOB_ITEMS = List.of(
						item(ANALYST, "Analyst"), item(CLERK, "Clerk"),
						item(MANAGER, "Manager"), item(PRESIDENT, "President"),
						item(SALESMAN, "Salesman"));
	}
	// end::employeeConstants[]

	// tag::constructor[]
	// Initializes this domain model
	public Employees() {
		super(DOMAIN);
		add(department(), employee());
		add(Employee.EMPLOYEE_REPORT, classPathReport(Employees.class, "employees.jasper"));
	}
	// end::constructor[]

	// tag::defineDepartment[]
	EntityDefinition department() {
		// Defining the entity Department.TYPE
		return Department.TYPE.define(
										Department.DEPARTMENT_NO.define()
														.primaryKey()
														.caption("No.")
														.nullable(false),
										Department.NAME.define()
														.column()
														.caption("Name")
														.maximumLength(14)
														.searchable(true)
														.nullable(false),
										Department.LOCATION.define()
														.column()
														.caption("Location")
														.maximumLength(13))
						.smallDataset(true)
						.orderBy(ascending(Department.NAME))
						.formatter(Department.NAME)
						.caption("Department")
						.build();
	}
	// end::defineDepartment[]

	// tag::defineEmployee[]
	EntityDefinition employee() {
		// Defining the entity Employee.TYPE
		return Employee.TYPE.define(
										Employee.ID.define()
														.primaryKey()
														.generator(sequence("employees.employee_seq")),
										Employee.NAME.define()
														.column()
														.caption("Name")
														.searchable(true)
														.maximumLength(10)
														.nullable(false),
										Employee.DEPARTMENT.define()
														.column()
														.nullable(false),
										Employee.DEPARTMENT_FK.define()
														.foreignKey()
														.caption("Department"),
										Employee.JOB.define()
														.column()
														.caption("Job")
														.items(Employee.JOB_ITEMS)
														.nullable(false),
										Employee.SALARY.define()
														.column()
														.caption("Salary")
														.nullable(false)
														.range(900, 10000)
														.fractionDigits(2),
										Employee.COMMISSION.define()
														.column()
														.caption("Commission")
														.range(100, 2000)
														.fractionDigits(2),
										Employee.MANAGER_ID.define()
														.column(),
										Employee.MANAGER_FK.define()
														.foreignKey()
														.caption("Manager"),
										Employee.HIREDATE.define()
														.column()
														.caption("Hiredate")
														.nullable(false)
														.dateTimePattern(LocaleDateTimePattern.builder()
																		.delimiterDash()
																		.yearFourDigits()
																		.build()),
										Employee.DEPARTMENT_LOCATION.define()
														.denormalized()
														.from(Employee.DEPARTMENT_FK)
														.attribute(Department.LOCATION)
														.caption("Location"))
						.orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
						.formatter(Employee.NAME)
						.caption("Employee")
						.build();
	}
}
// end::defineEmployee[]
