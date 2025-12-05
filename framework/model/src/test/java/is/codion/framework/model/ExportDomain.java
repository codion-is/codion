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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.time.LocalDate;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;
import static java.util.Arrays.asList;

public final class ExportDomain extends DomainModel {

	public static final DomainType DOMAIN = DomainType.domainType(ExportDomain.class);

	public ExportDomain() {
		super(DOMAIN);
		add(department(), employee());
	}

	public interface Department {
		EntityType TYPE = DOMAIN.entityType("employees.department");

		Column<Integer> ID = TYPE.integerColumn("deptno");
		Column<String> LOCATION = TYPE.stringColumn("loc");
		Column<String> NAME = TYPE.stringColumn("dname");
	}

	EntityDefinition department() {
		return Department.TYPE.as(
										Department.ID.as()
														.primaryKey()
														.caption("Id")
														.updatable(true)
														.nullable(false),
										Department.NAME.as()
														.column()
														.caption("Name")
														.searchable(true)
														.maximumLength(14)
														.nullable(false),
										Department.LOCATION.as()
														.column()
														.caption("Location")
														.maximumLength(13))
						.smallDataset(true)
						.orderBy(ascending(Department.NAME))
						.formatter(Department.NAME)
						.caption("Department")
						.build();
	}

	public interface Employee {
		EntityType TYPE = DOMAIN.entityType("employees.employee");

		Column<Integer> ID = TYPE.integerColumn("empno");
		Column<String> NAME = TYPE.stringColumn("ename");
		Column<String> JOB = TYPE.stringColumn("job");
		Column<Integer> MGR = TYPE.integerColumn("mgr");
		Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
		Column<Double> SALARY = TYPE.doubleColumn("sal");
		Column<Double> COMMISSION = TYPE.doubleColumn("comm");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
		Attribute<String> DEPARTMENT_LOCATION = TYPE.stringAttribute("location");
		Column<String> DATA = TYPE.stringColumn("data");

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
		ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
	}

	EntityDefinition employee() {
		return Employee.TYPE.as(
										Employee.ID.as()
														.primaryKey()
														.generator(sequence("employees.employee_seq"))
														.caption("Id"),
										Employee.NAME.as()
														.column()
														.caption("Name")
														.searchable(true)
														.maximumLength(10)
														.nullable(false),
										Employee.DEPARTMENT.as()
														.column()
														.nullable(false),
										Employee.DEPARTMENT_FK.as()
														.foreignKey()
														.caption("Department"),
										Employee.JOB.as()
														.column()
														.items(asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
														.caption("Job")
														.searchable(true),
										Employee.SALARY.as()
														.column()
														.caption("Salary")
														.nullable(false)
														.range(1000, 10000)
														.fractionDigits(2),
										Employee.COMMISSION.as()
														.column()
														.caption("Commission")
														.range(100, 2000)
														.fractionDigits(2),
										Employee.MGR.as()
														.column(),
										Employee.MGR_FK.as()
														.foreignKey()
														.caption("Manager"),
										Employee.HIREDATE.as()
														.column()
														.caption("Hiredate")
														.nullable(false),
										Employee.DEPARTMENT_LOCATION.as()
														.denormalized()
														.from(Employee.DEPARTMENT_FK)
														.using(Department.LOCATION)
														.caption("Location"),
										Employee.DATA.as()
														.column()
														.selected(false))
						.formatter(Employee.NAME)
						.caption("Employee")
						.build();
	}
}
