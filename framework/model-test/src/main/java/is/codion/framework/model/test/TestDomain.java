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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model.test;

import is.codion.common.item.Item;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ConditionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  public static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    super(DOMAIN);
    master();
    detail();
    department();
    employee();
    enumEntity();
    derived();
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
                    .column(),
            Master.CODE.define()
                    .column())
            .comparator((o1, o2) -> {//keep like this for equality test in SwingEntityTableModelTest.testSortComparator()
              Integer code1 = o1.get(Master.CODE);
              Integer code2 = o2.get(Master.CODE);

              return code1.compareTo(code2);
            })
            .stringFactory(Master.NAME));
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
    Column<String> MASTER_NAME = TYPE.stringColumn("master_name");
    Column<Integer> MASTER_CODE = TYPE.integerColumn("master_code");
    Column<Integer> INT_VALUE_LIST = TYPE.integerColumn("int_value_list");
    Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");

    ForeignKey MASTER_FK = TYPE.foreignKey("master_fk", MASTER_ID, Master.ID);
  }

  private static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    add(Detail.TYPE.define(
            Detail.ID.define()
                    .primaryKey(),
            Detail.INT.define()
                    .column()
                    .caption(Detail.INT.name()),
            Detail.DOUBLE.define()
                    .column()
                    .caption(Detail.DOUBLE.name()),
            Detail.STRING.define()
                    .column()
                    .caption("Detail string"),
            Detail.DATE.define()
                    .column()
                    .caption(Detail.DATE.name()),
            Detail.TIMESTAMP.define()
                    .column()
                    .caption(Detail.TIMESTAMP.name()),
            Detail.BOOLEAN.define()
                    .column()
                    .caption(Detail.BOOLEAN.name())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean column"),
            Detail.BOOLEAN_NULLABLE.define()
                    .column()
                    .caption(Detail.BOOLEAN_NULLABLE.name())
                    .defaultValue(true),
            Detail.MASTER_ID.define()
                    .column()
                    .readOnly(true),//AbstractEntityEditModelTest.persistWritableForeignKey()
            Detail.MASTER_FK.define()
                    .foreignKey()
                    .caption(Detail.MASTER_FK.name()),
            Detail.MASTER_NAME.define()
                    .denormalized(Detail.MASTER_FK, Master.NAME)
                    .caption(Detail.MASTER_NAME.name()),
            Detail.MASTER_CODE.define()
                    .denormalized(Detail.MASTER_FK, Master.CODE)
                    .caption(Detail.MASTER_CODE.name()),
            Detail.INT_VALUE_LIST.define()
                    .column()
                    .items(ITEMS)
                    .caption(Detail.INT_VALUE_LIST.name()),
            Detail.INT_DERIVED.define()
                    .derived(linkedValues -> {
              Integer intValue = linkedValues.get(Detail.INT);
              if (intValue == null) {
                return null;
              }

              return intValue * 10;
            }, Detail.INT)
                    .caption(Detail.INT_DERIVED.name()))
            .selectTableName(DETAIL_SELECT_TABLE_NAME)
            .orderBy(ascending(Detail.STRING))
            .smallDataset(true)
            .stringFactory(Detail.STRING));
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("employees.department");

    Column<Integer> ID = TYPE.integerColumn("deptno");
    Column<String> LOCATION = TYPE.stringColumn("loc");
    Column<String> NAME = TYPE.stringColumn("dname");
  }

  void department() {
    add(Department.TYPE.define(
            Department.ID.define()
                    .primaryKey()
                    .caption(Department.ID.name())
                    .updatable(true).nullable(false),
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
            .stringFactory(Department.NAME)
            .caption("Department"));
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

    ConditionType CONDITION_1_TYPE = TYPE.conditionType("condition1Id");
    ConditionType CONDITION_2_TYPE = TYPE.conditionType("condition2Id");
    ConditionType CONDITION_3_TYPE = TYPE.conditionType("condition3Id");
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
                    .caption(Employee.DEPARTMENT_FK.name())
                    .attributes(Department.NAME),
            Employee.JOB.define()
                    .column()
                    .items(asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .caption(Employee.JOB.name())
                    .searchable(true),
            Employee.SALARY.define()
                    .column()
                    .caption(Employee.SALARY.name())
                    .nullable(false)
                    .valueRange(1000, 10000)
                    .maximumFractionDigits(2),
            Employee.COMMISSION.define()
                    .column()
                    .caption(Employee.COMMISSION.name())
                    .valueRange(100, 2000)
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
            .stringFactory(Employee.NAME)
            .keyGenerator(KeyGenerator.sequence("employees.employee_seq"))
            .orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
            .condition(Employee.CONDITION_1_TYPE, (attributes, values) -> "1 = 2")
            .condition(Employee.CONDITION_2_TYPE, (attributes, values) -> "1 = 1")
            .condition(Employee.CONDITION_3_TYPE, (attributes, values) -> " ename = 'CLARK'")
            .caption("Employee")
            .backgroundColorProvider((entity, attribute) -> {
              if (attribute.equals(Employee.JOB) && "MANAGER".equals(entity.get(Employee.JOB))) {
                return "#00ff00";//Color.GREEN
              }

              return null;
            }));
  }

  public interface EnumEntity {
    EntityType TYPE = DOMAIN.entityType("enum_entity");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<EnumType> ENUM_TYPE = TYPE.column("enum_type", EnumType.class);

    enum EnumType {
      ONE, TWO, THREE
    }
  }

  void enumEntity() {
    add(EnumEntity.TYPE.define(
            EnumEntity.ID.define()
                    .primaryKey(),
            EnumEntity.ENUM_TYPE.define()
                    .column()));
  }

  public interface Derived {
    EntityType TYPE = DOMAIN.entityType("derived");

    Column<Integer> INT = TYPE.integerColumn("int");
    Column<Integer> INT2 = TYPE.integerColumn("int2");
    Column<Integer> INT3 = TYPE.integerColumn("int3");
    Column<Integer> INT4 = TYPE.integerColumn("int4");
  }

  void derived() {
    add(Derived.TYPE.define(
            Derived.INT.define()
                    .column(),
            Derived.INT2.define()
                    .derived(sourceValues -> sourceValues.optional(Derived.INT)
                            .map(value -> value + 1)
                            .orElse(null), Derived.INT),
            Derived.INT3.define()
                    .derived(sourceValues -> sourceValues.optional(Derived.INT2)
                            .map(value -> value + 1)
                            .orElse(null), Derived.INT2),
            Derived.INT4.define()
                    .derived(sourceValues -> sourceValues.optional(Derived.INT3)
                            .map(value -> value + 1)
                            .orElse(null), Derived.INT3)));
  }
}
