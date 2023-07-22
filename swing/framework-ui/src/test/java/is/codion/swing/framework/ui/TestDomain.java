/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.item.Item;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
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

    Attribute<Long> ID = TYPE.longAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<Integer> CODE = TYPE.integerAttribute("code");
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

    Attribute<Long> ID = TYPE.longAttribute("id");
    Attribute<Integer> INT = TYPE.integerAttribute("int");
    Attribute<Double> DOUBLE = TYPE.doubleAttribute("double");
    Attribute<BigDecimal> BIG_DECIMAL = TYPE.bigDecimalAttribute("big_decimal");
    Attribute<String> STRING = TYPE.stringAttribute("string");
    Attribute<LocalTime> TIME = TYPE.localTimeAttribute("time");
    Attribute<LocalDate> DATE = TYPE.localDateAttribute("date");
    Attribute<LocalDateTime> TIMESTAMP = TYPE.localDateTimeAttribute("timestamp");
    Attribute<OffsetDateTime> OFFSET = TYPE.offsetDateTimeAttribute("offset");
    Attribute<Boolean> BOOLEAN = TYPE.booleanAttribute("boolean");
    Attribute<Boolean> BOOLEAN_NULLABLE = TYPE.booleanAttribute("boolean_nullable");
    Attribute<Long> MASTER_ID = TYPE.longAttribute("master_id");
    ForeignKey MASTER_FK = TYPE.foreignKey("master_fk", MASTER_ID, Master.ID);
    Attribute<Long> DETAIL_ID = TYPE.longAttribute("detail_id");
    ForeignKey DETAIL_FK = TYPE.foreignKey("detail_fk", DETAIL_ID, ID);
    Attribute<String> MASTER_NAME = TYPE.stringAttribute("master_name");
    Attribute<Integer> MASTER_CODE = TYPE.integerAttribute("master_code");
    Attribute<Integer> INT_VALUE_LIST = TYPE.integerAttribute("int_value_list");
    Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");
    Attribute<EnumType> ENUM_TYPE = TYPE.attribute("enum_type", EnumType.class);

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

    Attribute<Integer> ID = TYPE.integerAttribute("deptno");
    Attribute<String> NAME = TYPE.stringAttribute("dname");
    Attribute<String> LOCATION = TYPE.stringAttribute("loc");
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

    Attribute<Integer> ID = TYPE.integerAttribute("empno");
    Attribute<String> NAME = TYPE.stringAttribute("ename");
    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Integer> MGR = TYPE.integerAttribute("mgr");
    Attribute<LocalDate> HIREDATE = TYPE.localDateAttribute("hiredate");
    Attribute<Double> SALARY = TYPE.doubleAttribute("sal");
    Attribute<Double> COMMISSION = TYPE.doubleAttribute("comm");
    Attribute<Integer> DEPARTMENT = TYPE.integerAttribute("deptno");
    Attribute<String> DEPARTMENT_LOCATION = TYPE.stringAttribute("location");

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
