/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Item;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Properties;

import java.sql.Types;

import static java.util.Arrays.asList;

public final class TestDomain extends Domain {

  public TestDomain() {
    department();
    employee();
    registerDomain();
  }

  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";

  public static final String T_DEPARTMENT = "scott.dept";

  void department() {
    define(T_DEPARTMENT,
            Properties.primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID)
                    .setUpdatable(true).setNullable(false),
            Properties.columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false),
            Properties.columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .setPreferredColumnWidth(150).setMaxLength(13))
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
  public static final String EMP_HIRETIME = "hiretime";
  public static final String EMP_SALARY = "sal";
  public static final String EMP_COMMISSION = "comm";
  public static final String EMP_DEPARTMENT = "deptno";
  public static final String EMP_DEPARTMENT_FK = "dept_fk";
  public static final String EMP_MGR_FK = "mgr_fk";
  public static final String EMP_DEPARTMENT_LOCATION = "location";
  public static final String EMP_DATA = "data";
  public static final String T_EMP = "scott.emp";

  void employee() {
    define(T_EMP,
            Properties.primaryKeyProperty(EMP_ID, Types.INTEGER, EMP_ID),
            Properties.columnProperty(EMP_NAME, Types.VARCHAR, EMP_NAME)
                    .setMaxLength(10).setNullable(false),
            Properties.foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK, T_DEPARTMENT,
                    Properties.columnProperty(EMP_DEPARTMENT))
                    .setNullable(false),
            Properties.valueListProperty(EMP_JOB, Types.VARCHAR, EMP_JOB,
                    asList(new Item("ANALYST"), new Item("CLERK"), new Item("MANAGER"), new Item("PRESIDENT"), new Item("SALESMAN"))),
            Properties.columnProperty(EMP_SALARY, Types.DOUBLE, EMP_SALARY)
                    .setNullable(false).setMin(1000).setMax(10000).setMaximumFractionDigits(2),
            Properties.columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION)
                    .setMin(100).setMax(2000).setMaximumFractionDigits(2),
            Properties.foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK, T_EMP,
                    Properties.columnProperty(EMP_MGR))
                    //not really soft, just for testing purposes
                    .setSoftReference(true),
            Properties.columnProperty(EMP_HIREDATE, Types.DATE, EMP_HIREDATE)
                    .setNullable(false),
            Properties.columnProperty(EMP_HIRETIME, Types.TIMESTAMP, EMP_HIRETIME),
            Properties.denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    getProperty(T_DEPARTMENT, DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION).setPreferredColumnWidth(100),
            Properties.columnProperty(EMP_DATA, Types.BLOB, "Data"))
            .setStringProvider(new StringProvider(EMP_NAME))
            .setKeyGenerator(incrementKeyGenerator("scott.emp", "empno"))
            .setSearchPropertyIds(EMP_NAME, EMP_JOB)
            .setCaption("Employee");
  }
}
