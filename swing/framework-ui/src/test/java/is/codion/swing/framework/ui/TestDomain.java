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

import is.codion.common.item.Item;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
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

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.OrderBy.ascending;
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
		add(Master.TYPE.define(
										Master.ID.define()
														.primaryKey(),
										Master.NAME.define()
														.column()
														.searchable(true),
										Master.CODE.define()
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
		add(Detail.TYPE.define(
										Detail.ID.define()
														.primaryKey(),
										Detail.INT.define()
														.column()
														.caption(Detail.INT.name())
														.range(-10_000, 10_000),
										Detail.DOUBLE.define()
														.column()
														.caption(Detail.DOUBLE.name())
														.range(-10_000, 10_000),
										Detail.BIG_DECIMAL.define()
														.column()
														.caption(Detail.BIG_DECIMAL.name()),
										Detail.STRING.define()
														.column()
														.caption("Detail string"),
										Detail.DATE.define()
														.column()
														.caption(Detail.DATE.name()),
										Detail.TIME.define()
														.column()
														.caption(Detail.TIME.name()),
										Detail.TIMESTAMP.define()
														.column()
														.caption(Detail.TIMESTAMP.name()),
										Detail.OFFSET.define()
														.column()
														.caption(Detail.OFFSET.name()),
										Detail.BOOLEAN.define()
														.column()
														.caption(Detail.BOOLEAN.name())
														.nullable(false)
														.defaultValue(true)
														.description("A boolean attribute"),
										Detail.BOOLEAN_NULLABLE.define()
														.column()
														.caption(Detail.BOOLEAN_NULLABLE.name())
														.defaultValue(true),
										Detail.MASTER_ID.define()
														.column(),
										Detail.MASTER_FK.define()
														.foreignKey()
														.caption(Detail.MASTER_FK.name()),
										Detail.DETAIL_ID.define()
														.column(),
										Detail.DETAIL_FK.define()
														.foreignKey()
														.caption(Detail.DETAIL_FK.name()),
										Detail.MASTER_NAME.define()
														.denormalized(Detail.MASTER_FK, Master.NAME)
														.caption(Detail.MASTER_NAME.name()),
										Detail.MASTER_CODE.define()
														.denormalized(Detail.MASTER_FK, Master.CODE)
														.caption(Detail.MASTER_CODE.name()),
										Detail.INT_ITEMS.define()
														.column()
														.items(ITEMS)
														.caption(Detail.INT_ITEMS.name()),
										Detail.INT_DERIVED.define()
														.derived(Detail.INT)
														.value(source -> {
															Integer intValue = source.get(Detail.INT);
															if (intValue == null) {
																return null;
															}

															return intValue * 10;
														})
														.caption(Detail.INT_DERIVED.name()),
										Detail.ENUM_TYPE.define()
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

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
		ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
	}

	void employee() {
		add(Employee.TYPE.define(
										Employee.ID.define()
														.primaryKey()
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
														.maximumFractionDigits(2),
										Employee.COMMISSION.define()
														.column()
														.caption(Employee.COMMISSION.name())
														.range(100, 2000)
														.maximumFractionDigits(2),
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
														.denormalized(Employee.DEPARTMENT_FK, Department.LOCATION)
														.caption(Department.LOCATION.name()))
						.formatter(Employee.NAME)
						.keyGenerator(KeyGenerator.sequence("employees.employee_seq"))
						.orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
						.caption("Employee")
						.build());
	}
}
