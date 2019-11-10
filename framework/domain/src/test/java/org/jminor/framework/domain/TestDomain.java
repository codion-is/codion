/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.DateFormats;
import org.jminor.common.Item;
import org.jminor.framework.domain.property.ColumnPropertyBuilder;
import org.jminor.framework.domain.property.Properties;

import java.sql.Types;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;

public final class TestDomain extends Domain {

  public TestDomain() {
    compositeMaster();
    compositeDetail();
    master();
    detail();
    department();
    employee();
    registerDomain();
  }

  public static final String T_COMPOSITE_MASTER = "domain.composite_master";
  public static final String COMPOSITE_MASTER_ID = "id";
  public static final String COMPOSITE_MASTER_ID_2 = "id2";

  void compositeMaster() {
    define(T_COMPOSITE_MASTER,
            Properties.columnProperty(COMPOSITE_MASTER_ID).setPrimaryKeyIndex(0).setNullable(true),
            Properties.columnProperty(COMPOSITE_MASTER_ID_2).setPrimaryKeyIndex(1));
  }

  public static final String T_COMPOSITE_DETAIL = "domain.composite_detail";
  public static final String COMPOSITE_DETAIL_MASTER_ID = "master_id";
  public static final String COMPOSITE_DETAIL_MASTER_ID_2 = "master_id2";
  public static final String COMPOSITE_DETAIL_MASTER_FK = "master_fk";

  void compositeDetail() {
    define(T_COMPOSITE_DETAIL,
            Properties.foreignKeyProperty(COMPOSITE_DETAIL_MASTER_FK, "master", T_COMPOSITE_MASTER,
                    asList(Properties.columnProperty(COMPOSITE_DETAIL_MASTER_ID).setPrimaryKeyIndex(0),
                            Properties.columnProperty(COMPOSITE_DETAIL_MASTER_ID_2).setPrimaryKeyIndex(1))));
  }

  public static final String T_MASTER = "domain.master_entity";
  public static final String MASTER_ID = "id";
  public static final String MASTER_NAME = "name";
  public static final String MASTER_CODE = "code";

  void master() {
    define(T_MASTER,
            Properties.primaryKeyProperty(MASTER_ID, Types.BIGINT),
            Properties.columnProperty(MASTER_NAME, Types.VARCHAR),
            Properties.columnProperty(MASTER_CODE, Types.INTEGER))
            .setComparator(Comparator.comparing(o -> o.getInteger(MASTER_CODE)))
            .setStringProvider(new StringProvider(MASTER_NAME));
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
  public static final String DETAIL_MASTER_CODE_DENORM = "master_code_denorm";

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  private static final List<Item> ITEMS = asList(new Item(0, "0"), new Item(1, "1"),
          new Item(2, "2"), new Item(3, "3"));

  void detail() {
    define(T_DETAIL,
            Properties.primaryKeyProperty(DETAIL_ID, Types.BIGINT),
            Properties.columnProperty(DETAIL_INT, Types.INTEGER, DETAIL_INT),
            Properties.columnProperty(DETAIL_DOUBLE, Types.DOUBLE, DETAIL_DOUBLE)
                    .setColumnHasDefaultValue(true),
            Properties.columnProperty(DETAIL_STRING, Types.VARCHAR, "Detail string")
                    .setSelectable(false),
            Properties.columnProperty(DETAIL_DATE, Types.DATE, DETAIL_DATE)
                    .setColumnHasDefaultValue(true),
            Properties.columnProperty(DETAIL_TIMESTAMP, Types.TIMESTAMP, DETAIL_TIMESTAMP),
            Properties.columnProperty(DETAIL_BOOLEAN, Types.BOOLEAN, DETAIL_BOOLEAN)
                    .setNullable(false)
                    .setDefaultValue(true)
                    .setDescription("A boolean property"),
            Properties.columnProperty(DETAIL_BOOLEAN_NULLABLE, Types.BOOLEAN, DETAIL_BOOLEAN_NULLABLE)
                    .setColumnHasDefaultValue(true)
                    .setDefaultValue(true),
            Properties.foreignKeyProperty(DETAIL_MASTER_FK, DETAIL_MASTER_FK, T_MASTER,
                    Properties.columnProperty(DETAIL_MASTER_ID, Types.BIGINT)),
            Properties.denormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_MASTER_FK,
                    getDefinition(T_MASTER).getProperty(MASTER_NAME), DETAIL_MASTER_NAME),
            Properties.denormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_MASTER_FK,
                    getDefinition(T_MASTER).getProperty(MASTER_CODE), DETAIL_MASTER_CODE),
            Properties.valueListProperty(DETAIL_INT_VALUE_LIST, Types.INTEGER, DETAIL_INT_VALUE_LIST, ITEMS),
            Properties.derivedProperty(DETAIL_INT_DERIVED, Types.INTEGER, DETAIL_INT_DERIVED, linkedValues -> {
              final Integer intValue = (Integer) linkedValues.get(DETAIL_INT);
              if (intValue == null) {

                return null;
              }

              return intValue * 10;
            }, DETAIL_INT),
            Properties.denormalizedProperty(DETAIL_MASTER_CODE_DENORM, DETAIL_MASTER_FK,
                    getDefinition(T_MASTER).getProperty(MASTER_CODE)))
            .setKeyGenerator(queriedKeyGenerator("select id from dual"))
            .setOrderBy(orderBy().ascending(DETAIL_STRING))
            .setSelectTableName(DETAIL_SELECT_TABLE_NAME)
            .setSmallDataset(true)
            .setStringProvider(new StringProvider(DETAIL_STRING));
  }

  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";
  public static final String DEPARTMENT_ACTIVE = "active";

  public static final String T_DEPARTMENT = "domain.scott.dept";

  void department() {
    define(T_DEPARTMENT, "scott.dept",
            Properties.primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID)
                    .setUpdatable(true).setNullable(false)
                    .setBeanProperty("deptNo"),
            Properties.columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false)
                    .setBeanProperty("name"),
            Properties.columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .setPreferredColumnWidth(150).setMaxLength(13)
                    .setBeanProperty("location"),
            Properties.booleanProperty(DEPARTMENT_ACTIVE, Types.INTEGER, null, 1, 0)
                    .setReadOnly(true)
                    .setBeanProperty("active"))
            .setSmallDataset(true)
            .setSearchPropertyIds(DEPARTMENT_NAME)
            .setOrderBy(orderBy().ascending(DEPARTMENT_NAME))
            .setStringProvider(new StringProvider(DEPARTMENT_NAME))
            .setBeanClass(Department.class)
            .setCaption("Department");
  }

  public static final String T_EMP = "domain.scott.emp";
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
  public static final String EMP_NAME_DEPARTMENT = "name_department";

  void employee() {
    define(T_EMP, "scott.emp",
            Properties.primaryKeyProperty(EMP_ID, Types.INTEGER, EMP_ID)
                    .setColumnName("empno")
                    .setBeanProperty("id"),
            Properties.columnProperty(EMP_NAME, Types.VARCHAR, EMP_NAME)
                    .setColumnName("ename").setMaxLength(10).setNullable(false)
                    .setBeanProperty("name"),
            Properties.foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK, T_DEPARTMENT,
                    (ColumnPropertyBuilder) Properties.columnProperty(EMP_DEPARTMENT)
                            .setBeanProperty("deptno"))
                    .setBeanProperty("department")
                    .setNullable(false),
            Properties.valueListProperty(EMP_JOB, Types.VARCHAR, EMP_JOB,
                    asList(new Item("ANALYST"), new Item("CLERK"),
                            new Item("MANAGER"), new Item("PRESIDENT"), new Item("SALESMAN")))
                    .setBeanProperty("job"),
            Properties.columnProperty(EMP_SALARY, Types.DOUBLE, EMP_SALARY)
                    .setNullable(false).setMin(1000).setMax(10000).setMaximumFractionDigits(2)
                    .setBeanProperty("salary"),
            Properties.columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION)
                    .setMin(100).setMax(2000).setMaximumFractionDigits(2)
            .setBeanProperty("commission"),
            Properties.foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK, T_EMP,
                    (ColumnPropertyBuilder) Properties.columnProperty(EMP_MGR)
                            .setBeanProperty("mgr"))
                    .setBeanProperty("manager"),
            Properties.columnProperty(EMP_HIREDATE, Types.TIMESTAMP, EMP_HIREDATE)
                    .setUpdatable(false)
                    .setDateTimeFormatPattern(DateFormats.SHORT_DOT)
                    .setNullable(false)
                    .setBeanProperty("hiredate"),
            Properties.denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION).setPreferredColumnWidth(100),
            Properties.derivedProperty(EMP_NAME_DEPARTMENT, Types.VARCHAR, null, linkedValues -> {
              final String name = (String) linkedValues.get(EMP_NAME);
              final Entity department = (Entity) linkedValues.get(EMP_DEPARTMENT_FK);
              if (name == null || department == null) {
                return null;
              }

              return name + " - " + department.getString(DEPARTMENT_NAME);
            }, EMP_NAME, EMP_DEPARTMENT_FK))
            .setKeyGenerator(incrementKeyGenerator("scott.emp", "empno"))
            .setOrderBy(orderBy().ascending(EMP_DEPARTMENT, EMP_NAME))
            .setStringProvider(new StringProvider(EMP_NAME))
            .setSearchPropertyIds(EMP_NAME, EMP_JOB)
            .setBeanClass(Employee.class)
            .setCaption("Employee");
  }

  @Override
  public <V> V toBean(final Entity entity) {
    final V bean = super.toBean(entity);
    if (entity.is(T_EMP) && !entity.isNull(EMP_HIREDATE)) {
      ((Employee) bean).setHiredate(entity.getTimestamp(EMP_HIREDATE).truncatedTo(ChronoUnit.DAYS));
    }

    return bean;
  }
}
