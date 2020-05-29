/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.item.Item;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;

import java.awt.Color;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
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
  public static final Attribute<Integer> MASTER_ID = attribute("id");
  public static final Attribute<String> MASTER_NAME = attribute("name");
  public static final Attribute<Integer> MASTER_CODE = attribute("code");

  void master() {
    define(T_MASTER,
            primaryKeyProperty(MASTER_ID, Types.BIGINT),
            columnProperty(MASTER_NAME, Types.VARCHAR),
            columnProperty(MASTER_CODE, Types.INTEGER))
            .comparator(Comparator.comparing(o -> o.get(MASTER_CODE)))
            .stringProvider(new StringProvider(MASTER_NAME));
  }

  public static final Attribute<Integer> DETAIL_ID = attribute("id");
  public static final Attribute<Integer> DETAIL_INT = attribute("int");
  public static final Attribute<Double> DETAIL_DOUBLE = attribute("double");
  public static final Attribute<String> DETAIL_STRING = attribute("string");
  public static final Attribute<LocalDate> DETAIL_DATE = attribute("date");
  public static final Attribute<LocalDateTime> DETAIL_TIMESTAMP = attribute("timestamp");
  public static final Attribute<Boolean> DETAIL_BOOLEAN = attribute("boolean");
  public static final Attribute<Boolean> DETAIL_BOOLEAN_NULLABLE = attribute("boolean_nullable");
  public static final Attribute<Integer> DETAIL_MASTER_ID = attribute("master_id");
  public static final Attribute<Entity> DETAIL_MASTER_FK = attribute("master_fk");
  public static final Attribute<String> DETAIL_MASTER_NAME = attribute("master_name");
  public static final Attribute<Integer> DETAIL_MASTER_CODE = attribute("master_code");
  public static final Attribute<Integer> DETAIL_INT_VALUE_LIST = attribute("int_value_list");
  public static final Attribute<Integer> DETAIL_INT_DERIVED = attribute("int_derived");

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  public static final String T_DETAIL = "domain.detail_entity";

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
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
                    columnProperty(DETAIL_MASTER_ID, Types.BIGINT)),
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
            .smallDataset(true)
            .stringProvider(new StringProvider(DETAIL_STRING));
  }

  public static final Attribute<Integer> DEPARTMENT_ID = attribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = attribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = attribute("loc");

  public static final String T_DEPARTMENT = "scott.dept";

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID.getId())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME.getId())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION.getId())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .caption("Department");
  }

  public static final Attribute<Integer> EMP_ID = attribute("empno");
  public static final Attribute<String> EMP_NAME = attribute("ename");
  public static final Attribute<String> EMP_JOB = attribute("job");
  public static final Attribute<Integer> EMP_MGR = attribute("mgr");
  public static final Attribute<LocalDate> EMP_HIREDATE = attribute("hiredate");
  public static final Attribute<Double> EMP_SALARY = attribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = attribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = attribute("deptno");
  public static final Attribute<Entity> EMP_DEPARTMENT_FK = attribute("dept_fk");
  public static final Attribute<Entity> EMP_MGR_FK = attribute("mgr_fk");
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = attribute("location");
  public static final String T_EMP = "scott.emp";

  void employee() {
    define(T_EMP,
            primaryKeyProperty(EMP_ID, Types.INTEGER, EMP_ID.getId()),
            columnProperty(EMP_NAME, Types.VARCHAR, EMP_NAME.getId())
                    .searchProperty(true).maximumLength(10).nullable(false),
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
            .stringProvider(new StringProvider(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .caption("Employee")
            .colorProvider((entity, property) -> {
              if (property.is(EMP_JOB) && "MANAGER".equals(entity.get(EMP_JOB))) {
                return Color.CYAN;
              }

              return null;
            });
  }
}
