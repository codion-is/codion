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
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.AbstractReport;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Generator;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ConditionType;
import is.codion.framework.domain.entity.query.EntitySelectQuery;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;
import static java.util.Arrays.asList;

public final class TestDomain extends DomainModel {

	static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

	public static final ReportType<Object, String, Map<String, Object>> REPORT = ReportType.reportType("report");

	public static final ProcedureType<EntityConnection, Object> PROCEDURE_ID = ProcedureType.procedureType("procedureId");
	public static final FunctionType<EntityConnection, Object, List<Object>> FUNCTION_ID = FunctionType.functionType("functionId");

	public TestDomain() {
		super(DOMAIN);
		department();
		employee();
		departmentFk();
		employeeFk();
		uuidTestDefaultValue();
		uuidTestNoDefaultValue();
		operations();
		empnoDeptno();
		job();
		noPkEntity();
		Report.REPORT_PATH.set("path/to/reports");
		add(REPORT, new AbstractReport<Object, String, Map<String, Object>>("report.path", false) {
			@Override
			public String fill(Connection connection, Map<String, Object> parameters) {
				return "result";
			}

			@Override
			public Object load() {
				return null;
			}
		});
		query();
		queryColumnsWhereClause();
		queryFromClause();
		queryFromWhereClause();
		master();
		detail();
		masterFk();
		detailFk();
		employeeNonOpt();
		nullConverter();
		generatorTestWithPk();
		generatorTestWithoutPk();
		generatorNonPk();
		noPkIdentical();
		mixedGenerated();
		partialGeneratedPk();
		queryWithCte();
		queryWithRecursiveCte();
		queryWithMultipleCtes();
	}

	public interface Department {
		EntityType TYPE = DOMAIN.entityType("employees.department");

		Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
		Column<String> DNAME = TYPE.stringColumn("dname");
		Column<String> LOC = TYPE.stringColumn("loc");
		Attribute<Boolean> ACTIVE = TYPE.booleanAttribute("active");
		Attribute<byte[]> DATA = TYPE.byteArrayAttribute("data");

		ConditionType DEPARTMENT_CONDITION_TYPE = TYPE.conditionType("condition");
		ConditionType DEPARTMENT_CONDITION_SALES_TYPE = TYPE.conditionType("conditionSalesId");
		ConditionType DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE = TYPE.conditionType("conditionInvalidColumnId");
	}

	void department() {
		add(Department.TYPE.define(
										Department.DEPTNO.define()
														.primaryKey()
														.caption(Department.DEPTNO.name())
														.updatable(true)
														.nullable(false),
										Department.DNAME.define()
														.column()
														.caption(Department.DNAME.name())
														.searchable(true)
														.maximumLength(14)
														.nullable(false),
										Department.LOC.define()
														.column()
														.caption(Department.LOC.name())
														.maximumLength(13),
										Department.ACTIVE.define()
														.attribute(),
										Department.DATA.define()
														.attribute())
						.smallDataset(true)
						.formatter(Department.DNAME)
						.condition(Department.DEPARTMENT_CONDITION_TYPE, (attributes, values) -> {
							StringBuilder builder = new StringBuilder("deptno in (");
							values.forEach(value -> builder.append("?,"));
							builder.deleteCharAt(builder.length() - 1);

							return builder.append(")").toString();
						})
						.condition(Department.DEPARTMENT_CONDITION_SALES_TYPE, (attributes, values) -> "dname = 'SALES'")
						.condition(Department.DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE, (attributes, values) -> "no_column is null")
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
		Column<OffsetDateTime> HIRETIME = TYPE.offsetDateTimeColumn("hiretime");
		Column<Double> SALARY = TYPE.doubleColumn("sal");
		Column<Double> COMMISSION = TYPE.doubleColumn("comm");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
		Column<String> DEPARTMENT_LOCATION = TYPE.stringColumn("location");
		Column<byte[]> DATA_LAZY = TYPE.byteArrayColumn("data_lazy");
		Column<byte[]> DATA = TYPE.byteArrayColumn("data");

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.DEPTNO);
		ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);

		ConditionType NAME_IS_BLAKE_CONDITION = TYPE.conditionType("condition1Id");
		ConditionType MGR_GREATER_THAN_CONDITION = TYPE.conditionType("condition2Id");
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
														.searchable(true).maximumLength(10).nullable(false),
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
														//not really soft, just for testing purposes
														.soft(true)
														.caption(Employee.MGR_FK.name()),
										Employee.HIREDATE.define()
														.column()
														.caption(Employee.HIREDATE.name())
														.nullable(false),
										Employee.HIRETIME.define()
														.column()
														.caption(Employee.HIRETIME.name()),
										Employee.DEPARTMENT_LOCATION.define()
														.denormalized()
														.from(Employee.DEPARTMENT_FK)
														.using(Department.LOC)
														.caption(Department.LOC.name()),
										Employee.DATA_LAZY.define()
														.column()
														.selected(false),
										Employee.DATA.define()
														.column())
						.formatter(Employee.NAME)
						.condition(Employee.NAME_IS_BLAKE_CONDITION, (attributes, values) -> "ename = 'BLAKE'")
						.condition(Employee.MGR_GREATER_THAN_CONDITION, (attributes, values) -> "mgr > ?")
						.caption("Employee")
						.build());
	}

	public interface DepartmentFk {
		EntityType TYPE = DOMAIN.entityType("employees.departmentfk");

		Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
		Column<String> DNAME = TYPE.stringColumn("dname");
		Column<String> LOC = TYPE.stringColumn("loc");
	}

	void departmentFk() {
		add(DepartmentFk.TYPE.define(
										DepartmentFk.DEPTNO.define()
														.primaryKey()
														.caption(Department.DEPTNO.name()),
										DepartmentFk.DNAME.define()
														.column()
														.caption(DepartmentFk.DNAME.name()),
										DepartmentFk.LOC.define()
														.column()
														.caption(DepartmentFk.LOC.name()))
						.table("employees.department")
						.formatter(DepartmentFk.DNAME)
						.build());
	}

	public interface EmployeeFk {
		EntityType TYPE = DOMAIN.entityType("employees.employeefk");

		Column<Integer> ID = TYPE.integerColumn("empno");
		Column<String> NAME = TYPE.stringColumn("ename");
		Column<String> JOB = TYPE.stringColumn("job");
		Column<Integer> MGR = TYPE.integerColumn("mgr");
		Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
		Column<OffsetDateTime> HIRETIME = TYPE.offsetDateTimeColumn("hiretime");
		Column<Double> SALARY = TYPE.doubleColumn("sal");
		Column<Double> COMMISSION = TYPE.doubleColumn("comm");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, DepartmentFk.DEPTNO);
		ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
	}

	void employeeFk() {
		add(EmployeeFk.TYPE.define(
										EmployeeFk.ID.define()
														.primaryKey()
														.generator(sequence("employees.employee_seq"))
														.caption(EmployeeFk.ID.name()),
										EmployeeFk.NAME.define()
														.column()
														.caption(EmployeeFk.NAME.name())
														.nullable(false),
										EmployeeFk.DEPARTMENT.define()
														.column()
														.nullable(false),
										EmployeeFk.DEPARTMENT_FK.define()
														.foreignKey()
														.caption(EmployeeFk.DEPARTMENT_FK.name())
														.include(DepartmentFk.DNAME),
										EmployeeFk.JOB.define()
														.column()
														.caption(EmployeeFk.JOB.name()),
										EmployeeFk.SALARY.define()
														.column()
														.caption(EmployeeFk.SALARY.name())
														.fractionDigits(2),
										EmployeeFk.COMMISSION.define()
														.column()
														.caption(EmployeeFk.COMMISSION.name()),
										EmployeeFk.MGR.define()
														.column(),
										EmployeeFk.MGR_FK.define()
														.foreignKey()
														.soft(true)
														.caption(EmployeeFk.MGR_FK.name())
														.include(EmployeeFk.NAME, EmployeeFk.JOB, EmployeeFk.DEPARTMENT_FK),
										EmployeeFk.HIREDATE.define()
														.column()
														.caption(EmployeeFk.HIREDATE.name())
														.nullable(false),
										EmployeeFk.HIRETIME.define()
														.column()
														.caption(EmployeeFk.HIRETIME.name()))
						.table("employees.employee")
						.formatter(EmployeeFk.NAME)
						.caption("Employee")
						.build());
	}

	public interface UUIDTestDefault {
		EntityType TYPE = DOMAIN.entityType("employees.uuid_test_default");

		Column<UUID> ID = TYPE.column("id", UUID.class);
		Column<String> DATA = TYPE.stringColumn("data");
	}

	private void uuidTestDefaultValue() {
		Generator<UUID> uuidGenerator = new Generator<UUID>() {
			@Override
			public void afterInsert(Entity entity, Column<UUID> column, Database database, Statement statement) throws SQLException {
				try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						entity.set(column, (UUID) generatedKeys.getObject(1));
					}
				}
			}

			@Override
			public boolean generatedKeys() {
				return true;
			}
		};
		add(UUIDTestDefault.TYPE.define(
										UUIDTestDefault.ID.define()
														.primaryKey()
														.caption("Id")
														.generator(uuidGenerator),
										UUIDTestDefault.DATA.define()
														.column()
														.caption("Data"))
						.build());
	}

	public interface UUIDTestNoDefault {
		EntityType TYPE = DOMAIN.entityType("employees.uuid_test_no_default");

		Column<UUID> ID = TYPE.column("id", UUID.class);
		Column<String> DATA = TYPE.stringColumn("data");
	}

	private void uuidTestNoDefaultValue() {
		Generator<UUID> uuidGenerator = new Generator<>() {
			@Override
			public void beforeInsert(Entity entity, Column<UUID> column, Database database, Connection connection) {
				entity.set(column, UUID.randomUUID());
			}
		};
		add(UUIDTestNoDefault.TYPE.define(
										UUIDTestNoDefault.ID.define()
														.primaryKey()
														.generator(uuidGenerator)
														.caption("Id"),
										UUIDTestNoDefault.DATA.define()
														.column()
														.caption("Data"))
						.build());
	}

	private void operations() {
		add(PROCEDURE_ID, (connection, parameter) -> {});
		add(FUNCTION_ID, (connection, parameter) -> null);
	}

	public interface Job {
		EntityType TYPE = DOMAIN.entityType("job");

		Column<String> JOB = TYPE.stringColumn("job");
		Column<Double> MAX_SALARY = TYPE.doubleColumn("max_salary");
		Column<Double> MIN_SALARY = TYPE.doubleColumn("min_salary");
		Column<Double> MAX_COMMISSION = TYPE.doubleColumn("max_commission");
		Column<Double> MIN_COMMISSION = TYPE.doubleColumn("min_commission");
	}

	private void job() {
		add(Job.TYPE.define(
										Job.JOB.define()
														.primaryKey()
														.groupBy(true),
										Job.MAX_SALARY.define()
														.column()
														.expression("max(sal)")
														.aggregate(true),
										Job.MIN_SALARY.define()
														.column()
														.expression("min(sal)")
														.aggregate(true),
										Job.MAX_COMMISSION.define()
														.column()
														.expression("max(comm)")
														.aggregate(true),
										Job.MIN_COMMISSION.define()
														.column()
														.expression("min(comm)")
														.aggregate(true))
						.table("employees.employee")
						.selectQuery(EntitySelectQuery.builder()
										.having("job <> 'PRESIDENT'")
										.build())
						.build());
	}

	public interface NoPrimaryKey {
		EntityType TYPE = DOMAIN.entityType("employees.no_pk_table");

		Column<Integer> COL_4 = TYPE.integerColumn("col4");
		Column<String> COL_3 = TYPE.stringColumn("col3");
		Column<String> COL_2 = TYPE.stringColumn("col2");
		Column<Integer> COL_1 = TYPE.integerColumn("col1");
	}

	private void noPkEntity() {
		add(NoPrimaryKey.TYPE.define(
										NoPrimaryKey.COL_1.define()
														.column(),
										NoPrimaryKey.COL_2.define()
														.column(),
										NoPrimaryKey.COL_3.define()
														.column(),
										NoPrimaryKey.COL_4.define()
														.column())
						.build());
	}

	public interface EmpnoDeptno {
		EntityType TYPE = DOMAIN.entityType("joinedQueryEntityType");

		Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
		Column<String> DEPARTMENT_NAME = TYPE.stringColumn("dname");
		Column<Integer> EMPNO = TYPE.integerColumn("empno");
		Column<String> EMPLOYEE_NAME = TYPE.stringColumn("ename");

		ConditionType CONDITION = EmpnoDeptno.TYPE.conditionType("condition");
	}

	private void empnoDeptno() {
		add(EmpnoDeptno.TYPE.define(
										EmpnoDeptno.EMPLOYEE_NAME.define()
														.column(),
										EmpnoDeptno.DEPTNO.define()
														.column()
														.expression("d.deptno"),
										EmpnoDeptno.EMPNO.define()
														.primaryKey(),
										EmpnoDeptno.DEPARTMENT_NAME.define()
														.column())
						.selectQuery(EntitySelectQuery.builder()
										.columns("empno as empno, dname, d.deptno as deptno, ename")// different order than attribute definitions
										.from("employees.employee e, employees.department d")
										.where("e.deptno = d.deptno")
										.orderBy("e.deptno, e.ename")
										.build())
						.condition(EmpnoDeptno.CONDITION, (attributes, values) -> "d.deptno = 10")
						.build());
	}

	public interface Query {
		EntityType TYPE = DOMAIN.entityType("query");

		Column<Integer> EMPNO = TYPE.integerColumn("empno");
		Column<String> ENAME = TYPE.stringColumn("ename");
	}

	private void query() {
		add(Query.TYPE.define(
										Query.EMPNO.define()
														.column(),
										Query.ENAME.define()
														.column())
						.table("employees.employee")
						.orderBy(OrderBy.descending(Query.ENAME))
						.selectTable("employees.employee e")
						.selectQuery(EntitySelectQuery.builder()
										.columns("empno, ename")
										.orderBy("ename")
										.build())
						.build());
	}

	public interface QueryColumnsWhereClause {
		EntityType TYPE = DOMAIN.entityType("query_where");

		Column<Integer> EMPNO = TYPE.integerColumn("empno");
		Column<String> ENAME = TYPE.stringColumn("ename");
	}

	private void queryColumnsWhereClause() {
		add(QueryColumnsWhereClause.TYPE.define(
										QueryColumnsWhereClause.EMPNO.define()
														.column(),
										QueryColumnsWhereClause.ENAME.define()
														.column())
						.table("employees.employee e")
						.orderBy(OrderBy.descending(QueryColumnsWhereClause.ENAME))
						.selectQuery(EntitySelectQuery.builder()
										.columns("e.empno, e.ename")
										.where("e.deptno > 10")
										.build())
						.build());
	}

	public interface QueryFromClause {
		EntityType TYPE = DOMAIN.entityType("query_from");

		Column<Integer> EMPNO = TYPE.integerColumn("empno");
		Column<String> ENAME = TYPE.stringColumn("ename");
	}

	private void queryFromClause() {
		add(QueryFromClause.TYPE.define(
										QueryFromClause.EMPNO.define()
														.column(),
										QueryFromClause.ENAME.define()
														.column())
						.orderBy(OrderBy.descending(QueryFromClause.ENAME))
						.selectQuery(EntitySelectQuery.builder()
										.from("employees.employee")
										.orderBy("ename")
										.build())
						.build());
	}

	public interface QueryFromWhereClause {
		EntityType TYPE = DOMAIN.entityType("query_from_where");

		Column<Integer> EMPNO = TYPE.integerColumn("empno");
		Column<String> ENAME = TYPE.stringColumn("ename");
	}

	private void queryFromWhereClause() {
		add(QueryFromWhereClause.TYPE.define(
										QueryFromWhereClause.EMPNO.define()
														.column(),
										QueryFromWhereClause.ENAME.define()
														.column())
						.orderBy(OrderBy.descending(QueryFromWhereClause.ENAME))
						.selectQuery(EntitySelectQuery.builder()
										.from("employees.employee")
										.where("deptno > 10")
										.orderBy("deptno")
										.build())
						.build());
	}

	public interface Master {
		EntityType TYPE = DOMAIN.entityType("employees.master");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> DATA = TYPE.stringColumn("data");
	}

	void master() {
		add(Master.TYPE.define(
										Master.ID.define()
														.primaryKey()
														.generator(identity()),
										Master.DATA.define()
														.column())
						.build());
	}

	public interface Detail {
		EntityType TYPE = DOMAIN.entityType("employees.detail");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> MASTER_1_ID = TYPE.integerColumn("master_1_id");
		Column<Integer> MASTER_2_ID = TYPE.integerColumn("master_2_id");

		ForeignKey MASTER_1_FK = TYPE.foreignKey("master_1_fk", MASTER_1_ID, Master.ID);
		ForeignKey MASTER_2_FK = TYPE.foreignKey("master_2_fk", MASTER_2_ID, Master.ID);
	}

	void detail() {
		add(Detail.TYPE.define(
										Detail.ID.define()
														.primaryKey()
														.generator(identity()),
										Detail.MASTER_1_ID.define()
														.column(),
										Detail.MASTER_1_FK.define()
														.foreignKey(),
										Detail.MASTER_2_ID.define()
														.column(),
										Detail.MASTER_2_FK.define()
														.foreignKey())
						.build());
	}

	public interface MasterFk {
		EntityType TYPE = DOMAIN.entityType("employees.master_fk");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
	}

	void masterFk() {
		add(MasterFk.TYPE.define(
										MasterFk.ID.define()
														.primaryKey(),
										MasterFk.NAME.define()
														.column())
						.build());
	}

	public interface DetailFk {
		EntityType TYPE = DOMAIN.entityType("employees.detail_fk");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> MASTER_NAME = TYPE.stringColumn("master_name");

		ForeignKey MASTER_FK = TYPE.foreignKey("master_fk", MASTER_NAME, MasterFk.NAME);
	}

	void detailFk() {
		add(DetailFk.TYPE.define(
										DetailFk.ID.define()
														.primaryKey(),
										DetailFk.MASTER_NAME.define()
														.column(),
										DetailFk.MASTER_FK.define()
														.foreignKey())
						.build());
	}

	public interface EmployeeNonOpt {
		EntityType TYPE = DOMAIN.entityType("empnonopt");

		Column<Integer> ID = TYPE.integerColumn("empno");
		Column<String> NAME = TYPE.stringColumn("ename");
	}

	void employeeNonOpt() {
		add(EmployeeNonOpt.TYPE.define(
										EmployeeNonOpt.ID.define()
														.primaryKey(),
										EmployeeNonOpt.NAME.define()
														.column())
						.table("employees.employee")
						.optimisticLocking(false)
						.build());
	}

	public interface NullConverter {
		EntityType TYPE = DOMAIN.entityType("null_converter");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
	}

	void nullConverter() {
		class NullColumnConverter implements Column.Converter<String, String> {
			@Override
			public boolean handlesNull() {
				return true;
			}

			@Override
			public String toColumnValue(String value, Statement statement) throws SQLException {
				if (value == null) {
					return "null";
				}

				return value;
			}

			@Override
			public String fromColumnValue(String columnValue) throws SQLException {
				if ("null".equals(columnValue)) {
					return null;
				}

				return columnValue;
			}
		}
		add(NullConverter.TYPE.define(
										NullConverter.ID.define()
														.primaryKey(),
										NullConverter.NAME.define()
														.column()
														.converter(String.class, new NullColumnConverter()))
						.table("employees.master_fk")
						.build());
	}

	public interface GeneratorTestWithPk {
		EntityType TYPE = DOMAIN.entityType("employees.generator");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> SEQ = TYPE.integerColumn("seq");
		Column<UUID> UUID = TYPE.column("uuid", UUID.class);
		Column<String> DATA = TYPE.stringColumn("data");
	}

	void generatorTestWithPk() {
		add(GeneratorTestWithPk.TYPE.define(
										GeneratorTestWithPk.ID.define()
														.primaryKey(0)
														.generator(identity()),
										GeneratorTestWithPk.SEQ.define()
														.primaryKey(1)
														.generator(sequence("employees.generator_test_seq")),
										GeneratorTestWithPk.UUID.define()
														.primaryKey(2)
														.generator(new Generator<UUID>() {
															@Override
															public void beforeInsert(Entity entity, Column<UUID> column, Database database, Connection connection) throws SQLException {
																entity.set(column, UUID.randomUUID());
															}
														}),
										GeneratorTestWithPk.DATA.define()
														.column()
														.maximumLength(10))
						.build());
	}

	public interface GeneratorTestWithoutPk {
		EntityType TYPE = DOMAIN.entityType("employees.generator_without_pk");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> SEQ = TYPE.integerColumn("seq");
		Column<UUID> UUID = TYPE.column("uuid", UUID.class);
		Column<String> DATA = TYPE.stringColumn("data");
	}

	void generatorTestWithoutPk() {
		add(GeneratorTestWithoutPk.TYPE.define(
										GeneratorTestWithoutPk.ID.define()
														.column()
														.generator(identity()),
										GeneratorTestWithoutPk.SEQ.define()
														.column()
														.generator(sequence("employees.generator_test_seq")),
										GeneratorTestWithoutPk.UUID.define()
														.column()
														.generator(new Generator<UUID>() {
															@Override
															public void beforeInsert(Entity entity, Column<UUID> column, Database database, Connection connection) throws SQLException {
																entity.set(column, UUID.randomUUID());
															}
														}),
										GeneratorTestWithoutPk.DATA.define()
														.column()
														.maximumLength(10))
						.table("employees.generator")
						.build());
	}

	public interface GeneratorNonPk {
		EntityType TYPE = DOMAIN.entityType("employees.generator_non_pk");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> GENERATED_COL = TYPE.integerColumn("generated_col");
		Column<String> DATA = TYPE.stringColumn("data");
	}

	void generatorNonPk() {
		add(GeneratorNonPk.TYPE.define(
										GeneratorNonPk.ID.define()
														.primaryKey(),
										GeneratorNonPk.GENERATED_COL.define()
														.column()
														.generator(identity()),
										GeneratorNonPk.DATA.define()
														.column()
														.maximumLength(10))
						.build());
	}

	public interface NoPkIdentical {
		EntityType TYPE = DOMAIN.entityType("employees.no_pk_identical");

		Column<String> DATA = TYPE.stringColumn("data");
	}

	void noPkIdentical() {
		add(NoPkIdentical.TYPE.define(
										NoPkIdentical.DATA.define()
														.column()
														.maximumLength(10))
						.build());
	}

	public interface MixedGenerated {
		EntityType TYPE = DOMAIN.entityType("employees.mixed_generated");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> MANUAL_PK = TYPE.integerColumn("manual_pk");
		Column<Integer> GENERATED_COL = TYPE.integerColumn("generated_col");
		Column<String> DATA = TYPE.stringColumn("data");
	}

	void mixedGenerated() {
		add(MixedGenerated.TYPE.define(
										MixedGenerated.ID.define()
														.primaryKey(0)
														.generator(identity()),
										MixedGenerated.MANUAL_PK.define()
														.primaryKey(1),
										MixedGenerated.GENERATED_COL.define()
														.column()
														.generator(sequence("employees.mixed_seq")),
										MixedGenerated.DATA.define()
														.column()
														.maximumLength(10))
						.build());
	}

	public interface PartialGeneratedPk {
		EntityType TYPE = DOMAIN.entityType("employees.partial_generated_pk");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> MANUAL_ID = TYPE.integerColumn("manual_id");
		Column<String> DATA = TYPE.stringColumn("data");
	}

	void partialGeneratedPk() {
		add(PartialGeneratedPk.TYPE.define(
										PartialGeneratedPk.ID.define()
														.primaryKey(0)
														.generator(identity()),
										PartialGeneratedPk.MANUAL_ID.define()
														.primaryKey(1),
										PartialGeneratedPk.DATA.define()
														.column()
														.maximumLength(10))
						.build());
	}

	public interface QueryWithCte {
		EntityType TYPE = DOMAIN.entityType("query_with_cte");

		Column<Integer> EMPNO = TYPE.integerColumn("empno");
		Column<String> ENAME = TYPE.stringColumn("ename");
		Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
	}

	private void queryWithCte() {
		add(QueryWithCte.TYPE.define(
										QueryWithCte.EMPNO.define()
														.column(),
										QueryWithCte.ENAME.define()
														.column(),
										QueryWithCte.DEPTNO.define()
														.column())
						.table("employees.employee")
						.selectQuery(EntitySelectQuery.builder()
										.with("high_earners")
										.as("SELECT empno, ename, deptno FROM employees.employee WHERE sal > 2000")
										.from("high_earners")
										.build())
						.build());
	}

	public interface QueryWithRecursiveCte {
		EntityType TYPE = DOMAIN.entityType("query_with_recursive_cte");

		Column<Integer> EMPNO = TYPE.integerColumn("empno");
		Column<String> ENAME = TYPE.stringColumn("ename");
		Column<Integer> MGR = TYPE.integerColumn("mgr");
		Column<Integer> LEVEL = TYPE.integerColumn("level");
	}

	private void queryWithRecursiveCte() {
		add(QueryWithRecursiveCte.TYPE.define(
										QueryWithRecursiveCte.EMPNO.define()
														.column(),
										QueryWithRecursiveCte.ENAME.define()
														.column(),
										QueryWithRecursiveCte.MGR.define()
														.column(),
										QueryWithRecursiveCte.LEVEL.define()
														.column())
						.table("employees.employee")
						.selectQuery(EntitySelectQuery.builder()
										.with("emp_hierarchy (empno, ename, mgr, level)")
										.as("""
														SELECT empno, ename, mgr, 1 as level
														FROM employees.employee
														WHERE mgr IS NULL
														UNION ALL
														SELECT e.empno, e.ename, e.mgr, eh.level + 1
														FROM employees.employee e
														JOIN emp_hierarchy eh ON e.mgr = eh.empno""")
										.recursive()
										.from("emp_hierarchy")
										.build())
						.build());
	}

	public interface QueryWithMultipleCtes {
		EntityType TYPE = DOMAIN.entityType("query_with_multiple_ctes");

		Column<Integer> EMPNO = TYPE.integerColumn("empno");
		Column<String> ENAME = TYPE.stringColumn("ename");
		Column<String> DNAME = TYPE.stringColumn("dname");
	}

	private void queryWithMultipleCtes() {
		add(QueryWithMultipleCtes.TYPE.define(
										QueryWithMultipleCtes.EMPNO.define()
														.column(),
										QueryWithMultipleCtes.ENAME.define()
														.column(),
										QueryWithMultipleCtes.DNAME.define()
														.column())
						.table("employees.employee")
						.selectQuery(EntitySelectQuery.builder()
										.with("high_earners")
										.as("SELECT empno, ename, deptno FROM employees.employee WHERE sal > 2000")
										.with("selected_depts")
										.as("SELECT deptno, dname FROM employees.department WHERE deptno IN (10, 20)")
										.from("high_earners he JOIN selected_depts sd ON he.deptno = sd.deptno")
										.build())
						.build());
	}
}
