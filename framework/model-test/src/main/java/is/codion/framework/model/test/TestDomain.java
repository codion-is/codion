/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model.test;

import is.codion.common.item.Item;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
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
  }

  public interface Master {
    EntityType TYPE = DOMAIN.entityType("domain.master_entity");

    Column<Long> ID = TYPE.longColumn("id");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<Integer> CODE = TYPE.integerColumn("code");
  }

  void master() {
    add(Master.TYPE.define(
            Master.ID.primaryKeyColumn(),
            Master.NAME.column(),
            Master.CODE.column())
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
            Detail.ID.primaryKeyColumn(),
            Detail.INT.column()
                    .caption(Detail.INT.name()),
            Detail.DOUBLE.column()
                    .caption(Detail.DOUBLE.name()),
            Detail.STRING.column()
                    .caption("Detail string"),
            Detail.DATE.column()
                    .caption(Detail.DATE.name()),
            Detail.TIMESTAMP.column()
                    .caption(Detail.TIMESTAMP.name()),
            Detail.BOOLEAN.column()
                    .caption(Detail.BOOLEAN.name())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean column"),
            Detail.BOOLEAN_NULLABLE.column()
                    .caption(Detail.BOOLEAN_NULLABLE.name())
                    .defaultValue(true),
            Detail.MASTER_ID.column()
                    .readOnly(true),//AbstractEntityEditModelTest.persistWritableForeignKey()
            Detail.MASTER_FK.foreignKey()
                    .caption(Detail.MASTER_FK.name()),
            Detail.MASTER_NAME.denormalized(Detail.MASTER_FK, Master.NAME)
                    .caption(Detail.MASTER_NAME.name()),
            Detail.MASTER_CODE.denormalized(Detail.MASTER_FK, Master.CODE)
                    .caption(Detail.MASTER_CODE.name()),
            Detail.INT_VALUE_LIST.itemColumn(ITEMS)
                    .caption(Detail.INT_VALUE_LIST.name()),
            Detail.INT_DERIVED.derived(linkedValues -> {
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
    EntityType TYPE = DOMAIN.entityType("scott.dept");

    Column<Integer> ID = TYPE.integerColumn("deptno");
    Column<String> LOCATION = TYPE.stringColumn("loc");
    Column<String> NAME = TYPE.stringColumn("dname");
  }

  void department() {
    add(Department.TYPE.define(
            Department.ID.primaryKeyColumn()
                    .caption(Department.ID.name())
                    .updatable(true).nullable(false),
            Department.NAME.column()
                    .caption(Department.NAME.name())
                    .searchColumn(true)
                    .maximumLength(14)
                    .nullable(false),
            Department.LOCATION.column()
                    .caption(Department.LOCATION.name())
                    .maximumLength(13))
            .smallDataset(true)
            .orderBy(ascending(Department.NAME))
            .stringFactory(Department.NAME)
            .caption("Department"));
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("scott.emp");

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
            Employee.ID.primaryKeyColumn()
                    .caption(Employee.ID.name()),
            Employee.NAME.column()
                    .caption(Employee.NAME.name())
                    .searchColumn(true)
                    .maximumLength(10)
                    .nullable(false),
            Employee.DEPARTMENT.column()
                    .nullable(false),
            Employee.DEPARTMENT_FK.foreignKey()
                    .caption(Employee.DEPARTMENT_FK.name())
                    .attributes(Department.NAME),
            Employee.JOB.itemColumn(
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .caption(Employee.JOB.name())
                    .searchColumn(true),
            Employee.SALARY.column()
                    .caption(Employee.SALARY.name())
                    .nullable(false)
                    .valueRange(1000, 10000)
                    .maximumFractionDigits(2),
            Employee.COMMISSION.column()
                    .caption(Employee.COMMISSION.name())
                    .valueRange(100, 2000)
                    .maximumFractionDigits(2),
            Employee.MGR.column(),
            Employee.MGR_FK.foreignKey()
                    .caption(Employee.MGR_FK.name()),
            Employee.HIREDATE.column()
                    .caption(Employee.HIREDATE.name())
                    .nullable(false),
            Employee.DEPARTMENT_LOCATION.denormalized(Employee.DEPARTMENT_FK, Department.LOCATION)
                    .caption(Department.LOCATION.name()))
            .stringFactory(Employee.NAME)
            .keyGenerator(increment("scott.emp", "empno"))
            .orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
            .conditionProvider(Employee.CONDITION_1_TYPE, (attributes, values) -> "1 = 2")
            .conditionProvider(Employee.CONDITION_2_TYPE, (attributes, values) -> "1 = 1")
            .conditionProvider(Employee.CONDITION_3_TYPE, (attributes, values) -> " ename = 'CLARK'")
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
            EnumEntity.ID.primaryKeyColumn(),
            EnumEntity.ENUM_TYPE.column()));
  }
}
