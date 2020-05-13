/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.plugin.jackson.json;

import dev.codion.framework.domain.Domain;
import dev.codion.framework.domain.entity.StringProvider;

import java.sql.Types;

import static java.util.Arrays.asList;
import static dev.codion.common.item.Items.item;
import static dev.codion.framework.domain.entity.KeyGenerators.increment;
import static dev.codion.framework.domain.property.Properties.*;

public final class TestDomain extends Domain {

  public TestDomain() {
    testEntity();
    department();
    employee();
  }

  public static final String T_ENTITY = "test.entity";
  public static final String ENTITY_DECIMAL = "id";
  public static final String ENTITY_DATE_TIME = "date_time";
  public static final String ENTITY_BLOB = "blob";
  public static final String ENTITY_READ_ONLY = "read_only";
  public static final String ENTITY_CONDITION_ID = "entityConditionId";
  public static final String ENTITY_BOOLEAN = "boolean";
  public static final String ENTITY_TIME = "time";

  void testEntity() {
    define(T_ENTITY,
            columnProperty(ENTITY_DECIMAL, Types.DECIMAL).primaryKeyIndex(0),
            columnProperty(ENTITY_DATE_TIME, Types.TIMESTAMP).primaryKeyIndex(1),
            columnProperty(ENTITY_BLOB, Types.BLOB),
            columnProperty(ENTITY_READ_ONLY, Types.VARCHAR)
                    .readOnly(true),
            columnProperty(ENTITY_BOOLEAN, Types.BOOLEAN),
            columnProperty(ENTITY_TIME, Types.TIME))
            .conditionProvider(ENTITY_CONDITION_ID, (propertyIds, values) -> "1 = 2");
  }

  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";
  public static final String DEPARTMENT_LOGO = "logo";

  public static final String T_DEPARTMENT = "scott.dept";

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID)
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .preferredColumnWidth(150).maximumLength(13),
            columnProperty(DEPARTMENT_LOGO, Types.BLOB))
            .smallDataset(true)
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .caption("Department");
  }

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
  public static final String T_EMP = "scott.emp";

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
            columnProperty(EMP_SALARY, Types.DECIMAL, EMP_SALARY)
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
            .caption("Employee");
  }
}
