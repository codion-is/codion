/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json;

import org.jminor.common.Item;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.StringProvider;

import java.sql.Types;

import static java.util.Arrays.asList;
import static org.jminor.framework.domain.KeyGenerators.incrementKeyGenerator;
import static org.jminor.framework.domain.property.Properties.*;

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
            columnProperty(ENTITY_DECIMAL, Types.DECIMAL).setPrimaryKeyIndex(0),
            columnProperty(ENTITY_DATE_TIME, Types.TIMESTAMP).setPrimaryKeyIndex(1),
            columnProperty(ENTITY_BLOB, Types.BLOB),
            columnProperty(ENTITY_READ_ONLY, Types.VARCHAR)
                    .setReadOnly(true),
            columnProperty(ENTITY_BOOLEAN, Types.BOOLEAN),
            columnProperty(ENTITY_TIME, Types.TIME))
            .addConditionProvider(ENTITY_CONDITION_ID, (propertyIds, values) -> "1 = 2");
  }

  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";
  public static final String DEPARTMENT_LOGO = "logo";

  public static final String T_DEPARTMENT = "scott.dept";

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID)
                    .setUpdatable(true).setNullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .setPreferredColumnWidth(150).setMaxLength(13),
            columnProperty(DEPARTMENT_LOGO, Types.BLOB))
            .setSmallDataset(true)
            .setSearchPropertyIds(DEPARTMENT_NAME)
            .setStringProvider(new StringProvider(DEPARTMENT_NAME))
            .setCaption("Department");
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
                    .setMaxLength(10).setNullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK, T_DEPARTMENT,
                    columnProperty(EMP_DEPARTMENT))
                    .setNullable(false),
            valueListProperty(EMP_JOB, Types.VARCHAR, EMP_JOB,
                    asList(new Item("ANALYST"), new Item("CLERK"), new Item("MANAGER"), new Item("PRESIDENT"), new Item("SALESMAN"))),
            columnProperty(EMP_SALARY, Types.DECIMAL, EMP_SALARY)
                    .setNullable(false).setMin(1000).setMax(10000).setMaximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION)
                    .setMin(100).setMax(2000).setMaximumFractionDigits(2),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK, T_EMP,
                    columnProperty(EMP_MGR)),
            columnProperty(EMP_HIREDATE, Types.DATE, EMP_HIREDATE)
                    .setNullable(false),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION).setPreferredColumnWidth(100))
            .setStringProvider(new StringProvider(EMP_NAME))
            .setKeyGenerator(incrementKeyGenerator("scott.emp", "empno"))
            .setSearchPropertyIds(EMP_NAME, EMP_JOB)
            .setCaption("Employee");
  }
}
