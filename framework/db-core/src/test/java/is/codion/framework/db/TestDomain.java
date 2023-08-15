/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

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
import java.util.Comparator;
import java.util.List;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    super(DOMAIN);
    superEntity();
    master();
    detail();
    department();
    employee();
  }

  public interface Super {
    EntityType TYPE = DOMAIN.entityType("db.super_entity");

    Column<Integer> ID = TYPE.integerColumn("id");
  }

  void superEntity() {
    add(Super.TYPE.define(Super.ID.primaryKey()));
  }

  public interface Master {
    EntityType TYPE = DOMAIN.entityType("db.master_entity");

    Column<Integer> ID_1 = TYPE.integerColumn("id");
    Column<Integer> ID_2 = TYPE.integerColumn("id2");
    Column<Integer> SUPER_ID = TYPE.integerColumn("super_id");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<Integer> CODE = TYPE.integerColumn("code");

    ForeignKey SUPER_FK = TYPE.foreignKey("super_fk", SUPER_ID, Super.ID);
  }

  void master() {
    add(Master.TYPE.define(
            Master.ID_1.column().primaryKeyIndex(0),
            Master.ID_2.column().primaryKeyIndex(1),
            Master.SUPER_ID.column(),
            Master.SUPER_FK.foreignKey().caption("Super"),
            Master.NAME.column(),
            Master.CODE.column())
            .comparator(Comparator.comparing(o -> o.get(Master.CODE)))
            .stringFactory(Master.NAME));
  }

  public interface Detail {
    EntityType TYPE = DOMAIN.entityType("db.detail_entity");

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
    Column<Integer> INT_VALUE_LIST = TYPE.integerColumn("int_value_list");
    Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");

    ForeignKey MASTER_FK = TYPE.foreignKey("master_fk",
            MASTER_ID_1, Master.ID_1,
            MASTER_ID_2, Master.ID_2);
    ForeignKey MASTER_VIA_CODE_FK = TYPE.foreignKey("master_via_code_fk", MASTER_CODE, Master.CODE);
  }

  private static final EntityType DETAIL_SELECT_TABLE_NAME = DOMAIN.entityType("db.entity_test_select");

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    add(Detail.TYPE.define(
            Detail.ID.primaryKey(),
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
            Detail.MASTER_ID_1.column(),
            Detail.MASTER_ID_2.column(),
            Detail.MASTER_FK.foreignKey()
                    .caption(Detail.MASTER_FK.name()),
            Detail.MASTER_VIA_CODE_FK.foreignKey()
                    .caption(Detail.MASTER_FK.name()),
            Detail.MASTER_NAME.denormalized(Detail.MASTER_FK, Master.NAME)
                    .caption(Detail.MASTER_NAME.name()),
            Detail.MASTER_CODE.column()
                    .caption(Detail.MASTER_CODE.name()),
            Detail.INT_VALUE_LIST.item(ITEMS)
                    .caption(Detail.INT_VALUE_LIST.name()),
            Detail.INT_DERIVED.derived(linkedValues -> {
              Integer intValue = linkedValues.get(Detail.INT);
              if (intValue == null) {
                return null;
              }

              return intValue * 10;
            }, Detail.INT)
                    .caption(Detail.INT_DERIVED.name()))
            .selectTableName(DETAIL_SELECT_TABLE_NAME.name())
            .orderBy(ascending(Detail.STRING))
            .smallDataset(true)
            .stringFactory(Detail.STRING));
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("db.scott.dept");

    Column<Integer> ID = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");

    ConditionType CONDITION = TYPE.conditionType("condition");
    ConditionType NAME_NOT_NULL_CONDITION = TYPE.conditionType("conditionNameNotNull");
  }

  void department() {
    add(Department.TYPE.define(
            Department.ID.primaryKey()
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
            .tableName("scott.dept")
            .smallDataset(true)
            .orderBy(ascending(Department.NAME))
            .stringFactory(Department.NAME)
            .conditionProvider(Department.CONDITION, (columns, values) -> {
              StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .conditionProvider(Department.NAME_NOT_NULL_CONDITION, (columns, values) -> "department name is not null")
            .caption("Department"));
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("db.scott.emp");

    Column<Integer> ID = TYPE.integerColumn("emp_id");
    Column<String> NAME = TYPE.stringColumn("emp_name");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Integer> MGR = TYPE.integerColumn("mgr");
    Column<LocalDateTime> HIREDATE = TYPE.localDateTimeColumn("hiredate");
    Column<Double> SALARY = TYPE.doubleColumn("sal");
    Column<Double> COMMISSION = TYPE.doubleColumn("comm");
    Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
    Column<String> DEPARTMENT_LOCATION = TYPE.stringColumn("location");

    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
  }

  void employee() {
    add(Employee.TYPE.define(
            Employee.ID.primaryKey()
                    .caption(Employee.ID.name()).columnName("empno"),
            Employee.NAME.column()
                    .caption(Employee.NAME.name())
                    .searchColumn(true)
                    .columnName("ename")
                    .maximumLength(10)
                    .nullable(false),
            Employee.DEPARTMENT.column()
                    .nullable(false),
            Employee.DEPARTMENT_FK.foreignKey()
                    .caption(Employee.DEPARTMENT_FK.name()),
            Employee.JOB.item(
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
            .tableName("scott.emp")
            .orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
            .stringFactory(Employee.NAME)
            .caption("Employee"));
  }
}
