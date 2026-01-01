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
package is.codion.framework.model.test;

import is.codion.common.utilities.item.Item;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ConditionType;
import is.codion.framework.domain.entity.query.EntitySelectQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;
import static java.util.Arrays.asList;

public final class TestDomain extends DomainModel {

	public static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

	public TestDomain() {
		super(DOMAIN);
		add(master(), detail(), department(), employee(), enumEntity(), derived(), job(), dateTimeTest(), nongen());
	}

	public interface Master {
		EntityType TYPE = DOMAIN.entityType("domain.master_entity");

		Column<Long> ID = TYPE.longColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<Integer> CODE = TYPE.integerColumn("code");
	}

	EntityDefinition master() {
		return Master.TYPE.as(
										Master.ID.as()
														.primaryKey(),
										Master.NAME.as()
														.column(),
										Master.CODE.as()
														.column())
						.comparator((o1, o2) -> {
							Integer code1 = o1.get(Master.CODE);
							Integer code2 = o2.get(Master.CODE);

							return code1.compareTo(code2);
						})
						.formatter(Master.NAME)
						.build();
	}

	public interface Detail {
		EntityType TYPE = DOMAIN.entityType("domain.detail_entity");

		Column<Long> ID = TYPE.longColumn("id");
		Column<Integer> INT = TYPE.integerColumn("int");
		Column<Double> DOUBLE = TYPE.doubleColumn("double");
		Column<String> STRING = TYPE.stringColumn("string");
		Column<LocalDate> DATE = TYPE.localDateColumn("date");
		Column<LocalDateTime> TIMESTAMP = TYPE.localDateTimeColumn("timestamp");
		Column<Boolean> BOOLEAN = TYPE.booleanColumn("boolean");
		Column<Boolean> BOOLEAN_NULLABLE = TYPE.booleanColumn("boolean_nullable");
		Column<Long> MASTER_ID = TYPE.longColumn("master_id");
		Attribute<String> MASTER_NAME = TYPE.stringAttribute("master_name");
		Attribute<Integer> MASTER_CODE = TYPE.integerAttribute("master_code");
		Column<Integer> INT_VALUE_LIST = TYPE.integerColumn("int_value_list");
		Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");

		ForeignKey MASTER_FK = TYPE.foreignKey("master_fk", MASTER_ID, Master.ID);
	}

	private static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

	private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
					item(2, "2"), item(3, "3"));

	EntityDefinition detail() {
		return Detail.TYPE.as(
										Detail.ID.as()
														.primaryKey(),
										Detail.INT.as()
														.column()
														.caption(Detail.INT.name()),
										Detail.DOUBLE.as()
														.column()
														.caption(Detail.DOUBLE.name()),
										Detail.STRING.as()
														.column()
														.caption("Detail string"),
										Detail.DATE.as()
														.column()
														.caption(Detail.DATE.name()),
										Detail.TIMESTAMP.as()
														.column()
														.caption(Detail.TIMESTAMP.name()),
										Detail.BOOLEAN.as()
														.column()
														.caption(Detail.BOOLEAN.name())
														.nullable(false)
														.defaultValue(true)
														.description("A boolean column"),
										Detail.BOOLEAN_NULLABLE.as()
														.column()
														.caption(Detail.BOOLEAN_NULLABLE.name())
														.defaultValue(true),
										Detail.MASTER_ID.as()
														.column()
														.readOnly(true),//AbstractEntityEditModelTest.persistWritableForeignKey()
										Detail.MASTER_FK.as()
														.foreignKey()
														.caption(Detail.MASTER_FK.name()),
										Detail.MASTER_NAME.as()
														.denormalized()
														.from(Detail.MASTER_FK)
														.using(Master.NAME)
														.caption(Detail.MASTER_NAME.name()),
										Detail.MASTER_CODE.as()
														.denormalized()
														.from(Detail.MASTER_FK)
														.using(Master.CODE)
														.caption(Detail.MASTER_CODE.name()),
										Detail.INT_VALUE_LIST.as()
														.column()
														.items(ITEMS)
														.caption(Detail.INT_VALUE_LIST.name()),
										Detail.INT_DERIVED.as()
														.derived()
														.from(Detail.INT)
														.with(values -> {
															Integer intValue = values.get(Detail.INT);
															if (intValue == null) {
																return null;
															}

															return intValue * 10;
														})
														.caption(Detail.INT_DERIVED.name()))
						.selectTable(DETAIL_SELECT_TABLE_NAME)
						.orderBy(ascending(Detail.STRING))
						.smallDataset(true)
						.formatter(Detail.STRING)
						.build();
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
														.caption(Department.ID.name())
														.updatable(true)
														.nullable(false),
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

		ConditionType CONDITION_1_TYPE = TYPE.conditionType("condition1Id");
		ConditionType CONDITION_2_TYPE = TYPE.conditionType("condition2Id");
		ConditionType ENAME_CLARK = TYPE.conditionType("condition3Id");
	}

	EntityDefinition employee() {
		return Employee.TYPE.as(
										Employee.ID.as()
														.primaryKey()
														.generator(sequence("employees.employee_seq"))
														.caption(Employee.ID.name()),
										Employee.NAME.as()
														.column()
														.caption(Employee.NAME.name())
														.searchable(true)
														.maximumLength(10)
														.nullable(false),
										Employee.DEPARTMENT.as()
														.column()
														.nullable(false),
										Employee.DEPARTMENT_FK.as()
														.foreignKey()
														.caption(Employee.DEPARTMENT_FK.name())
														.include(Department.NAME),
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
														.caption(Department.LOCATION.name()),
										Employee.DATA.as()
														.column()
														.selected(false))
						.formatter(Employee.NAME)
						.orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
						.condition(Employee.CONDITION_1_TYPE, (attributes, values) -> "1 = 2")
						.condition(Employee.CONDITION_2_TYPE, (attributes, values) -> "1 = 1")
						.condition(Employee.ENAME_CLARK, (attributes, values) -> " ename = 'CLARK'")
						.caption("Employee")
						.build();
	}

	public interface EnumEntity {
		EntityType TYPE = DOMAIN.entityType("enum_entity");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<EnumType> ENUM_TYPE = TYPE.column("enum_type", EnumType.class);

		enum EnumType {
			ONE, TWO, THREE
		}
	}

	EntityDefinition enumEntity() {
		return EnumEntity.TYPE.as(
										EnumEntity.ID.as()
														.primaryKey(),
										EnumEntity.ENUM_TYPE.as()
														.column())
						.build();
	}

	public interface Derived {
		EntityType TYPE = DOMAIN.entityType("derived");

		Column<Integer> INT1 = TYPE.integerColumn("int1");
		Column<Integer> INT2 = TYPE.integerColumn("int2");
		Column<Integer> INT3 = TYPE.integerColumn("int3");
		Column<Integer> INT4 = TYPE.integerColumn("int4");
	}

	EntityDefinition derived() {
		return Derived.TYPE.as(
										Derived.INT1.as()
														.column(),
										Derived.INT2.as()
														.derived()
														.from(Derived.INT1)
														.with(values -> values.optional(Derived.INT1)
																		.map(value -> value + 1)
																		.orElse(null)),
										Derived.INT3.as()
														.derived()
														.from(Derived.INT2)
														.with(values -> values.optional(Derived.INT2)
																		.map(value -> value + 1)
																		.orElse(null)),
										Derived.INT4.as()
														.derived()
														.from(Derived.INT3)
														.with(values -> values.optional(Derived.INT3)
																		.map(value -> value + 1)
																		.orElse(null)))
						.build();
	}

	public interface Job {
		EntityType TYPE = DOMAIN.entityType("job");

		Column<String> JOB = TYPE.stringColumn("job");
		Column<Double> MAX_SALARY = TYPE.doubleColumn("max_salary");
		Column<Double> MIN_SALARY = TYPE.doubleColumn("min_salary");
		Column<Double> MAX_COMMISSION = TYPE.doubleColumn("max_commission");
		Column<Double> MIN_COMMISSION = TYPE.doubleColumn("min_commission");

		ConditionType ADDITIONAL_HAVING = TYPE.conditionType("additional_having");
	}

	EntityDefinition job() {
		return Job.TYPE.as(
										Job.JOB.as()
														.primaryKey()
														.groupBy(true),
										Job.MAX_SALARY.as()
														.column()
														.expression("max(sal)")
														.aggregate(true),
										Job.MIN_SALARY.as()
														.column()
														.expression("min(sal)")
														.aggregate(true),
										Job.MAX_COMMISSION.as()
														.column()
														.expression("max(comm)")
														.aggregate(true),
										Job.MIN_COMMISSION.as()
														.column()
														.expression("min(comm)")
														.aggregate(true))
						.table("employees.employee")
						.selectQuery(EntitySelectQuery.builder()
										.having("job <> 'PRESIDENT'")
										.build())
						.condition(Job.ADDITIONAL_HAVING, (attributes, values) -> "count(*) > 1")
						.build();
	}

	public interface DateTimeTest {
		EntityType TYPE = DOMAIN.entityType("domain.date_time_test");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<LocalTime> TIME_HH_MM = TYPE.localTimeColumn("time_hh_mm");
		Column<LocalTime> TIME_HH_MM_SS = TYPE.localTimeColumn("time_hh_mm_ss");
		Column<LocalTime> TIME_HH_MM_SS_SSS = TYPE.localTimeColumn("time_hh_mm_ss_sss");
		Column<LocalDateTime> DATE_TIME_HH_MM = TYPE.localDateTimeColumn("date_time_hh_mm");
		Column<LocalDateTime> DATE_TIME_HH_MM_SS = TYPE.localDateTimeColumn("date_time_hh_mm_ss");
		Column<LocalDateTime> DATE_TIME_HH_MM_SS_SSS = TYPE.localDateTimeColumn("date_time_hh_mm_ss_sss");
	}

	EntityDefinition dateTimeTest() {
		return DateTimeTest.TYPE.as(
										DateTimeTest.ID.as()
														.primaryKey(),
										DateTimeTest.TIME_HH_MM.as()
														.column()
														.dateTimePattern("HH:mm"),
										DateTimeTest.TIME_HH_MM_SS.as()
														.column()
														.dateTimePattern("HH:mm.ss"),
										DateTimeTest.TIME_HH_MM_SS_SSS.as()
														.column()
														.dateTimePattern("HH:mm.ss.SSS"),
										DateTimeTest.DATE_TIME_HH_MM.as()
														.column()
														.dateTimePattern("dd-MM-yyyy HH:mm"),
										DateTimeTest.DATE_TIME_HH_MM_SS.as()
														.column()
														.dateTimePattern("dd-MM-yyyy HH:mm.ss"),
										DateTimeTest.DATE_TIME_HH_MM_SS_SSS.as()
														.column()
														.dateTimePattern("dd-MM-yyyy HH:mm.ss.SSS"))
						.build();
	}

	public interface NonGeneratedPK {
		EntityType TYPE = DOMAIN.entityType("nongenerated");

		Column<UUID> ID = TYPE.column("id", UUID.class);
		Column<String> NAME = TYPE.stringColumn("name");
	}

	EntityDefinition nongen() {
		return NonGeneratedPK.TYPE.as(
										NonGeneratedPK.ID.as()
														.primaryKey(),
										NonGeneratedPK.NAME.as()
														.column()
														.maximumLength(5))
						.build();
	}
}
