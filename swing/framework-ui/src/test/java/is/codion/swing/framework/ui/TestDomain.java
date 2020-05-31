/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.item.Item;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.Attributes;
import is.codion.framework.domain.property.EntityAttribute;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

public final class TestDomain extends Domain {

  public TestDomain() {
    master();
    detail();
    department();
    employee();
  }

  public static final String T_MASTER = "domain.master_entity";
  public static final Attribute<Long> MASTER_ID = Attributes.longAttribute("id", T_MASTER);
  public static final Attribute<String> MASTER_NAME = Attributes.stringAttribute("name", T_MASTER);
  public static final Attribute<Integer> MASTER_CODE = Attributes.integerAttribute("code", T_MASTER);

  void master() {
    define(T_MASTER,
            primaryKeyProperty(MASTER_ID),
            columnProperty(MASTER_NAME),
            columnProperty(MASTER_CODE))
            .comparator(Comparator.comparing(o -> o.get(MASTER_CODE)))
            .stringProvider(new StringProvider(MASTER_NAME));
  }

  public static final String T_DETAIL = "domain.detail_entity";
  public static final Attribute<Long> DETAIL_ID = Attributes.longAttribute("id", T_DETAIL);
  public static final Attribute<Integer> DETAIL_INT = Attributes.integerAttribute("int", T_DETAIL);
  public static final Attribute<Double> DETAIL_DOUBLE = Attributes.doubleAttribute("double", T_DETAIL);
  public static final Attribute<String> DETAIL_STRING = Attributes.stringAttribute("string", T_DETAIL);
  public static final Attribute<LocalDate> DETAIL_DATE = Attributes.localDateAttribute("date", T_DETAIL);
  public static final Attribute<LocalDateTime> DETAIL_TIMESTAMP = Attributes.localDateTimeAttribute("timestamp", T_DETAIL);
  public static final Attribute<Boolean> DETAIL_BOOLEAN = Attributes.booleanAttribute("boolean", T_DETAIL);
  public static final Attribute<Boolean> DETAIL_BOOLEAN_NULLABLE = Attributes.booleanAttribute("boolean_nullable", T_DETAIL);
  public static final Attribute<Long> DETAIL_MASTER_ID = Attributes.longAttribute("master_id", T_DETAIL);
  public static final EntityAttribute DETAIL_MASTER_FK = Attributes.entityAttribute("master_fk", T_DETAIL);
  public static final Attribute<String> DETAIL_MASTER_NAME = Attributes.stringAttribute("master_name", T_DETAIL);
  public static final Attribute<Integer> DETAIL_MASTER_CODE = Attributes.integerAttribute("master_code", T_DETAIL);
  public static final Attribute<Integer> DETAIL_INT_VALUE_LIST = Attributes.integerAttribute("int_value_list", T_DETAIL);
  public static final Attribute<Integer> DETAIL_INT_DERIVED = Attributes.integerAttribute("int_derived", T_DETAIL);

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
            foreignKeyProperty(DETAIL_MASTER_FK, DETAIL_MASTER_FK.getName(), T_MASTER,
                    columnProperty(DETAIL_MASTER_ID)),
            denormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_MASTER_FK, MASTER_NAME, DETAIL_MASTER_NAME.getName()),
            denormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_MASTER_FK, MASTER_CODE, DETAIL_MASTER_CODE.getName()),
            valueListProperty(DETAIL_INT_VALUE_LIST, DETAIL_INT_VALUE_LIST.getName(), ITEMS),
            derivedProperty(DETAIL_INT_DERIVED, DETAIL_INT_DERIVED.getName(), linkedValues -> {
              final Integer intValue = (Integer) linkedValues.get(DETAIL_INT);
              if (intValue == null) {
                return null;
              }

              return intValue * 10;
            }, DETAIL_INT))
            .selectTableName(DETAIL_SELECT_TABLE_NAME)
            .orderBy(orderBy().ascending(DETAIL_STRING))
            .smallDataset(true)
            .stringProvider(new StringProvider(DETAIL_STRING));
  }

  public static final String T_DEPARTMENT = "scott.dept";
  public static final Attribute<Integer> DEPARTMENT_ID = Attributes.integerAttribute("deptno", T_DEPARTMENT);
  public static final Attribute<String> DEPARTMENT_NAME = Attributes.stringAttribute("dname", T_DEPARTMENT);
  public static final Attribute<String> DEPARTMENT_LOCATION = Attributes.stringAttribute("loc", T_DEPARTMENT);

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.getName())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .orderBy(orderBy().ascending(DEPARTMENT_NAME))
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .caption("Department");
  }

  public static final String T_EMP = "scott.emp";
  public static final Attribute<Integer> EMP_ID = Attributes.integerAttribute("empno", T_EMP);
  public static final Attribute<String> EMP_NAME = Attributes.stringAttribute("ename", T_EMP);
  public static final Attribute<String> EMP_JOB = Attributes.stringAttribute("job", T_EMP);
  public static final Attribute<Integer> EMP_MGR = Attributes.integerAttribute("mgr", T_EMP);
  public static final Attribute<LocalDate> EMP_HIREDATE = Attributes.localDateAttribute("hiredate", T_EMP);
  public static final Attribute<Double> EMP_SALARY = Attributes.doubleAttribute("sal", T_EMP);
  public static final Attribute<Double> EMP_COMMISSION = Attributes.doubleAttribute("comm", T_EMP);
  public static final Attribute<Integer> EMP_DEPARTMENT = Attributes.integerAttribute("deptno", T_EMP);
  public static final EntityAttribute EMP_DEPARTMENT_FK = Attributes.entityAttribute("dept_fk", T_EMP);
  public static final EntityAttribute EMP_MGR_FK = Attributes.entityAttribute("mgr_fk", T_EMP);
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = Attributes.stringAttribute("location", T_EMP);

  public static final String EMP_CONDITION_1_ID = "condition1Id";
  public static final String EMP_CONDITION_2_ID = "condition2Id";
  public static final String EMP_CONDITION_3_ID = "condition3Id";

  /**
   * Otherwise we'd depend on java.awt.Color
   */
  public static final Object CYAN = new Object();

  void employee() {
    define(T_EMP,
            primaryKeyProperty(EMP_ID, EMP_ID.getName()),
            columnProperty(EMP_NAME, EMP_NAME.getName())
                    .searchProperty(true).maximumLength(10).nullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK.getName(), T_DEPARTMENT,
                    columnProperty(EMP_DEPARTMENT))
                    .nullable(false),
            valueListProperty(EMP_JOB, EMP_JOB.getName(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(EMP_SALARY, EMP_SALARY.getName())
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, EMP_COMMISSION.getName())
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK.getName(), T_EMP,
                    columnProperty(EMP_MGR)),
            columnProperty(EMP_HIREDATE, EMP_HIREDATE.getName())
                    .nullable(false),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK, DEPARTMENT_LOCATION,
                    DEPARTMENT_LOCATION.getName()).preferredColumnWidth(100))
            .stringProvider(new StringProvider(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .orderBy(orderBy().ascending(EMP_DEPARTMENT, EMP_NAME))
            .conditionProvider(EMP_CONDITION_1_ID, (attributes, values) -> "1 = 2")
            .conditionProvider(EMP_CONDITION_2_ID, (attributes, values) -> "1 = 1")
            .conditionProvider(EMP_CONDITION_3_ID, (attributes, values) -> " ename = 'CLARK'")
            .caption("Employee")
            .colorProvider((entity, property) -> {
              if (property.is(EMP_JOB) && "MANAGER".equals(entity.get(EMP_JOB))) {
                return CYAN;
              }

              return null;
            });
  }
}
