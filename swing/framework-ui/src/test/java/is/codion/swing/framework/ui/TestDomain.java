/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.item.Item;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.property.Property.*;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

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
    add(definition(
            primaryKeyProperty(Master.ID),
            columnProperty(Master.NAME)
                    .searchProperty(true),
            columnProperty(Master.CODE))
            .comparator(Comparator.comparing(o -> o.get(Master.CODE)))
            .stringFactory(Master.NAME));
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
    Column<Integer> INT_VALUE_LIST = TYPE.integerColumn("int_value_list");
    Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");
    Column<EnumType> ENUM_TYPE = TYPE.column("enum_type", EnumType.class);

    enum EnumType {
      ONE, TWO, THREE
    }
  }

  private static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    add(definition(
            primaryKeyProperty(Detail.ID),
            columnProperty(Detail.INT, Detail.INT.name())
                    .valueRange(-10_000, 10_000),
            columnProperty(Detail.DOUBLE, Detail.DOUBLE.name())
                    .valueRange(-10_000, 10_000),
            columnProperty(Detail.BIG_DECIMAL, Detail.BIG_DECIMAL.name()),
            columnProperty(Detail.STRING, "Detail string"),
            columnProperty(Detail.DATE, Detail.DATE.name()),
            columnProperty(Detail.TIME, Detail.TIME.name()),
            columnProperty(Detail.TIMESTAMP, Detail.TIMESTAMP.name()),
            columnProperty(Detail.OFFSET, Detail.OFFSET.name()),
            columnProperty(Detail.BOOLEAN, Detail.BOOLEAN.name())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            columnProperty(Detail.BOOLEAN_NULLABLE, Detail.BOOLEAN_NULLABLE.name())
                    .defaultValue(true),
            columnProperty(Detail.MASTER_ID),
            foreignKeyProperty(Detail.MASTER_FK, Detail.MASTER_FK.name()),
            columnProperty(Detail.DETAIL_ID),
            foreignKeyProperty(Detail.DETAIL_FK, Detail.DETAIL_FK.name()),
            denormalizedProperty(Detail.MASTER_NAME, Detail.MASTER_NAME.name(), Detail.MASTER_FK, Master.NAME),
            denormalizedProperty(Detail.MASTER_CODE, Detail.MASTER_CODE.name(), Detail.MASTER_FK, Master.CODE),
            itemProperty(Detail.INT_VALUE_LIST, Detail.INT_VALUE_LIST.name(), ITEMS),
            derivedProperty(Detail.INT_DERIVED, Detail.INT_DERIVED.name(), linkedValues -> {
              Integer intValue = linkedValues.get(Detail.INT);
              if (intValue == null) {
                return null;
              }

              return intValue * 10;
            }, Detail.INT),
            columnProperty(Detail.ENUM_TYPE))
            .selectTableName(DETAIL_SELECT_TABLE_NAME)
            .orderBy(ascending(Detail.STRING))
            .smallDataset(true)
            .stringFactory(Detail.STRING));
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("scott.dept");

    Column<Integer> ID = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");
  }

  void department() {
    add(definition(
            primaryKeyProperty(Department.ID, Department.ID.name())
                    .updatable(true)
                    .nullable(false),
            columnProperty(Department.NAME, Department.NAME.name())
                    .searchProperty(true)
                    .maximumLength(14)
                    .nullable(false),
            columnProperty(Department.LOCATION, Department.LOCATION.name())
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
  }

  /**
   * Otherwise we'd depend on java.awt.Color
   */
  public static final Object CYAN = new Object();

  void employee() {
    add(definition(
            primaryKeyProperty(Employee.ID, Employee.ID.name()),
            columnProperty(Employee.NAME, Employee.NAME.name())
                    .searchProperty(true)
                    .maximumLength(10)
                    .nullable(false),
            columnProperty(Employee.DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(Employee.DEPARTMENT_FK, Employee.DEPARTMENT_FK.name()),
            itemProperty(Employee.JOB, Employee.JOB.name(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(Employee.SALARY, Employee.SALARY.name())
                    .nullable(false)
                    .valueRange(1000, 10000)
                    .maximumFractionDigits(2),
            columnProperty(Employee.COMMISSION, Employee.COMMISSION.name())
                    .valueRange(100, 2000)
                    .maximumFractionDigits(2),
            columnProperty(Employee.MGR),
            foreignKeyProperty(Employee.MGR_FK, Employee.MGR_FK.name()),
            columnProperty(Employee.HIREDATE, Employee.HIREDATE.name())
                    .nullable(false),
            denormalizedProperty(Employee.DEPARTMENT_LOCATION, Department.LOCATION.name(), Employee.DEPARTMENT_FK, Department.LOCATION))
            .stringFactory(Employee.NAME)
            .keyGenerator(increment("scott.emp", "empno"))
            .orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
            .caption("Employee")
            .backgroundColorProvider((entity, attribute) -> {
              if (attribute.equals(Employee.JOB) && "MANAGER".equals(entity.get(Employee.JOB))) {
                return CYAN;
              }

              return null;
            }));
  }
}
