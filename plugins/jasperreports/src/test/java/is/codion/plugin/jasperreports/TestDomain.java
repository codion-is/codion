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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jasperreports;

import is.codion.common.db.report.ReportType;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import net.sf.jasperreports.engine.JasperPrint;

import java.time.LocalDate;
import java.util.Map;

import static is.codion.common.db.report.ReportType.reportType;
import static is.codion.common.utilities.item.Item.item;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;
import static is.codion.plugin.jasperreports.JasperReports.*;
import static java.util.Arrays.asList;

public final class TestDomain extends DomainModel {

	static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

	public TestDomain() {
		super(DOMAIN);
		department();
		employee();
		add(Employee.PDF_REPORT, export(Employee.CLASS_PATH_REPORT, JRExport.PDF));
		add(Employee.SERIALIZED_REPORT, export(Employee.CLASS_PATH_REPORT, JRExport.SERIALIZED));
	}

	public interface Department {
		EntityType TYPE = DOMAIN.entityType("employees.department");

		Column<String> LOCATION = TYPE.stringColumn("loc");
		Column<String> NAME = TYPE.stringColumn("dname");
		Column<Integer> ID = TYPE.integerColumn("deptno");
	}

	void department() {
		add(Department.TYPE.as()
						.attributes(
										Department.ID.as()
														.primaryKey()
														.caption(Department.ID.name())
														.updatable(true).nullable(false),
										Department.NAME.as()
														.column()
														.caption(Department.NAME.name())
														.searchable(true)
														.maximumLength(14)
														.nullable(false),
										Department.LOCATION.as()
														.column()
														.caption(Department.LOCATION.name())
														.maximumLength(13))
						.smallDataset(true)
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

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
		ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);

		JRReport<JasperPrint> FILE_REPORT =
						fileReport("/employees.jasper", true);
		JRReport<JasperPrint> CLASS_PATH_REPORT =
						classPathReport(TestDomain.class, "/employees.jasper");

		//names a report and a byte[], nothing of JasperReports
		ReportType<Map<String, Object>, byte[]> PDF_REPORT = reportType("employee_pdf");
		//the filled report itself, carried as bytes, for a client which reconstructs it with the engine
		ReportType<Map<String, Object>, byte[]> SERIALIZED_REPORT = reportType("employee_serialized");
	}

	void employee() {
		add(Employee.TYPE.as()
						.attributes(
										Employee.ID.as()
														.primaryKey()
														.generator(sequence("employees.employee_seq"))
														.caption(Employee.ID.name()),
										Employee.NAME.as()
														.column()
														.caption(Employee.NAME.name())
														.searchable(true).maximumLength(10).nullable(false),
										Employee.DEPARTMENT.as()
														.column()
														.nullable(false),
										Employee.DEPARTMENT_FK.as()
														.foreignKey()
														.caption(Employee.DEPARTMENT_FK.name()),
										Employee.JOB.as()
														.column()
														.items(asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
														.caption(Employee.JOB.name())
														.searchable(true),
										Employee.SALARY.as()
														.column()
														.caption(Employee.SALARY.name())
														.nullable(false)
														.range(1000, 10000)
														.fractionDigits(2),
										Employee.COMMISSION.as()
														.column()
														.caption(Employee.COMMISSION.name())
														.range(100, 2000)
														.fractionDigits(2),
										Employee.MGR.as()
														.column(),
										Employee.MGR_FK.as()
														.foreignKey()
														.caption(Employee.MGR_FK.name()),
										Employee.HIREDATE.as()
														.column()
														.caption(Employee.HIREDATE.name())
														.nullable(false),
										Employee.DEPARTMENT_LOCATION.as()
														.denormalized()
														.from(Employee.DEPARTMENT_FK)
														.using(Department.LOCATION)
														.caption(Department.LOCATION.name()))
						.formatter(Employee.NAME)
						.caption("Employee")
						.build());
	}
}
