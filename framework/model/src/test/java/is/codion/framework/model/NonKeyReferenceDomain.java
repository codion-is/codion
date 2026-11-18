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
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import static is.codion.framework.domain.DomainType.domainType;

/**
 * A foreign key referencing a column other than the primary key, over the employees tables: a department keyed by its
 * name, an employee referencing its number, the foreign key including only the location.
 */
final class NonKeyReferenceDomain extends DomainModel {

	static final DomainType DOMAIN = domainType(NonKeyReferenceDomain.class);

	interface Department {
		EntityType TYPE = DOMAIN.entityType("employees.department");

		Column<String> NAME = TYPE.stringColumn("dname");
		Column<Integer> NUMBER = TYPE.integerColumn("deptno");
		Column<String> LOCATION = TYPE.stringColumn("loc");
	}

	interface Employee {
		EntityType TYPE = DOMAIN.entityType("employees.employee");

		Column<Integer> ID = TYPE.integerColumn("empno");
		Column<String> NAME = TYPE.stringColumn("ename");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.NUMBER);
	}

	NonKeyReferenceDomain() {
		super(DOMAIN);
		add(Department.TYPE.as()
						.attributes(
										Department.NAME.as()
														.primaryKey()
														.searchable(true),
										Department.NUMBER.as()
														.column(),
										Department.LOCATION.as()
														.column())
						.build());
		add(Employee.TYPE.as()
						.attributes(
										Employee.ID.as()
														.primaryKey(),
										Employee.NAME.as()
														.column(),
										Employee.DEPARTMENT.as()
														.column(),
										Employee.DEPARTMENT_FK.as()
														.foreignKey()
														.include(Department.LOCATION))
						.build());
	}
}
