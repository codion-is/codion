/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model.test;

import is.codion.common.item.Item;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.property.Property.*;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

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
            columnProperty(Master.NAME),
            columnProperty(Master.CODE))
            .comparator((o1, o2) -> {//keep like this for equality test in SwingEntityTableModelTest.testSortComparator()
              Integer code1 = o1.get(Master.CODE);
              Integer code2 = o2.get(Master.CODE);

              return code1.compareTo(code2);
            })
            .stringFactory(Master.NAME));
  }

  public interface Detail {
    EntityType TYPE = DOMAIN.entityType("domain.detail_entity");

    Attribute<Long> ID = TYPE.longAttribute("id");
    Attribute<Integer> INT = TYPE.integerAttribute("int");
    Attribute<Double> DOUBLE = TYPE.doubleAttribute("double");
    Attribute<String> STRING = TYPE.stringAttribute("string");
    Attribute<LocalDate> DATE = TYPE.localDateAttribute("date");
    Attribute<LocalDateTime> TIMESTAMP = TYPE.localDateTimeAttribute("timestamp");
    Attribute<Boolean> BOOLEAN = TYPE.booleanAttribute("boolean");
    Attribute<Boolean> BOOLEAN_NULLABLE = TYPE.booleanAttribute("boolean_nullable");
    Attribute<Long> MASTER_ID = TYPE.longAttribute("master_id");
    Attribute<String> MASTER_NAME = TYPE.stringAttribute("master_name");
    Attribute<Integer> MASTER_CODE = TYPE.integerAttribute("master_code");
    Attribute<Integer> INT_VALUE_LIST = TYPE.integerAttribute("int_value_list");
    Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");

    ForeignKey MASTER_FK = TYPE.foreignKey("master_fk", MASTER_ID, Master.ID);
  }

  private static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    add(definition(
            primaryKeyProperty(Detail.ID),
            columnProperty(Detail.INT, Detail.INT.name()),
            columnProperty(Detail.DOUBLE, Detail.DOUBLE.name()),
            columnProperty(Detail.STRING, "Detail string"),
            columnProperty(Detail.DATE, Detail.DATE.name()),
            columnProperty(Detail.TIMESTAMP, Detail.TIMESTAMP.name()),
            columnProperty(Detail.BOOLEAN, Detail.BOOLEAN.name())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            columnProperty(Detail.BOOLEAN_NULLABLE, Detail.BOOLEAN_NULLABLE.name())
                    .defaultValue(true),
            columnProperty(Detail.MASTER_ID),
            foreignKeyProperty(Detail.MASTER_FK, Detail.MASTER_FK.name()),
            denormalizedViewProperty(Detail.MASTER_NAME, Detail.MASTER_NAME.name(), Detail.MASTER_FK, Master.NAME),
            denormalizedViewProperty(Detail.MASTER_CODE, Detail.MASTER_CODE.name(), Detail.MASTER_FK, Master.CODE),
            itemProperty(Detail.INT_VALUE_LIST, Detail.INT_VALUE_LIST.name(), ITEMS),
            derivedProperty(Detail.INT_DERIVED, Detail.INT_DERIVED.name(), linkedValues -> {
              Integer intValue = linkedValues.get(Detail.INT);
              if (intValue == null) {
                return null;
              }

              return intValue * 10;
            }, Detail.INT))
            .selectTableName(DETAIL_SELECT_TABLE_NAME)
            .orderBy(ascending(Detail.STRING))
            .smallDataset(true)
            .stringFactory(Detail.STRING));
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("scott.dept");

    Attribute<Integer> ID = TYPE.integerAttribute("deptno");
    Attribute<String> LOCATION = TYPE.stringAttribute("loc");
    Attribute<String> NAME = TYPE.stringAttribute("dname");
  }

  void department() {
    add(definition(
            primaryKeyProperty(Department.ID, Department.ID.name())
                    .updatable(true).nullable(false),
            columnProperty(Department.NAME, Department.NAME.name())
                    .searchProperty(true)
                    .preferredColumnWidth(120)
                    .maximumLength(14)
                    .nullable(false),
            columnProperty(Department.LOCATION, Department.LOCATION.name())
                    .preferredColumnWidth(150)
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

    ConditionType CONDITION_1_TYPE = TYPE.conditionType("condition1Id");
    ConditionType CONDITION_2_TYPE = TYPE.conditionType("condition2Id");
    ConditionType CONDITION_3_TYPE = TYPE.conditionType("condition3Id");
  }

  void employee() {
    add(definition(
            primaryKeyProperty(Employee.ID, Employee.ID.name()),
            columnProperty(Employee.NAME, Employee.NAME.name())
                    .searchProperty(true)
                    .maximumLength(10)
                    .nullable(false),
            columnProperty(Employee.DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(Employee.DEPARTMENT_FK, Employee.DEPARTMENT_FK.name())
                    .selectAttributes(Department.NAME),
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
            denormalizedViewProperty(Employee.DEPARTMENT_LOCATION, Department.LOCATION.name(), Employee.DEPARTMENT_FK, Department.LOCATION)
                    .preferredColumnWidth(100))
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
}
