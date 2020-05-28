/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.item.Item;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
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
  public static final Attribute<Integer> SUPER_ID = attribute("id");

  void superEntity() {
    define(T_SUPER,
            primaryKeyProperty(SUPER_ID, Types.INTEGER));
  }

  public static final String T_MASTER = "db.master_entity";
  public static final Attribute<Integer> MASTER_ID_1 = attribute("id");
  public static final Attribute<Integer> MASTER_ID_2 = attribute("id2");
  public static final Attribute<Integer> MASTER_SUPER_ID = attribute("super_id");
  public static final Attribute<Entity> MASTER_SUPER_FK = attribute("super_fk");
  public static final Attribute<String> MASTER_NAME = attribute("name");
  public static final Attribute<Integer> MASTER_CODE = attribute("code");

  void master() {
    define(T_MASTER,
            columnProperty(MASTER_ID_1, Types.INTEGER).primaryKeyIndex(0),
            columnProperty(MASTER_ID_2, Types.INTEGER).primaryKeyIndex(1),
            foreignKeyProperty(MASTER_SUPER_FK, "Super", T_SUPER,
                    columnProperty(MASTER_SUPER_ID, Types.INTEGER)),
            columnProperty(MASTER_NAME, Types.VARCHAR),
            columnProperty(MASTER_CODE, Types.INTEGER))
            .comparator(Comparator.comparing(o -> o.get(MASTER_CODE)))
            .stringProvider(new StringProvider(MASTER_NAME));
  }

  public static final Attribute<Long> DETAIL_ID = attribute("id");
  public static final Attribute<Integer> DETAIL_INT = attribute("int");
  public static final Attribute<Double> DETAIL_DOUBLE = attribute("double");
  public static final Attribute<String> DETAIL_STRING = attribute("string");
  public static final Attribute<LocalDate> DETAIL_DATE = attribute("date");
  public static final Attribute<LocalDateTime> DETAIL_TIMESTAMP = attribute("timestamp");
  public static final Attribute<Boolean> DETAIL_BOOLEAN = attribute("boolean");
  public static final Attribute<Boolean> DETAIL_BOOLEAN_NULLABLE = attribute("boolean_nullable");
  public static final Attribute<Integer> DETAIL_MASTER_ID_1 = attribute("master_id");
  public static final Attribute<Integer> DETAIL_MASTER_ID_2 = attribute("master_id_2");
  public static final Attribute<Entity> DETAIL_MASTER_FK = attribute("master_fk");
  public static final Attribute<String> DETAIL_MASTER_NAME = attribute("master_name");
  public static final Attribute<Integer> DETAIL_MASTER_CODE = attribute("master_code");
  public static final Attribute<Integer> DETAIL_INT_VALUE_LIST = attribute("int_value_list");
  public static final Attribute<Integer> DETAIL_INT_DERIVED = attribute("int_derived");

  public static final String DETAIL_SELECT_TABLE_NAME = "db.entity_test_select";

  public static final String T_DETAIL = "db.detail_entity";

  private static final List<Item> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    define(T_DETAIL,
            primaryKeyProperty(DETAIL_ID, Types.BIGINT),
            columnProperty(DETAIL_INT, Types.INTEGER, DETAIL_INT.getId()),
            columnProperty(DETAIL_DOUBLE, Types.DOUBLE, DETAIL_DOUBLE.getId()),
            columnProperty(DETAIL_STRING, Types.VARCHAR, "Detail string"),
            columnProperty(DETAIL_DATE, Types.DATE, DETAIL_DATE.getId()),
            columnProperty(DETAIL_TIMESTAMP, Types.TIMESTAMP, DETAIL_TIMESTAMP.getId()),
            columnProperty(DETAIL_BOOLEAN, Types.BOOLEAN, DETAIL_BOOLEAN.getId())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            columnProperty(DETAIL_BOOLEAN_NULLABLE, Types.BOOLEAN, DETAIL_BOOLEAN_NULLABLE.getId())
                    .defaultValue(true),
            foreignKeyProperty(DETAIL_MASTER_FK, DETAIL_MASTER_FK.getId(), T_MASTER,
                    asList(columnProperty(DETAIL_MASTER_ID_1, Types.INTEGER),
                            columnProperty(DETAIL_MASTER_ID_2, Types.INTEGER))),
            denormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_MASTER_FK,
                    getDefinition(T_MASTER).getProperty(MASTER_NAME), DETAIL_MASTER_NAME.getId()),
            denormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_MASTER_FK,
                    getDefinition(T_MASTER).getProperty(MASTER_CODE), DETAIL_MASTER_CODE.getId()),
            valueListProperty(DETAIL_INT_VALUE_LIST, Types.INTEGER, DETAIL_INT_VALUE_LIST.getId(), ITEMS),
            derivedProperty(DETAIL_INT_DERIVED, Types.INTEGER, DETAIL_INT_DERIVED.getId(), linkedValues -> {
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

  public static final Attribute<Integer> DEPARTMENT_ID = attribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = attribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = attribute("loc");
  public static final Attribute<Boolean> DEPARTMENT_ACTIVE = attribute("active");
  public static final Attribute<byte[]> DEPARTMENT_DATA = attribute("data");

  public static final String T_DEPARTMENT = "db.scott.dept";

  public static final String DEPARTMENT_CONDITION_ID = "condition";
  public static final String DEPARTMENT_NAME_NOT_NULL_CONDITION_ID = "departmentNameNotNull";

  void department() {
    define(T_DEPARTMENT, "scott.dept",
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID.getId())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME.getId())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION.getId())
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
  public static final Attribute<Integer> EMP_ID = attribute("emp_id");
  public static final Attribute<String> EMP_NAME = attribute("emp_name");
  public static final Attribute<String> EMP_JOB = attribute("job");
  public static final Attribute<Integer> EMP_MGR = attribute("mgr");
  public static final Attribute<LocalDateTime> EMP_HIREDATE = attribute("hiredate");
  public static final Attribute<Double> EMP_SALARY = attribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = attribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = attribute("deptno");
  public static final Attribute<Entity> EMP_DEPARTMENT_FK = attribute("dept_fk");
  public static final Attribute<Entity> EMP_MGR_FK = attribute("mgr_fk");
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = attribute("location");

  void employee() {
    define(T_EMP, "scott.emp",
            primaryKeyProperty(EMP_ID, Types.INTEGER, EMP_ID.getId()).columnName("empno"),
            columnProperty(EMP_NAME, Types.VARCHAR, EMP_NAME.getId())
                    .searchProperty(true).columnName("ename").maximumLength(10).nullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK.getId(), T_DEPARTMENT,
                    columnProperty(EMP_DEPARTMENT, Types.INTEGER))
                    .nullable(false),
            valueListProperty(EMP_JOB, Types.VARCHAR, EMP_JOB.getId(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(EMP_SALARY, Types.DOUBLE, EMP_SALARY.getId())
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION.getId())
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK.getId(), T_EMP,
                    columnProperty(EMP_MGR, Types.INTEGER)),
            columnProperty(EMP_HIREDATE, Types.DATE, EMP_HIREDATE.getId())
                    .nullable(false),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION.getId()).preferredColumnWidth(100))
            .orderBy(orderBy().ascending(EMP_DEPARTMENT, EMP_NAME))
            .stringProvider(new StringProvider(EMP_NAME))
            .caption("Employee");
  }
}
