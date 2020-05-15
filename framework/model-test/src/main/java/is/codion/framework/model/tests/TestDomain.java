/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.model.tests;

import dev.codion.common.item.Item;
import dev.codion.framework.domain.Domain;
import dev.codion.framework.domain.entity.StringProvider;

import java.sql.Types;
import java.util.List;

import static dev.codion.common.item.Items.item;
import static dev.codion.framework.domain.entity.KeyGenerators.increment;
import static dev.codion.framework.domain.entity.OrderBy.orderBy;
import static dev.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

public final class TestDomain extends Domain {

  public TestDomain() {
    master();
    detail();
    department();
    employee();
  }

  public static final String T_MASTER = "domain.master_entity";
  public static final String MASTER_ID = "id";
  public static final String MASTER_NAME = "name";
  public static final String MASTER_CODE = "code";

  void master() {
    define(T_MASTER,
            primaryKeyProperty(MASTER_ID, Types.BIGINT),
            columnProperty(MASTER_NAME, Types.VARCHAR),
            columnProperty(MASTER_CODE, Types.INTEGER))
            .comparator((o1, o2) -> {//keep like this for equality test in SwingEntityTableModelTest.testSortComparator()
              final Integer code1 = o1.getInteger(MASTER_CODE);
              final Integer code2 = o2.getInteger(MASTER_CODE);

              return code1.compareTo(code2);
            })
            .stringProvider(new StringProvider(MASTER_NAME));
  }

  public static final String T_DETAIL = "domain.detail_entity";
  public static final String DETAIL_ID = "id";
  public static final String DETAIL_INT = "int";
  public static final String DETAIL_DOUBLE = "double";
  public static final String DETAIL_STRING = "string";
  public static final String DETAIL_DATE = "date";
  public static final String DETAIL_TIMESTAMP = "timestamp";
  public static final String DETAIL_BOOLEAN = "boolean";
  public static final String DETAIL_BOOLEAN_NULLABLE = "boolean_nullable";
  public static final String DETAIL_MASTER_ID = "master_id";
  public static final String DETAIL_MASTER_FK = "master_fk";
  public static final String DETAIL_MASTER_NAME = "master_name";
  public static final String DETAIL_MASTER_CODE = "master_code";
  public static final String DETAIL_INT_VALUE_LIST = "int_value_list";
  public static final String DETAIL_INT_DERIVED = "int_derived";

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  private static final List<Item> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    define(T_DETAIL,
            primaryKeyProperty(DETAIL_ID, Types.BIGINT),
            columnProperty(DETAIL_INT, Types.INTEGER, DETAIL_INT),
            columnProperty(DETAIL_DOUBLE, Types.DOUBLE, DETAIL_DOUBLE),
            columnProperty(DETAIL_STRING, Types.VARCHAR, "Detail string"),
            columnProperty(DETAIL_DATE, Types.DATE, DETAIL_DATE),
            columnProperty(DETAIL_TIMESTAMP, Types.TIMESTAMP, DETAIL_TIMESTAMP),
            columnProperty(DETAIL_BOOLEAN, Types.BOOLEAN, DETAIL_BOOLEAN)
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            columnProperty(DETAIL_BOOLEAN_NULLABLE, Types.BOOLEAN, DETAIL_BOOLEAN_NULLABLE)
                    .defaultValue(true),
            foreignKeyProperty(DETAIL_MASTER_FK, DETAIL_MASTER_FK, T_MASTER,
                    columnProperty(DETAIL_MASTER_ID, Types.BIGINT)),
            denormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_MASTER_FK,
                    getDefinition(T_MASTER).getProperty(MASTER_NAME), DETAIL_MASTER_NAME),
            denormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_MASTER_FK,
                    getDefinition(T_MASTER).getProperty(MASTER_CODE), DETAIL_MASTER_CODE),
            valueListProperty(DETAIL_INT_VALUE_LIST, Types.INTEGER, DETAIL_INT_VALUE_LIST, ITEMS),
            derivedProperty(DETAIL_INT_DERIVED, Types.INTEGER, DETAIL_INT_DERIVED, linkedValues -> {
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
  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID)
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .orderBy(orderBy().ascending(DEPARTMENT_NAME))
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .caption("Department");
  }

  public static final String T_EMP = "scott.emp";
  public static final String EMP_ID = "empno";
  public static final String EMP_NAME = "ename";
  public static final String EMP_JOB = "job";
  public static final String EMP_MGR = "mgr";
  public static final String EMP_HIREDATE = "hiredate";
  public static final String EMP_SALARY = "sal";
  public static final String EMP_COMMISSION = "comm";
  public static final String EMP_DEPARTMENT = "deptno";
  public static final String EMP_DEPARTMENT_FK = "dept_fk";
  public static final String EMP_MGR_FK = "mgr_fk";
  public static final String EMP_DEPARTMENT_LOCATION = "location";

  public static final String EMP_CONDITION_1_ID = "condition1Id";
  public static final String EMP_CONDITION_2_ID = "condition2Id";
  public static final String EMP_CONDITION_3_ID = "condition3Id";

  /**
   * Otherwise we'd depend on java.awt.Color
   */
  public static final Object CYAN = new Object();

  void employee() {
    define(T_EMP,
            primaryKeyProperty(EMP_ID, Types.INTEGER, EMP_ID),
            columnProperty(EMP_NAME, Types.VARCHAR, EMP_NAME)
                    .searchProperty(true).maximumLength(10).nullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK, T_DEPARTMENT,
                    columnProperty(EMP_DEPARTMENT))
                    .nullable(false),
            valueListProperty(EMP_JOB, Types.VARCHAR, EMP_JOB,
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(EMP_SALARY, Types.DOUBLE, EMP_SALARY)
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION)
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK, T_EMP,
                    columnProperty(EMP_MGR)),
            columnProperty(EMP_HIREDATE, Types.DATE, EMP_HIREDATE)
                    .nullable(false),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION).preferredColumnWidth(100))
            .stringProvider(new StringProvider(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .orderBy(orderBy().ascending(EMP_DEPARTMENT, EMP_NAME))
            .conditionProvider(EMP_CONDITION_1_ID, (propertyIds, values) -> "1 = 2")
            .conditionProvider(EMP_CONDITION_2_ID, (propertyIds, values) -> "1 = 1")
            .conditionProvider(EMP_CONDITION_3_ID, (propertyIds, values) -> " ename = 'CLARK'")
            .caption("Employee")
            .colorProvider((entity, property) -> {
              if (property.is(EMP_JOB) && "MANAGER".equals(entity.get(EMP_JOB))) {
                return CYAN;
              }

              return null;
            });
  }
}
