/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
import static is.codion.framework.domain.property.Properties.*;
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

  public static final EntityType T_MASTER = DOMAIN.entityType("domain.master_entity");
  public static final Attribute<Long> MASTER_ID = T_MASTER.longAttribute("id");
  public static final Attribute<String> MASTER_NAME = T_MASTER.stringAttribute("name");
  public static final Attribute<Integer> MASTER_CODE = T_MASTER.integerAttribute("code");

  void master() {
    define(T_MASTER,
            primaryKeyProperty(MASTER_ID),
            columnProperty(MASTER_NAME),
            columnProperty(MASTER_CODE))
            .comparator((o1, o2) -> {//keep like this for equality test in SwingEntityTableModelTest.testSortComparator()
              final Integer code1 = o1.get(MASTER_CODE);
              final Integer code2 = o2.get(MASTER_CODE);

              return code1.compareTo(code2);
            })
            .stringFactory(stringFactory(MASTER_NAME));
  }

  public static final EntityType T_DETAIL = DOMAIN.entityType("domain.detail_entity");
  public static final Attribute<Long> DETAIL_ID = T_DETAIL.longAttribute("id");
  public static final Attribute<Integer> DETAIL_INT = T_DETAIL.integerAttribute("int");
  public static final Attribute<Double> DETAIL_DOUBLE = T_DETAIL.doubleAttribute("double");
  public static final Attribute<String> DETAIL_STRING = T_DETAIL.stringAttribute("string");
  public static final Attribute<LocalDate> DETAIL_DATE = T_DETAIL.localDateAttribute("date");
  public static final Attribute<LocalDateTime> DETAIL_TIMESTAMP = T_DETAIL.localDateTimeAttribute("timestamp");
  public static final Attribute<Boolean> DETAIL_BOOLEAN = T_DETAIL.booleanAttribute("boolean");
  public static final Attribute<Boolean> DETAIL_BOOLEAN_NULLABLE = T_DETAIL.booleanAttribute("boolean_nullable");
  public static final Attribute<Long> DETAIL_MASTER_ID = T_DETAIL.longAttribute("master_id");
  public static final ForeignKey DETAIL_MASTER_FK = T_DETAIL.foreignKey("master_fk", DETAIL_MASTER_ID, MASTER_ID);
  public static final Attribute<String> DETAIL_MASTER_NAME = T_DETAIL.stringAttribute("master_name");
  public static final Attribute<Integer> DETAIL_MASTER_CODE = T_DETAIL.integerAttribute("master_code");
  public static final Attribute<Integer> DETAIL_INT_VALUE_LIST = T_DETAIL.integerAttribute("int_value_list");
  public static final Attribute<Integer> DETAIL_INT_DERIVED = T_DETAIL.integerAttribute("int_derived");

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    define(T_DETAIL,
            primaryKeyProperty(DETAIL_ID),
            columnProperty(DETAIL_INT, DETAIL_INT.getName()),
            columnProperty(DETAIL_DOUBLE, DETAIL_DOUBLE.getName()),
            columnProperty(DETAIL_STRING, "Detail string"),
            columnProperty(DETAIL_DATE, DETAIL_DATE.getName()),
            columnProperty(DETAIL_TIMESTAMP, DETAIL_TIMESTAMP.getName()),
            columnProperty(DETAIL_BOOLEAN, DETAIL_BOOLEAN.getName())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            columnProperty(DETAIL_BOOLEAN_NULLABLE, DETAIL_BOOLEAN_NULLABLE.getName())
                    .defaultValue(true),
            columnProperty(DETAIL_MASTER_ID),
            foreignKeyProperty(DETAIL_MASTER_FK, DETAIL_MASTER_FK.getName()),
            denormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_MASTER_NAME.getName(), DETAIL_MASTER_FK, MASTER_NAME),
            denormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_MASTER_CODE.getName(), DETAIL_MASTER_FK, MASTER_CODE),
            itemProperty(DETAIL_INT_VALUE_LIST, DETAIL_INT_VALUE_LIST.getName(), ITEMS),
            derivedProperty(DETAIL_INT_DERIVED, DETAIL_INT_DERIVED.getName(), linkedValues -> {
              final Integer intValue = linkedValues.get(DETAIL_INT);
              if (intValue == null) {
                return null;
              }

              return intValue * 10;
            }, DETAIL_INT))
            .selectTableName(DETAIL_SELECT_TABLE_NAME)
            .orderBy(orderBy().ascending(DETAIL_STRING))
            .smallDataset()
            .stringFactory(stringFactory(DETAIL_STRING));
  }

  public static final EntityType T_DEPARTMENT = DOMAIN.entityType("scott.dept");
  public static final Attribute<Integer> DEPARTMENT_ID = T_DEPARTMENT.integerAttribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = T_DEPARTMENT.stringAttribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = T_DEPARTMENT.stringAttribute("loc");

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.getName())
                    .searchProperty().preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset()
            .orderBy(orderBy().ascending(DEPARTMENT_NAME))
            .stringFactory(stringFactory(DEPARTMENT_NAME))
            .caption("Department");
  }

  public static final EntityType T_EMP = DOMAIN.entityType("scott.emp");
  public static final Attribute<Integer> EMP_ID = T_EMP.integerAttribute("empno");
  public static final Attribute<String> EMP_NAME = T_EMP.stringAttribute("ename");
  public static final Attribute<String> EMP_JOB = T_EMP.stringAttribute("job");
  public static final Attribute<Integer> EMP_MGR = T_EMP.integerAttribute("mgr");
  public static final Attribute<LocalDate> EMP_HIREDATE = T_EMP.localDateAttribute("hiredate");
  public static final Attribute<Double> EMP_SALARY = T_EMP.doubleAttribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = T_EMP.doubleAttribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = T_EMP.integerAttribute("deptno");
  public static final ForeignKey EMP_DEPARTMENT_FK = T_EMP.foreignKey("dept_fk", EMP_DEPARTMENT, DEPARTMENT_ID);
  public static final ForeignKey EMP_MGR_FK = T_EMP.foreignKey("mgr_fk", EMP_MGR, EMP_ID);
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = T_EMP.stringAttribute("location");

  public static final ConditionType EMP_CONDITION_1_TYPE = T_EMP.conditionType("condition1Id");
  public static final ConditionType EMP_CONDITION_2_TYPE = T_EMP.conditionType("condition2Id");
  public static final ConditionType EMP_CONDITION_3_TYPE = T_EMP.conditionType("condition3Id");

  /**
   * Otherwise we'd depend on java.awt.Color
   */
  public static final Object CYAN = new Object();

  void employee() {
    define(T_EMP,
            primaryKeyProperty(EMP_ID, EMP_ID.getName()),
            columnProperty(EMP_NAME, EMP_NAME.getName())
                    .searchProperty().maximumLength(10).nullable(false),
            columnProperty(EMP_DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK.getName())
                    .selectAttributes(DEPARTMENT_NAME),
            itemProperty(EMP_JOB, EMP_JOB.getName(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(),
            columnProperty(EMP_SALARY, EMP_SALARY.getName())
                    .nullable(false).range(1000, 10000).maximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, EMP_COMMISSION.getName())
                    .range(100, 2000).maximumFractionDigits(2),
            columnProperty(EMP_MGR),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK.getName()),
            columnProperty(EMP_HIREDATE, EMP_HIREDATE.getName())
                    .nullable(false),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName(), EMP_DEPARTMENT_FK, DEPARTMENT_LOCATION)
                    .preferredColumnWidth(100))
            .stringFactory(stringFactory(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .orderBy(orderBy().ascending(EMP_DEPARTMENT, EMP_NAME))
            .conditionProvider(EMP_CONDITION_1_TYPE, (attributes, values) -> "1 = 2")
            .conditionProvider(EMP_CONDITION_2_TYPE, (attributes, values) -> "1 = 1")
            .conditionProvider(EMP_CONDITION_3_TYPE, (attributes, values) -> " ename = 'CLARK'")
            .caption("Employee")
            .backgroundColorProvider((entity, attribute) -> {
              if (attribute.equals(EMP_JOB) && "MANAGER".equals(entity.get(EMP_JOB))) {
                return "#00ff00";//Color.GREEN
              }

              return null;
            });
  }
}
