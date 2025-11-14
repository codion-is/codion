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
package is.codion.framework.servlet;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.AbstractReport;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;
import static java.util.Arrays.asList;

public final class TestDomain extends DomainModel {

	static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

	public TestDomain() {
		super(DOMAIN);
		department();
		employee();
		operations();
		report();
	}

	public interface Department {
		EntityType TYPE = DOMAIN.entityType("employees.department");

		Column<Integer> ID = TYPE.integerColumn("deptno");
		Column<String> NAME = TYPE.stringColumn("dname");
		Column<String> LOCATION = TYPE.stringColumn("loc");
	}

	void department() {
		add(Department.TYPE.define(
										Department.ID.define()
														.primaryKey()
														.caption(Department.ID.name())
														.updatable(true)
														.nullable(false),
										Department.NAME.define()
														.column()
														.caption(Department.NAME.name())
														.searchable(true)
														.maximumLength(14)
														.nullable(false),
										Department.LOCATION.define()
														.column()
														.caption(Department.LOCATION.name())
														.maximumLength(13))
						.smallDataset(true)
						.orderBy(ascending(Department.NAME))
						.formatter(Department.NAME)
						.caption("Department")
						.build());
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
		Column<String> DEPARTMENT_LOCATION = TYPE.stringColumn("location");
		Column<byte[]> DATA = TYPE.byteArrayColumn("data");

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
		ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
	}

	void employee() {
		add(Employee.TYPE.define(
										Employee.ID.define()
														.primaryKey()
														.generator(sequence("employees.employee_seq"))
														.caption(Employee.ID.name()),
										Employee.NAME.define()
														.column()
														.caption(Employee.NAME.name())
														.searchable(true)
														.maximumLength(10)
														.nullable(false),
										Employee.DEPARTMENT.define()
														.column()
														.nullable(false),
										Employee.DEPARTMENT_FK.define()
														.foreignKey()
														.caption(Employee.DEPARTMENT_FK.name()),
										Employee.JOB.define()
														.column()
														.items(asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
														.caption(Employee.JOB.name())
														.searchable(true),
										Employee.SALARY.define()
														.column()
														.caption(Employee.SALARY.name())
														.nullable(false)
														.range(1000, 10000)
														.fractionDigits(2),
										Employee.COMMISSION.define()
														.column()
														.caption(Employee.COMMISSION.name())
														.range(100, 2000)
														.fractionDigits(2),
										Employee.MGR.define()
														.column(),
										Employee.MGR_FK.define()
														.foreignKey()
														.caption(Employee.MGR_FK.name()),
										Employee.HIREDATE.define()
														.column()
														.caption(Employee.HIREDATE.name())
														.nullable(false),
										Employee.DEPARTMENT_LOCATION.define()
														.denormalized()
														.from(Employee.DEPARTMENT_FK)
														.using(Department.LOCATION)
														.caption(Department.LOCATION.name()),
										Employee.DATA.define()
														.column()
														.caption("Data")
														.selected(false))
						.formatter(Employee.NAME)
						.orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
						.caption("Employee")
						.build());
	}

	public static final FunctionType<EntityConnection, List<String>, List<Integer>> FUNCTION = FunctionType.functionType("functionId");
	public static final ProcedureType<EntityConnection, List<String>> PROCEDURE = ProcedureType.procedureType("procedureId");

	void operations() {
		add(PROCEDURE, (connection, objects) -> {});
		add(FUNCTION, (connection, objects) -> asList(1, 2, 3));
	}

	public static final ReportType<Object, String, String> REPORT = ReportType.reportType("test");

	void report() {
		add(REPORT, new AbstractReport<Object, String, String>("report.path", false) {
			@Override
			public String fill(Connection connection, String parameters) {
				return parameters;
			}

			@Override
			public Object load() {
				return null;
			}
		});
	}
}
