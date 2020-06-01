/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.item.Item;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.attribute.EntityAttribute;
import is.codion.framework.domain.entity.EntityIdentity;
import is.codion.framework.domain.entity.StringProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.Entities.entityIdentity;
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

  public static final EntityIdentity T_SUPER = entityIdentity("db.super_entity");
  public static final Attribute<Integer> SUPER_ID = T_SUPER.integerAttribute("id");

  void superEntity() {
    define(T_SUPER,
            primaryKeyProperty(SUPER_ID));
  }

  public static final EntityIdentity T_MASTER = entityIdentity("db.master_entity");
  public static final Attribute<Integer> MASTER_ID_1 = T_MASTER.integerAttribute("id");
  public static final Attribute<Integer> MASTER_ID_2 = T_MASTER.integerAttribute("id2");
  public static final Attribute<Integer> MASTER_SUPER_ID = T_MASTER.integerAttribute("super_id");
  public static final EntityAttribute MASTER_SUPER_FK = T_MASTER.entityAttribute("super_fk");
  public static final Attribute<String> MASTER_NAME = T_MASTER.stringAttribute("name");
  public static final Attribute<Integer> MASTER_CODE = T_MASTER.integerAttribute("code");

  void master() {
    define(T_MASTER,
            columnProperty(MASTER_ID_1).primaryKeyIndex(0),
            columnProperty(MASTER_ID_2).primaryKeyIndex(1),
            foreignKeyProperty(MASTER_SUPER_FK, "Super", T_SUPER,
                    columnProperty(MASTER_SUPER_ID)),
            columnProperty(MASTER_NAME),
            columnProperty(MASTER_CODE))
            .comparator(Comparator.comparing(o -> o.get(MASTER_CODE)))
            .stringProvider(new StringProvider(MASTER_NAME));
  }

  public static final EntityIdentity T_DETAIL = entityIdentity("db.detail_entity");

  public static final Attribute<Long> DETAIL_ID = T_DETAIL.longAttribute("id");
  public static final Attribute<Integer> DETAIL_INT = T_DETAIL.integerAttribute("int");
  public static final Attribute<Double> DETAIL_DOUBLE = T_DETAIL.doubleAttribute("double");
  public static final Attribute<String> DETAIL_STRING = T_DETAIL.stringAttribute("string");
  public static final Attribute<LocalDate> DETAIL_DATE = T_DETAIL.localDateAttribute("date");
  public static final Attribute<LocalDateTime> DETAIL_TIMESTAMP = T_DETAIL.localDateTimeAttribute("timestamp");
  public static final Attribute<Boolean> DETAIL_BOOLEAN = T_DETAIL.booleanAttribute("boolean");
  public static final Attribute<Boolean> DETAIL_BOOLEAN_NULLABLE = T_DETAIL.booleanAttribute("boolean_nullable");
  public static final Attribute<Integer> DETAIL_MASTER_ID_1 = T_DETAIL.integerAttribute("master_id");
  public static final Attribute<Integer> DETAIL_MASTER_ID_2 = T_DETAIL.integerAttribute("master_id_2");
  public static final EntityAttribute DETAIL_MASTER_FK = T_DETAIL.entityAttribute("master_fk");
  public static final Attribute<String> DETAIL_MASTER_NAME = T_DETAIL.stringAttribute("master_name");
  public static final Attribute<Integer> DETAIL_MASTER_CODE = T_DETAIL.integerAttribute("master_code");
  public static final Attribute<Integer> DETAIL_INT_VALUE_LIST = T_DETAIL.integerAttribute("int_value_list");
  public static final Attribute<Integer> DETAIL_INT_DERIVED = T_DETAIL.integerAttribute("int_derived");

  public static final EntityIdentity DETAIL_SELECT_TABLE_NAME = entityIdentity("db.entity_test_select");

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
                    asList(columnProperty(DETAIL_MASTER_ID_1),
                            columnProperty(DETAIL_MASTER_ID_2))),
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
            .selectTableName(DETAIL_SELECT_TABLE_NAME.getName())
            .orderBy(orderBy().ascending(DETAIL_STRING))
            .smallDataset(true)
            .stringProvider(new StringProvider(DETAIL_STRING));
  }

  public static final EntityIdentity T_DEPARTMENT = entityIdentity("db.scott.dept");

  public static final Attribute<Integer> DEPARTMENT_ID = T_DEPARTMENT.integerAttribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = T_DEPARTMENT.stringAttribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = T_DEPARTMENT.stringAttribute("loc");

  public static final String DEPARTMENT_CONDITION_ID = "condition";
  public static final String DEPARTMENT_NAME_NOT_NULL_CONDITION_ID = "departmentNameNotNull";

  void department() {
    define(T_DEPARTMENT, "scott.dept",
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.getName())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .orderBy(orderBy().ascending(DEPARTMENT_NAME))
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .conditionProvider(DEPARTMENT_CONDITION_ID, (attributes, values) -> {
              final StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .conditionProvider(DEPARTMENT_NAME_NOT_NULL_CONDITION_ID, (attributes, values) -> "department name is not null")
            .caption("Department");
  }

  public static final EntityIdentity T_EMP = entityIdentity("db.scott.emp");
  public static final Attribute<Integer> EMP_ID = T_EMP.integerAttribute("emp_id");
  public static final Attribute<String> EMP_NAME = T_EMP.stringAttribute("emp_name");
  public static final Attribute<String> EMP_JOB = T_EMP.stringAttribute("job");
  public static final Attribute<Integer> EMP_MGR = T_EMP.integerAttribute("mgr");
  public static final Attribute<LocalDateTime> EMP_HIREDATE = T_EMP.localDateTimeAttribute("hiredate");
  public static final Attribute<Double> EMP_SALARY = T_EMP.doubleAttribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = T_EMP.doubleAttribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = T_EMP.integerAttribute("deptno");
  public static final EntityAttribute EMP_DEPARTMENT_FK = T_EMP.entityAttribute("dept_fk");
  public static final EntityAttribute EMP_MGR_FK = T_EMP.entityAttribute("mgr_fk");
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = T_EMP.stringAttribute("location");

  void employee() {
    define(T_EMP, "scott.emp",
            primaryKeyProperty(EMP_ID, EMP_ID.getName()).columnName("empno"),
            columnProperty(EMP_NAME, EMP_NAME.getName())
                    .searchProperty(true).columnName("ename").maximumLength(10).nullable(false),
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
            .orderBy(orderBy().ascending(EMP_DEPARTMENT, EMP_NAME))
            .stringProvider(new StringProvider(EMP_NAME))
            .caption("Employee");
  }
}
