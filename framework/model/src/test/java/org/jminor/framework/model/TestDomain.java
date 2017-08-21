/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Item;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Properties;

import java.awt.Color;
import java.sql.Types;
import java.util.Arrays;

public final class TestDomain extends Entities {

  public TestDomain() {
    defineMaster();
    defineDetail();
    defineDepartment();
    defineEmployee();
    registerDomain();
  }

  public static final String T_MASTER = "domain.master_entity";
  public static final String MASTER_ID = "id";
  public static final String MASTER_NAME = "name";
  public static final String MASTER_CODE = "code";

  void defineMaster() {
    define(T_MASTER,
            Properties.primaryKeyProperty(MASTER_ID, Types.BIGINT),
            Properties.columnProperty(MASTER_NAME, Types.VARCHAR),
            Properties.columnProperty(MASTER_CODE, Types.INTEGER))
            .setComparator((o1, o2) -> {
              final Integer code1 = o1.getInteger(MASTER_CODE);
              final Integer code2 = o2.getInteger(MASTER_CODE);

              return code1.compareTo(code2);
            })
            .setStringProvider(new Entities.StringProvider(MASTER_NAME));
  }

  public static final String DETAIL_ID = "id";
  public static final String DETAIL_MASTER_ID = "master_id";
  public static final String DETAIL_MASTER_FK = "master_fk";

  public static final String T_DETAIL = "domain.detail_entity";

  void defineDetail() {
    define(T_DETAIL,
            Properties.primaryKeyProperty(DETAIL_ID, Types.BIGINT),
            Properties.foreignKeyProperty(DETAIL_MASTER_FK, DETAIL_MASTER_FK, T_MASTER,
                    Properties.columnProperty(DETAIL_MASTER_ID, Types.BIGINT)))
            .setSmallDataset(true);
  }

  public static final String SCOTT_DOMAIN_ID = "domain.scott.domain";

  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";

  public static final String T_DEPARTMENT = "scott.dept";

  void defineDepartment() {
    define(T_DEPARTMENT,
            Properties.primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID)
                    .setUpdatable(true).setNullable(false),
            Properties.columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false),
            Properties.columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .setPreferredColumnWidth(150).setMaxLength(13))
            .setSmallDataset(true)
            .setSearchPropertyIDs(DEPARTMENT_NAME)
            .setOrderByClause(DEPARTMENT_NAME)
            .setStringProvider(new Entities.StringProvider(DEPARTMENT_NAME))
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

  void defineEmployee() {
    define(T_EMP,
            Properties.primaryKeyProperty(EMP_ID, Types.INTEGER, EMP_ID),
            Properties.columnProperty(EMP_NAME, Types.VARCHAR, EMP_NAME)
                    .setMaxLength(10).setNullable(false),
            Properties.foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK, T_DEPARTMENT,
                    Properties.columnProperty(EMP_DEPARTMENT))
                    .setNullable(false),
            Properties.valueListProperty(EMP_JOB, Types.VARCHAR, EMP_JOB,
                    Arrays.asList(new Item("ANALYST"), new Item("CLERK"), new Item("MANAGER"), new Item("PRESIDENT"), new Item("SALESMAN"))),
            Properties.columnProperty(EMP_SALARY, Types.DOUBLE, EMP_SALARY)
                    .setNullable(false).setMin(1000).setMax(10000).setMaximumFractionDigits(2),
            Properties.columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION)
                    .setMin(100).setMax(2000).setMaximumFractionDigits(2),
            Properties.foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK, T_EMP,
                    Properties.columnProperty(EMP_MGR)),
            Properties.columnProperty(EMP_HIREDATE, Types.DATE, EMP_HIREDATE)
                    .setNullable(false),
            Properties.denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    getProperty(T_DEPARTMENT, DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION).setPreferredColumnWidth(100))
            .setStringProvider(new Entities.StringProvider(EMP_NAME))
            .setKeyGenerator(incrementKeyGenerator("scott.emp", "empno"))
            .setSearchPropertyIDs(EMP_NAME, EMP_JOB)
            .setOrderByClause(EMP_DEPARTMENT + ", ename")
            .setCaption("Employee")
            .setBackgroundColorProvider((entity, property) -> {
              if (property.is(EMP_JOB) && "MANAGER".equals(entity.get(EMP_JOB))) {
                return Color.CYAN;
              }

              return null;
            });
  }
}
