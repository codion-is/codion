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
package is.codion.framework.domain;

import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.common.utilities.item.Item;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.DerivedValue;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ConditionType;
import is.codion.framework.domain.entity.query.EntitySelectQuery;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.attribute.Column.Generator.queried;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;
import static java.util.Arrays.asList;

public final class TestDomain extends DomainModel {

	public static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

	public TestDomain() {
		this(DOMAIN);
	}

	public TestDomain(DomainType domain) {
		super(domain);
		compositeMaster();
		compositeDetail();
		master();
		detail();
		superEntity();
		master2();
		detail2();
		department();
		employee();
		keyTest();
		noPKEntity();
		transientModifies();
		transientModifiesNot();
		nullString();
		invalidDerived();
		foreignKeyLazyColumn();
		nonCachedToString();
	}

	public interface CompositeMaster {
		EntityType TYPE = DOMAIN.entityType("domain.composite_master");

		Column<Integer> COMPOSITE_MASTER_ID_3 = TYPE.integerColumn("id3");
		Column<Integer> COMPOSITE_MASTER_ID_2 = TYPE.integerColumn("id2");
		Column<Integer> COMPOSITE_MASTER_ID = TYPE.integerColumn("id");
	}

	void compositeMaster() {
		add(CompositeMaster.TYPE.as(
										CompositeMaster.COMPOSITE_MASTER_ID.as()
														.primaryKey(0).nullable(true),
										CompositeMaster.COMPOSITE_MASTER_ID_2.as()
														.primaryKey(1),
										CompositeMaster.COMPOSITE_MASTER_ID_3.as()
														.primaryKey(2))
						.build());
	}

	public interface CompositeDetail {
		EntityType TYPE = DOMAIN.entityType("domain.composite_detail");

		Column<Integer> COMPOSITE_DETAIL_MASTER_ID = TYPE.integerColumn("master_id");
		Column<Integer> COMPOSITE_DETAIL_MASTER_ID_2 = TYPE.integerColumn("master_id2");
		Column<Integer> COMPOSITE_DETAIL_MASTER_ID_3 = TYPE.integerColumn("master_id3");
		ForeignKey COMPOSITE_DETAIL_MASTER_FK = TYPE.foreignKey("master_fk",
						COMPOSITE_DETAIL_MASTER_ID, CompositeMaster.COMPOSITE_MASTER_ID,
						COMPOSITE_DETAIL_MASTER_ID_2, CompositeMaster.COMPOSITE_MASTER_ID_2,
						COMPOSITE_DETAIL_MASTER_ID_3, CompositeMaster.COMPOSITE_MASTER_ID_3);
	}

	void compositeDetail() {
		add(CompositeDetail.TYPE.as(
										CompositeDetail.COMPOSITE_DETAIL_MASTER_ID.as()
														.primaryKey(0),
										CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2.as()
														.primaryKey(1),
										CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3.as()
														.primaryKey(2),
										CompositeDetail.COMPOSITE_DETAIL_MASTER_FK.as()
														.foreignKey()
														.caption("master")
														.readOnly(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3))
						.build());
	}

	public interface Super {
		EntityType TYPE = DOMAIN.entityType("db.super_entity");

		Column<Integer> ID = TYPE.integerColumn("id");
	}

	void superEntity() {
		add(Super.TYPE.as(Super.ID.as().primaryKey()).build());
	}

	public interface Master {
		EntityType TYPE = DOMAIN.entityType("domain.master_entity");
		Column<Long> ID = TYPE.longColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<Integer> CODE = TYPE.integerColumn("code");
		Column<Integer> READ_ONLY = TYPE.integerColumn("read_only");
	}

	void master() {
		add(Master.TYPE.as(
										Master.ID.as()
														.primaryKey(),
										Master.NAME.as()
														.column(),
										Master.CODE.as()
														.column(),
										Master.READ_ONLY.as()
														.column()
														.readOnly(true))
						.comparator(new MasterComparator())
						.formatter(Master.NAME)
						.build());
	}

	public interface Master2 {
		EntityType TYPE = DOMAIN.entityType("db.master_entity");

		Column<Integer> ID_1 = TYPE.integerColumn("id");
		Column<Integer> ID_2 = TYPE.integerColumn("id2");
		Column<Integer> SUPER_ID = TYPE.integerColumn("super_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<Integer> CODE = TYPE.integerColumn("code");

		ForeignKey SUPER_FK = TYPE.foreignKey("super_fk", SUPER_ID, Super.ID);
	}

	void master2() {
		add(Master2.TYPE.as(
										Master2.ID_1.as().primaryKey(0),
										Master2.ID_2.as().primaryKey(1),
										Master2.SUPER_ID.as().column(),
										Master2.SUPER_FK.as()
														.foreignKey().caption("Super"),
										Master2.NAME.as().column(),
										Master2.CODE.as().column())
						.comparator(Comparator.comparing(o -> o.get(Master2.CODE)))
						.formatter(Master2.NAME)
						.build());
	}

	public interface Detail2 {
		EntityType TYPE = DOMAIN.entityType("db.detail_entity2");

		Column<Long> ID = TYPE.longColumn("id");
		Column<Integer> INT = TYPE.integerColumn("int");
		Column<Double> DOUBLE = TYPE.doubleColumn("double");
		Column<String> STRING = TYPE.stringColumn("string");
		Column<LocalDate> DATE = TYPE.localDateColumn("date");
		Column<LocalDateTime> TIMESTAMP = TYPE.localDateTimeColumn("timestamp");
		Column<Boolean> BOOLEAN = TYPE.booleanColumn("boolean");
		Column<Boolean> BOOLEAN_NULLABLE = TYPE.booleanColumn("boolean_nullable");
		Column<Integer> MASTER_ID_1 = TYPE.integerColumn("master_id");
		Column<Integer> MASTER_ID_2 = TYPE.integerColumn("master_id_2");
		Column<String> MASTER_NAME = TYPE.stringColumn("master_name");
		Column<Integer> MASTER_CODE = TYPE.integerColumn("master_code");
		Column<Integer> INT_ITEMS = TYPE.integerColumn("int_value_list");
		Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");

		ForeignKey MASTER_FK = TYPE.foreignKey("master_fk",
						MASTER_ID_1, Master2.ID_1,
						MASTER_ID_2, Master2.ID_2);
		ForeignKey MASTER_VIA_CODE_FK = TYPE.foreignKey("master_via_code_fk", MASTER_CODE, Master2.CODE);
	}

	private static final EntityType DETAIL_SELECT_TABLE_NAME = DOMAIN.entityType("db.entity_test_select");

	private static final List<Item<Integer>> INT_VALUE_ITEMS = asList(item(0, "Zero"), item(1, "One"),
					item(2, "Two"), item(3, "Three"));

	void detail2() {
		add(Detail2.TYPE.as(
										Detail2.ID.as()
														.primaryKey(),
										Detail2.INT.as()
														.column()
														.caption(Detail2.INT.name()),
										Detail2.DOUBLE.as()
														.column()
														.caption(Detail2.DOUBLE.name()),
										Detail2.STRING.as()
														.column()
														.caption("Detail2 string"),
										Detail2.DATE.as()
														.column()
														.caption(Detail2.DATE.name()),
										Detail2.TIMESTAMP.as()
														.column()
														.caption(Detail2.TIMESTAMP.name()),
										Detail2.BOOLEAN.as()
														.column()
														.caption(Detail2.BOOLEAN.name())
														.nullable(false)
														.defaultValue(true)
														.description("A boolean column"),
										Detail2.BOOLEAN_NULLABLE.as()
														.column()
														.caption(Detail2.BOOLEAN_NULLABLE.name())
														.defaultValue(true),
										Detail2.MASTER_ID_1.as()
														.column(),
										Detail2.MASTER_ID_2.as()
														.column(),
										Detail2.MASTER_FK.as()
														.foreignKey()
														.caption(Detail2.MASTER_FK.name()),
										Detail2.MASTER_VIA_CODE_FK.as()
														.foreignKey()
														.caption(Detail2.MASTER_FK.name()),
										Detail2.MASTER_NAME.as()
														.denormalized()
														.from(Detail2.MASTER_FK)
														.using(Master2.NAME)
														.caption(Detail2.MASTER_NAME.name()),
										Detail2.MASTER_CODE.as()
														.column()
														.caption(Detail2.MASTER_CODE.name()),
										Detail2.INT_ITEMS.as()
														.column()
														.items(INT_VALUE_ITEMS)
														.caption(Detail2.INT_ITEMS.name()),
										Detail2.INT_DERIVED.as()
														.derived()
														.from(Detail2.INT)
														.with(values -> {
															Integer intValue = values.get(Detail2.INT);
															if (intValue == null) {
																return null;
															}

															return intValue * 10;
														})
														.caption(Detail2.INT_DERIVED.name()))
						.selectTable(DETAIL_SELECT_TABLE_NAME.name())
						.orderBy(ascending(Detail2.STRING))
						.smallDataset(true)
						.formatter(Detail2.STRING)
						.build());
	}

	private static final class MasterComparator implements Comparator<Entity>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public int compare(Entity o1, Entity o2) {
			return o1.get(Master.CODE).compareTo(o2.get(Master.CODE));
		}
	}

	public interface Detail {
		EntityType TYPE = DOMAIN.entityType("domain.detail_entity");
		Column<Long> ID = TYPE.longColumn("id");
		Column<Short> SHORT = TYPE.shortColumn("short");
		Column<Integer> INT = TYPE.integerColumn("int");
		Column<Double> DOUBLE = TYPE.doubleColumn("double");
		Column<String> STRING = TYPE.stringColumn("string");
		Column<LocalDate> DATE = TYPE.localDateColumn("date");
		Column<LocalDateTime> TIMESTAMP = TYPE.localDateTimeColumn("timestamp");
		Column<Boolean> BOOLEAN = TYPE.booleanColumn("boolean");
		Column<Boolean> BOOLEAN_NULLABLE = TYPE.booleanColumn("boolean_nullable");
		Column<Long> MASTER_ID = TYPE.longColumn("master_id");
		ForeignKey MASTER_FK = TYPE.foreignKey("master2_fk", MASTER_ID, Master.ID);
		Column<String> MASTER_NAME = TYPE.stringColumn("master_name");
		Column<Integer> MASTER_CODE = TYPE.integerColumn("master_code");
		Column<Integer> MASTER_CODE_NON_DENORM = TYPE.integerColumn("master_code_non_denorm");
		ForeignKey MASTER_VIA_CODE_FK = TYPE.foreignKey("master_via_code_fk", MASTER_CODE_NON_DENORM, Master.CODE);
		Column<Integer> INT_ITEMS = TYPE.integerColumn("int_value_list");
		Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");
		Column<byte[]> BYTES = TYPE.byteArrayColumn("bytes");
	}

	void detail() {
		add(Detail.TYPE.as(
										Detail.ID.as()
														.primaryKey()
														.generator(queried("select id from dual")),
										Detail.SHORT.as()
														.column()
														.caption(Detail.SHORT.name()),
										Detail.INT.as()
														.column()
														.caption(Detail.INT.name()),
										Detail.DOUBLE.as()
														.column()
														.caption(Detail.DOUBLE.name())
														.withDefault(true),
										Detail.STRING.as()
														.column()
														.caption("Detail string")
														.selected(false),
										Detail.DATE.as()
														.column()
														.caption(Detail.DATE.name())
														.withDefault(true),
										Detail.TIMESTAMP.as()
														.column()
														.caption(Detail.TIMESTAMP.name()),
										Detail.BOOLEAN.as()
														.column()
														.caption(Detail.BOOLEAN.name())
														.nullable(false)
														.defaultValue(true)
														.description("A boolean property"),
										Detail.BOOLEAN_NULLABLE.as()
														.column()
														.caption(Detail.BOOLEAN_NULLABLE.name())
														.withDefault(true)
														.defaultValue(true),
										Detail.MASTER_ID.as()
														.column(),
										Detail.MASTER_FK.as()
														.foreignKey()
														.caption(Detail.MASTER_FK.name()),
										Detail.MASTER_CODE_NON_DENORM.as()
														.column(),
										Detail.MASTER_VIA_CODE_FK.as()
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
										Detail.INT_ITEMS.as()
														.column()
														.items(INT_VALUE_ITEMS)
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
										Detail.BYTES.as()
														.column()
														.updatable(false)
														.selected(false))
						.orderBy(ascending(Detail.STRING))
						.selectTable(DETAIL_SELECT_TABLE_NAME.name())
						.smallDataset(true)
						.formatter(Detail.STRING)
						.build());
	}

	public interface Department {
		EntityType TYPE = DOMAIN.entityType("domain.employees.department");
		Column<Integer> ID = TYPE.integerColumn("deptno");
		Column<String> NAME = TYPE.stringColumn("dname");
		Column<String> LOCATION = TYPE.stringColumn("loc");
		Column<Boolean> ACTIVE = TYPE.booleanColumn("active");
		Column<byte[]> DATA = TYPE.byteArrayColumn("data");
		Column<Character> CODE = TYPE.characterColumn("code");

		ConditionType CONDITION = TYPE.conditionType("condition");
		ConditionType NAME_NOT_NULL_CONDITION = TYPE.conditionType("conditionNameNotNull");
	}

	void department() {
		add(Department.TYPE.as(
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
														.maximumLength(13),
										Department.ACTIVE.as()
														.column()
														.readOnly(true),
										Department.DATA.as()
														.column()
														.selected(false),
										Department.CODE.as()
														.column())
						.table("employees.department")
						.smallDataset(true)
						.orderBy(ascending(Department.NAME))
						.formatter(Department.NAME)
						.condition(Department.CONDITION, (columns, values) -> {
							StringBuilder builder = new StringBuilder("deptno in (");
							values.forEach(value -> builder.append("?,"));
							builder.deleteCharAt(builder.length() - 1);

							return builder.append(")").toString();
						})
						.condition(Department.NAME_NOT_NULL_CONDITION, (columns, values) -> "department name is not null")
						.caption("Department")
						.build());
	}

	public interface Employee {
		EntityType TYPE = DOMAIN.entityType("domain.employees.employee");
		Column<Integer> ID = TYPE.integerColumn("emp_id");
		Column<String> NAME = TYPE.stringColumn("emp_name");
		Column<String> JOB = TYPE.stringColumn("job");
		Column<Integer> MGR = TYPE.integerColumn("mgr");
		Column<LocalDateTime> HIREDATE = TYPE.localDateTimeColumn("hiredate");
		Column<Double> SALARY = TYPE.doubleColumn("sal");
		Column<Double> COMMISSION = TYPE.doubleColumn("comm");
		Column<Integer> DEPARTMENT_NO = TYPE.integerColumn("deptno");
		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT_NO, Department.ID);
		ForeignKey MANAGER_FK = TYPE.foreignKey("mgr_fk", MGR, Employee.ID);
		Column<String> DEPARTMENT_LOCATION = TYPE.stringColumn("location");
		Attribute<String> DEPARTMENT_NAME = TYPE.stringAttribute("department_name");
		Column<byte[]> DATA = TYPE.byteArrayColumn("data");

		ConditionType CONDITION = TYPE.conditionType("condition");
	}

	void employee() {
		add(Employee.TYPE.as(
										Employee.ID.as()
														.primaryKey()
														.generator(sequence("employees.employee_seq"))
														.caption(Employee.ID.name())
														.name("empno"),
										Employee.NAME.as()
														.column()
														.caption(Employee.NAME.name())
														.searchable(true)
														.name("ename")
														.maximumLength(10)
														.nullable(false),
										Employee.DEPARTMENT_NO.as()
														.column()
														.nullable(false),
										Employee.DEPARTMENT_FK.as()
														.foreignKey()
														.caption(Employee.DEPARTMENT_FK.name()),
										Employee.JOB.as()
														.column()
														.items(asList(item("ANALYST"), item("CLERK"),
																		item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
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
										Employee.MANAGER_FK.as()
														.foreignKey()
														.caption(Employee.MANAGER_FK.name()),
										Employee.HIREDATE.as()
														.column()
														.caption(Employee.HIREDATE.name())
														.updatable(false)
														.dateTimePattern(LocaleDateTimePattern.builder()
																		.delimiterDot()
																		.yearFourDigits()
																		.build())
														.nullable(false),
										Employee.DEPARTMENT_LOCATION.as()
														.denormalized()
														.from(Employee.DEPARTMENT_FK)
														.using(Department.LOCATION)
														.caption(Department.LOCATION.name()),
										Employee.DEPARTMENT_NAME.as()
														.derived()
														.from(Employee.NAME, Employee.DEPARTMENT_FK)
														.with(new DepartmentName()),
										Employee.DATA.as()
														.column()
														.caption("Data"))
						.table("employees.employee")
						.selectTable("employees.employee")
						.orderBy(ascending(Employee.DEPARTMENT_NO, Employee.NAME))
						.formatter(Employee.NAME)
						.selectQuery(EntitySelectQuery.builder().build())
						.condition(Employee.CONDITION, (columns, values) -> "")
						.caption("Employee")
						.build());
	}

	private static final class DepartmentName implements DerivedValue<String>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public String get(SourceValues values) {
			String name = values.get(Employee.NAME);
			Entity department = values.get(Employee.DEPARTMENT_FK);
			if (name == null || department == null) {
				return null;
			}
			return name + " - " + department.get(Department.NAME);
		}
	}

	public interface KeyTest {
		EntityType TYPE = DOMAIN.entityType("KeyTest");

		Column<Integer> ID1 = TYPE.integerColumn("id1");
		Column<Integer> ID2 = TYPE.integerColumn("id2");
		Column<Integer> ID3 = TYPE.integerColumn("id3");
	}

	void keyTest() {
		add(KeyTest.TYPE.as(
										KeyTest.ID1.as()
														.primaryKey(0),
										KeyTest.ID2.as()
														.primaryKey(1),
										KeyTest.ID3.as()
														.primaryKey(2)
														.nullable(true))
						.build());
	}

	public interface NoPk {
		EntityType TYPE = DOMAIN.entityType("no_pk");
		Column<Integer> COL1 = TYPE.integerColumn("col1");
		Column<Integer> COL2 = TYPE.integerColumn("col2");
		Column<Integer> COL3 = TYPE.integerColumn("col3");
	}

	void noPKEntity() {
		add(NoPk.TYPE.as(
										NoPk.COL1.as()
														.column(),
										NoPk.COL2.as()
														.column(),
										NoPk.COL3.as()
														.column())
						.build());
	}

	public interface TransModifies {
		EntityType TYPE = DOMAIN.entityType("trans_modifies");

		Column<Integer> ID = TYPE.integerColumn("id");
		Attribute<Integer> TRANS = TYPE.integerAttribute("trans");
	}

	void transientModifies() {
		add(TransModifies.TYPE.as(
										TransModifies.ID.as()
														.primaryKey(),
										TransModifies.TRANS.as()
														.attribute())
						.build());
	}

	public interface TransModifiesNot {
		EntityType TYPE = DOMAIN.entityType("trans_modifies_not");

		Column<Integer> ID = TYPE.integerColumn("id");
		Attribute<Integer> TRANS = TYPE.integerAttribute("trans");
	}

	void transientModifiesNot() {
		add(TransModifiesNot.TYPE.as(
										TransModifiesNot.ID.as()
														.primaryKey(),
										TransModifiesNot.TRANS.as()
														.attribute()
														.modifies(false))
						.build());
	}

	public interface NullString {
		EntityType TYPE = DOMAIN.entityType("null_string");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> ATTR = TYPE.integerColumn("attr");
		Column<Integer> ATTR2 = TYPE.integerColumn("attr2");
	}

	void nullString() {
		add(NullString.TYPE.as(
										NullString.ID.as()
														.primaryKey(),
										NullString.ATTR.as()
														.column(),
										NullString.ATTR2.as()
														.column())
						.formatter(entity -> null)
						.build());
	}

	public interface InvalidDerived {
		EntityType TYPE = DOMAIN.entityType("invalid_derived");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> INT = TYPE.integerColumn("int");
		Attribute<Integer> INVALID_DERIVED = TYPE.integerAttribute("invalid_derived");
	}

	void invalidDerived() {
		add(InvalidDerived.TYPE.as(
										InvalidDerived.ID.as()
														.primaryKey(),
										InvalidDerived.INT.as()
														.column(),
										InvalidDerived.INVALID_DERIVED.as()
														.derived()
														.from(InvalidDerived.ID)
														.with(values -> values.get(InvalidDerived.INT).intValue()))
						.caption(InvalidDerived.INVALID_DERIVED.name())//incorrect source value, trigger exception
						.formatter(entity -> null)
						.build());
	}

	public interface ForeignKeyLazyColumn {
		EntityType TYPE = DOMAIN.entityType("foreign_key_lazy_column");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> DEPARTMENT_ID = TYPE.integerColumn("department_id");

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("department_fk", DEPARTMENT_ID, Department.ID);
	}

	void foreignKeyLazyColumn() {
		add(ForeignKeyLazyColumn.TYPE.as(
										ForeignKeyLazyColumn.ID.as()
														.primaryKey(),
										ForeignKeyLazyColumn.DEPARTMENT_ID.as()
														.column()
														.selected(false),
										ForeignKeyLazyColumn.DEPARTMENT_FK.as()
														.foreignKey())
						.build());
	}

	public interface NonCachedToString {
		EntityType TYPE = DOMAIN.entityType("non_cached_to_string");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> STRING = TYPE.stringColumn("string");
	}

	void nonCachedToString() {
		add(NonCachedToString.TYPE.as(
										NonCachedToString.ID.as()
														.primaryKey(),
										NonCachedToString.STRING.as()
														.column())
						.formatter(entity -> entity.formatted(NonCachedToString.ID) + "." + entity.get(NonCachedToString.STRING))
						.cacheToString(false)
						.build());
	}
}
