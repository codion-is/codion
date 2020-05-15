/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db;

import dev.codion.common.item.Item;
import dev.codion.framework.domain.Domain;
import dev.codion.framework.domain.entity.StringProvider;

import java.sql.Types;
import java.util.Comparator;
import java.util.List;

import static dev.codion.common.item.Items.item;
import static dev.codion.framework.domain.entity.OrderBy.orderBy;
import static dev.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

public final class TestDomain extends Domain {

  public TestDomain() {
    superEntity();
    master();
    detail();
    department();
    employee();
  }

  public static final String T_SUPER = "db.super_entity";
  public static final String SUPER_ID = "id";

  void superEntity() {
    define(T_SUPER,
            primaryKeyProperty(SUPER_ID));
  }

  public static final String T_MASTER = "db.master_entity";
  public static final String MASTER_ID_1 = "id";
  public static final String MASTER_ID_2 = "id2";
  public static final String MASTER_SUPER_ID = "super_id";
  public static final String MASTER_SUPER_FK = "super_fk";
  public static final String MASTER_NAME = "name";
  public static final String MASTER_CODE = "code";

  void master() {
    define(T_MASTER,
            columnProperty(MASTER_ID_1).primaryKeyIndex(0),
            columnProperty(MASTER_ID_2).primaryKeyIndex(1),
            foreignKeyProperty(MASTER_SUPER_FK, "Super", T_SUPER,
                    columnProperty(MASTER_SUPER_ID)),
            columnProperty(MASTER_NAME, Types.VARCHAR),
            columnProperty(MASTER_CODE, Types.INTEGER))
            .comparator(Comparator.comparing(o -> o.getInteger(MASTER_CODE)))
            .stringProvider(new StringProvider(MASTER_NAME));
  }

  public static final String DETAIL_ID = "id";
  public static final String DETAIL_INT = "int";
  public static final String DETAIL_DOUBLE = "double";
  public static final String DETAIL_STRING = "string";
  public static final String DETAIL_DATE = "date";
  public static final String DETAIL_TIMESTAMP = "timestamp";
  public static final String DETAIL_BOOLEAN = "boolean";
  public static final String DETAIL_BOOLEAN_NULLABLE = "boolean_nullable";
  public static final String DETAIL_MASTER_ID_1 = "master_id";
  public static final String DETAIL_MASTER_ID_2 = "master_id_2";
  public static final String DETAIL_MASTER_FK = "master_fk";
  public static final String DETAIL_MASTER_NAME = "master_name";
  public static final String DETAIL_MASTER_CODE = "master_code";
  public static final String DETAIL_INT_VALUE_LIST = "int_value_list";
  public static final String DETAIL_INT_DERIVED = "int_derived";

  public static final String DETAIL_SELECT_TABLE_NAME = "db.entity_test_select";

  public static final String T_DETAIL = "db.detail_entity";

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
                    asList(columnProperty(DETAIL_MASTER_ID_1),
                            columnProperty(DETAIL_MASTER_ID_2))),
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

  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";

  public static final String T_DEPARTMENT = "db.scott.dept";

  public static final String DEPARTMENT_CONDITION_ID = "condition";
  public static final String DEPARTMENT_NAME_NOT_NULL_CONDITION_ID = "departmentNameNotNull";

  void department() {
    define(T_DEPARTMENT, "scott.dept",
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID)
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .orderBy(orderBy().ascending(DEPARTMENT_NAME))
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .conditionProvider(DEPARTMENT_CONDITION_ID, (propertyIds, values) -> {
              final StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .conditionProvider(DEPARTMENT_NAME_NOT_NULL_CONDITION_ID, (propertyIds, values) -> "department name is not null")
            .caption("Department");
  }

  public static final String T_EMP = "db.scott.emp";
  public static final String EMP_ID = "emp_id";
  public static final String EMP_NAME = "emp_name";
  public static final String EMP_JOB = "job";
  public static final String EMP_MGR = "mgr";
  public static final String EMP_HIREDATE = "hiredate";
  public static final String EMP_SALARY = "sal";
  public static final String EMP_COMMISSION = "comm";
  public static final String EMP_DEPARTMENT = "deptno";
  public static final String EMP_DEPARTMENT_FK = "dept_fk";
  public static final String EMP_MGR_FK = "mgr_fk";
  public static final String EMP_DEPARTMENT_LOCATION = "location";

  void employee() {
    define(T_EMP, "scott.emp",
            primaryKeyProperty(EMP_ID, Types.INTEGER, EMP_ID).columnName("empno"),
            columnProperty(EMP_NAME, Types.VARCHAR, EMP_NAME)
                    .searchProperty(true).columnName("ename").maximumLength(10).nullable(false),
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
            .orderBy(orderBy().ascending(EMP_DEPARTMENT, EMP_NAME))
            .stringProvider(new StringProvider(EMP_NAME))
            .caption("Employee");
  }
}
