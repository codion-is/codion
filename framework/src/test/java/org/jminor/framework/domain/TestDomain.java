/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.AbstractProcedure;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Item;
import org.jminor.framework.db.EntityConnection;

import java.awt.Color;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

public final class TestDomain {

  private TestDomain() {}
  public static void init() {}

  public static final String T_MASTER = "test.master_entity";
  public static final String MASTER_ID = "id";
  public static final String MASTER_NAME = "name";
  public static final String MASTER_CODE = "code";

  static {
    Entities.define(T_MASTER,
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
  public static final String DETAIL_INT = "int";
  public static final String DETAIL_DOUBLE = "double";
  public static final String DETAIL_STRING = "string";
  public static final String DETAIL_DATE = "date";
  public static final String DETAIL_TIMESTAMP = "timestamp";
  public static final String DETAIL_BOOLEAN = "boolean";
  public static final String DETAIL_BOOLEAN_NULLABLE = "boolean_nullable";
  public static final String DETAIL_ENTITY_ID = "entity_id";
  public static final String DETAIL_ENTITY_FK = "entity_ref";
  public static final String DETAIL_MASTER_NAME = "master_name";
  public static final String DETAIL_MASTER_CODE = "master_code";
  public static final String DETAIL_INT_VALUE_LIST = "int_value_list";
  public static final String DETAIL_INT_DERIVED = "int_derived";

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  @Entity.Table(orderByClause = DETAIL_STRING, selectTableName = DETAIL_SELECT_TABLE_NAME)
  public static final String T_DETAIL = "test.detail_entity";

  private static final List<Item> ITEMS = Arrays.asList(new Item(0, "0"), new Item(1, "1"),
          new Item(2, "2"), new Item(3, "3"));

  static {
    Entities.define(T_DETAIL,
            Properties.primaryKeyProperty(DETAIL_ID, Types.BIGINT),
            Properties.columnProperty(DETAIL_INT, Types.INTEGER, DETAIL_INT),
            Properties.columnProperty(DETAIL_DOUBLE, Types.DOUBLE, DETAIL_DOUBLE),
            Properties.columnProperty(DETAIL_STRING, Types.VARCHAR, "Detail string"),
            Properties.columnProperty(DETAIL_DATE, Types.DATE, DETAIL_DATE),
            Properties.columnProperty(DETAIL_TIMESTAMP, Types.TIMESTAMP, DETAIL_TIMESTAMP),
            Properties.columnProperty(DETAIL_BOOLEAN, Types.BOOLEAN, DETAIL_BOOLEAN)
                    .setNullable(false)
                    .setDefaultValue(true)
                    .setDescription("A boolean property"),
            Properties.columnProperty(DETAIL_BOOLEAN_NULLABLE, Types.BOOLEAN, DETAIL_BOOLEAN_NULLABLE)
                    .setDefaultValue(true),
            Properties.foreignKeyProperty(DETAIL_ENTITY_FK, DETAIL_ENTITY_FK, T_MASTER,
                    Properties.columnProperty(DETAIL_ENTITY_ID, Types.BIGINT)),
            Properties.denormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_ENTITY_FK,
                    Entities.getProperty(T_MASTER, MASTER_NAME), DETAIL_MASTER_NAME),
            Properties.denormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_ENTITY_FK,
                    Entities.getProperty(T_MASTER, MASTER_CODE), DETAIL_MASTER_CODE),
            Properties.valueListProperty(DETAIL_INT_VALUE_LIST, Types.INTEGER, DETAIL_INT_VALUE_LIST, ITEMS),
            Properties.derivedProperty(DETAIL_INT_DERIVED, Types.INTEGER, DETAIL_INT_DERIVED, linkedValues -> {
              final Integer intValue = (Integer) linkedValues.get(DETAIL_INT);
              if (intValue == null) {
                return null;
              }

              return intValue * 10;
            }, DETAIL_INT))
            .setSmallDataset(true)
            .setStringProvider(new Entities.StringProvider(DETAIL_STRING));
  }

  public static final String SCOTT_DOMAIN_ID = "scott.domain";

  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";

  @Entity.Table(tableName = "scott.dept")
  public static final String T_DEPARTMENT = "unittest.scott.dept";

  static {
    Entities.define(T_DEPARTMENT,
            Properties.primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID)
                    .setUpdatable(true).setNullable(false),
            Properties.columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false),
            Properties.columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .setPreferredColumnWidth(150).setMaxLength(13))
            .setDomainID(SCOTT_DOMAIN_ID)
            .setSmallDataset(true)
            .setOrderByClause(DEPARTMENT_NAME)
            .setStringProvider(new Entities.StringProvider(DEPARTMENT_NAME))
            .setCaption("Department");
  }

  @Property.Column(entityID = "unittest.scott.emp", columnName = "empno")
  public static final String EMP_ID = "emp_id";
  @Property.Column(entityID = "unittest.scott.emp", columnName = "ename")
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
  @Entity.Table(orderByClause = EMP_DEPARTMENT + ", ename",
          keyGenerator = Entity.KeyGenerator.Type.INCREMENT,
          keyGeneratorSource = "scott.emp",
          keyGeneratorIncrementColumnName = "empno")
  public static final String T_EMP = "unittest.scott.emp";

  static {
    Entities.define(T_EMP, "scott.emp",
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
                    Entities.getProperty(T_DEPARTMENT, DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION).setPreferredColumnWidth(100))
            .setDomainID(SCOTT_DOMAIN_ID)
            .setStringProvider(new Entities.StringProvider(EMP_NAME))
            .setSearchPropertyIDs(EMP_NAME, EMP_JOB)
            .setCaption("Employee")
            .setBackgroundColorProvider((entity, property) -> {
              if (property.is(EMP_JOB) && "MANAGER".equals(entity.get(EMP_JOB))) {
                return Color.CYAN;
              }

              return null;
            });
  }

  public static final class TestProcedure extends AbstractProcedure<EntityConnection> {

    public TestProcedure(final String id) {
      super(id, "TestProcedure");
    }

    @Override
    public void execute(final EntityConnection connection, final Object... arguments) throws DatabaseException {
      //do absolutely nothing
    }
  }

  @Databases.Operation(className = "org.jminor.framework.domain.TestDomain$TestProcedure")
  public static final String PROCEDURE_ID = "org.jminor.framework.domain.TestProcedureID";

  static {
    Entities.processAnnotations(TestDomain.class);
  }
}
