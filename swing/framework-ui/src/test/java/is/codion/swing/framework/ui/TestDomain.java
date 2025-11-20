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
package is.codion.swing.framework.ui;

import is.codion.common.utilities.item.Item;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;
import static java.util.Arrays.asList;

public final class TestDomain extends DomainModel {

	public static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

	public TestDomain() {
		super(DOMAIN);
		master();
		detail();
		department();
		employee();
	}

	public interface Master {
		EntityType TYPE = DOMAIN.entityType("domain.master_entity");

		Column<Long> ID = TYPE.longColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<Integer> CODE = TYPE.integerColumn("code");
	}

	void master() {
		add(Master.TYPE.as(
										Master.ID.as()
														.primaryKey(),
										Master.NAME.as()
														.column()
														.searchable(true),
										Master.CODE.as()
														.column())
						.comparator(Comparator.comparing(o -> o.get(Master.CODE)))
						.formatter(Master.NAME)
						.build());
	}

	public interface Detail {
		EntityType TYPE = DOMAIN.entityType("domain.detail_entity");

		Column<Long> ID = TYPE.longColumn("id");
		Column<Integer> INT = TYPE.integerColumn("int");
		Column<Double> DOUBLE = TYPE.doubleColumn("double");
		Column<BigDecimal> BIG_DECIMAL = TYPE.bigDecimalColumn("big_decimal");
		Column<String> STRING = TYPE.stringColumn("string");
		Column<LocalTime> TIME = TYPE.localTimeColumn("time");
		Column<LocalDate> DATE = TYPE.localDateColumn("date");
		Column<LocalDateTime> TIMESTAMP = TYPE.localDateTimeColumn("timestamp");
		Column<OffsetDateTime> OFFSET = TYPE.offsetDateTimeColumn("offset");
		Column<Boolean> BOOLEAN = TYPE.booleanColumn("boolean");
		Column<Boolean> BOOLEAN_NULLABLE = TYPE.booleanColumn("boolean_nullable");
		Column<Long> MASTER_ID = TYPE.longColumn("master_id");
		ForeignKey MASTER_FK = TYPE.foreignKey("master_fk", MASTER_ID, Master.ID);
		Column<Long> DETAIL_ID = TYPE.longColumn("detail_id");
		ForeignKey DETAIL_FK = TYPE.foreignKey("detail_fk", DETAIL_ID, ID);
		Column<String> MASTER_NAME = TYPE.stringColumn("master_name");
		Column<Integer> MASTER_CODE = TYPE.integerColumn("master_code");
		Column<Integer> INT_ITEMS = TYPE.integerColumn("int_items");
		Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");
		Column<EnumType> ENUM_TYPE = TYPE.column("enum_type", EnumType.class);

		enum EnumType {
			ONE, TWO, THREE
		}
	}

	private static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

	private static final List<Item<Integer>> ITEMS = asList(
					item(0, "Zero"), item(1, "One"),
					item(2, "Two"), item(3, "Three"));

	void detail() {
		add(Detail.TYPE.as(
										Detail.ID.as()
														.primaryKey(),
										Detail.INT.as()
														.column()
														.caption(Detail.INT.name())
														.range(-10_000, 10_000),
										Detail.DOUBLE.as()
														.column()
														.caption(Detail.DOUBLE.name())
														.range(-10_000, 10_000),
										Detail.BIG_DECIMAL.as()
														.column()
														.caption(Detail.BIG_DECIMAL.name()),
										Detail.STRING.as()
														.column()
														.caption("Detail string"),
										Detail.DATE.as()
														.column()
														.caption(Detail.DATE.name()),
										Detail.TIME.as()
														.column()
														.caption(Detail.TIME.name()),
										Detail.TIMESTAMP.as()
														.column()
														.caption(Detail.TIMESTAMP.name()),
										Detail.OFFSET.as()
														.column()
														.caption(Detail.OFFSET.name()),
										Detail.BOOLEAN.as()
														.column()
														.caption(Detail.BOOLEAN.name())
														.nullable(false)
														.defaultValue(true)
														.description("A boolean attribute"),
										Detail.BOOLEAN_NULLABLE.as()
														.column()
														.caption(Detail.BOOLEAN_NULLABLE.name())
														.defaultValue(true),
										Detail.MASTER_ID.as()
														.column(),
										Detail.MASTER_FK.as()
														.foreignKey()
														.caption(Detail.MASTER_FK.name()),
										Detail.DETAIL_ID.as()
														.column(),
										Detail.DETAIL_FK.as()
														.foreignKey()
														.caption(Detail.DETAIL_FK.name()),
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
										Detail.INT_ITEMS.as()
														.column()
														.items(ITEMS)
														.caption(Detail.INT_ITEMS.name()),
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
														.caption(Detail.INT_DERIVED.name()),
										Detail.ENUM_TYPE.as()
														.column())
						.selectTable(DETAIL_SELECT_TABLE_NAME)
						.orderBy(ascending(Detail.STRING))
						.smallDataset(true)
						.formatter(Detail.STRING)
						.build());
	}

	public interface Department {
		EntityType TYPE = DOMAIN.entityType("employees.department");

		Column<Integer> ID = TYPE.integerColumn("deptno");
		Column<String> NAME = TYPE.stringColumn("dname");
		Column<String> LOCATION = TYPE.stringColumn("loc");
	}

	void department() {
		add(Department.TYPE.as(
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
	}

	void employee() {
		add(Employee.TYPE.as(
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
						.orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
						.caption("Employee")
						.build());
	}
}
