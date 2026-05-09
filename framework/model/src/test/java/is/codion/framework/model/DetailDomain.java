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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;

public final class DetailDomain extends DomainModel {

	public static final DomainType DOMAIN = domainType(DetailDomain.class);

	public DetailDomain() {
		super(DOMAIN);
		add(department(), employee(), departmentExtra());
	}

	public interface Department {
		EntityType TYPE = DOMAIN.entityType("employees.department");

		Column<Integer> ID = TYPE.integerColumn("deptno");
		Column<String> NAME = TYPE.stringColumn("dname");
	}

	EntityDefinition department() {
		return Department.TYPE.as(
										Department.ID.as()
														.primaryKey()
														.updatable(true)
														.nullable(false),
										Department.NAME.as()
														.column()
														.maximumLength(14)
														.nullable(false))
						.build();
	}

	public interface Employee {
		EntityType TYPE = DOMAIN.entityType("employees.employee");

		Column<Integer> ID = TYPE.integerColumn("empno");
		Column<String> NAME = TYPE.stringColumn("ename");
		Column<Double> SALARY = TYPE.doubleColumn("sal");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
		Column<Integer> MANAGER_ID = TYPE.integerColumn("mgr");

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
		ForeignKey MANAGER_FK = TYPE.foreignKey("manager_fk", MANAGER_ID, Employee.ID);
	}

	public interface DepartmentExtra {
		EntityType TYPE = DOMAIN.entityType("employees.department_extra");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_extra_dept_fk", DEPARTMENT, Department.ID);
	}

	EntityDefinition departmentExtra() {
		return DepartmentExtra.TYPE.as(
										DepartmentExtra.ID.as()
														.primaryKey()
														.generator(identity()),
										DepartmentExtra.DEPARTMENT.as()
														.column()
														.nullable(false),
										DepartmentExtra.DEPARTMENT_FK.as()
														.foreignKey(),
										DepartmentExtra.DESCRIPTION.as()
														.column())
						.build();
	}

	EntityDefinition employee() {
		return Employee.TYPE.as(
										Employee.ID.as()
														.primaryKey()
														.generator(sequence("employees.employee_seq")),
										Employee.NAME.as()
														.column()
														.nullable(false),
										Employee.DEPARTMENT.as()
														.column()
														.nullable(false),
										Employee.DEPARTMENT_FK.as()
														.foreignKey(),
										Employee.MANAGER_ID.as()
														.column(),
										Employee.MANAGER_FK.as()
														.foreignKey(),
										Employee.SALARY.as()
														.column()
														.nullable(false)
														.range(2000d, 10_000d))
						.build();
	}
}
