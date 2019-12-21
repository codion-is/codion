/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Item;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.operation.AbstractFunction;
import org.jminor.common.db.operation.AbstractProcedure;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.KeyGenerator;
import org.jminor.framework.domain.StringProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.jminor.framework.domain.KeyGenerators.increment;
import static org.jminor.framework.domain.property.Properties.*;

public final class TestDomain extends Domain {

  public static final String PROCEDURE_ID = "procedureId";
  public static final String FUNCTION_ID = "functionId";

  public TestDomain() {
    department();
    employee();
    uuidTestDefaultValue();
    uuidTestNoDefaultValue();
    operations();
    joinedQuery();
    groupByQuery();
    registerDomain();
  }

  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";

  public static final String T_DEPARTMENT = "scott.dept";

  public static final String DEPARTMENT_CONDITION_ID = "condition";
  public static final String DEPARTMENT_CONDITION_SALES_ID = "conditionSalesId";
  public static final String DEPARTMENT_CONDITION_INVALID_COLUMN_ID = "conditionInvalidColumnId";

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID)
                    .setUpdatable(true).setNullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .setPreferredColumnWidth(150).setMaxLength(13))
            .setSmallDataset(true)
            .setSearchPropertyIds(DEPARTMENT_NAME)
            .setStringProvider(new StringProvider(DEPARTMENT_NAME))
            .addConditionProvider(DEPARTMENT_CONDITION_ID, (propetyIds, values) -> {
              final StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .addConditionProvider(DEPARTMENT_CONDITION_SALES_ID, (propetyIds, values) -> "dname = 'SALES'")
            .addConditionProvider(DEPARTMENT_CONDITION_INVALID_COLUMN_ID, (propetyIds, values) -> "no_column is null")
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
  public static final String EMP_DATA_LAZY = "data_lazy";
  public static final String EMP_DATA = "data";
  public static final String T_EMP = "scott.emp";

  public static final String EMP_NAME_IS_BLAKE_CONDITION_ID = "condition1Id";
  public static final String EMP_MGR_GREATER_THAN_CONDITION_ID = "condition2Id";

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
            columnProperty(EMP_SALARY, Types.DOUBLE, EMP_SALARY)
                    .setNullable(false).setMin(1000).setMax(10000).setMaximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION)
                    .setMin(100).setMax(2000).setMaximumFractionDigits(2),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK, T_EMP,
                    columnProperty(EMP_MGR))
                    //not really soft, just for testing purposes
                    .setSoftReference(true),
            columnProperty(EMP_HIREDATE, Types.DATE, EMP_HIREDATE)
                    .setNullable(false),
            columnProperty(EMP_HIRETIME, Types.TIMESTAMP, EMP_HIRETIME),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION).setPreferredColumnWidth(100),
            blobProperty(EMP_DATA_LAZY)
                    .setLazyLoaded(true),
            blobProperty(EMP_DATA))
            .setStringProvider(new StringProvider(EMP_NAME))
            .setKeyGenerator(increment("scott.emp", "empno"))
            .setSearchPropertyIds(EMP_NAME, EMP_JOB)
            .addConditionProvider(EMP_NAME_IS_BLAKE_CONDITION_ID, (propetyIds, values) -> "ename = 'BLAKE'")
            .addConditionProvider(EMP_MGR_GREATER_THAN_CONDITION_ID, (propetyIds, values) -> "mgr > ?")
            .setCaption("Employee");
  }

  public static final String T_UUID_TEST_DEFAULT = "scott.uuid_test_default";
  public static final String UUID_TEST_DEFAULT_ID = "id";
  public static final String UUID_TEST_DEFAULT_DATA = "data";

  private void uuidTestDefaultValue() {
    final KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void afterInsert(final Entity entity, final DatabaseConnection connection, final Statement insertStatement) throws SQLException {
        final ResultSet generatedKeys = insertStatement.getGeneratedKeys();
        if (generatedKeys.next()) {
          entity.put(UUID_TEST_DEFAULT_ID, generatedKeys.getObject(1));
        }
      }
      @Override
      public boolean returnPrimaryKeyValues() {
        return true;
      }
    };
    define(T_UUID_TEST_DEFAULT,
            primaryKeyProperty(UUID_TEST_DEFAULT_ID, Types.JAVA_OBJECT, "Id"),
            columnProperty(UUID_TEST_DEFAULT_DATA, Types.VARCHAR, "Data"))
            .setKeyGenerator(uuidKeyGenerator);
  }

  public static final String T_UUID_TEST_NO_DEFAULT = "scott.uuid_test_no_default";
  public static final String UUID_TEST_NO_DEFAULT_ID = "id";
  public static final String UUID_TEST_NO_DEFAULT_DATA = "data";

  private void uuidTestNoDefaultValue() {
    final KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {
        entity.put(UUID_TEST_NO_DEFAULT_ID, UUID.randomUUID());
      }
    };
    define(T_UUID_TEST_NO_DEFAULT,
            primaryKeyProperty(UUID_TEST_NO_DEFAULT_ID, Types.JAVA_OBJECT, "Id"),
            columnProperty(UUID_TEST_NO_DEFAULT_DATA, Types.VARCHAR, "Data"))
            .setKeyGenerator(uuidKeyGenerator);
  }

  private void operations() {
    addOperation(new AbstractProcedure<EntityConnection>(PROCEDURE_ID, "executeProcedure") {
      @Override
      public void execute(final EntityConnection connection, final Object... arguments) {}
    });
    addOperation(new AbstractFunction<EntityConnection>(FUNCTION_ID, "executeFunction") {
      @Override
      public List execute(final EntityConnection connection, final Object... arguments) {
        return null;
      }
    });
  }

  public static final String GROUP_BY_QUERY_ENTITY_ID = "groupByQueryEntityID";
  public static final String JOINED_QUERY_CONDITION_ID = "conditionId";

  private void groupByQuery() {
    define(GROUP_BY_QUERY_ENTITY_ID, "scott.emp",
            columnProperty("job", Types.VARCHAR)
                    .setPrimaryKeyIndex(0)
                    .setGroupingColumn(true))
            .setHavingClause("job <> 'PRESIDENT'");
  }


  public static final String JOINED_QUERY_ENTITY_ID = "joinedQueryEntityID";

  private void joinedQuery() {
    define(JOINED_QUERY_ENTITY_ID,
            primaryKeyProperty("e.empno"),
            columnProperty("d.deptno", Types.INTEGER))
            .setSelectQuery("select e.empno, d.deptno from scott.emp e, scott.dept d where e.deptno = d.deptno", true)
            .addConditionProvider(JOINED_QUERY_CONDITION_ID, (propetyIds, values) -> "d.deptno = 10");
  }
}
