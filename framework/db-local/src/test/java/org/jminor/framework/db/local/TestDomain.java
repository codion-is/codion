/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.db.connection.DatabaseConnection;
import org.jminor.common.db.operation.AbstractDatabaseFunction;
import org.jminor.common.db.operation.AbstractDatabaseProcedure;
import org.jminor.common.db.reports.AbstractReportWrapper;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.KeyGenerator;
import org.jminor.framework.domain.entity.StringProvider;
import org.jminor.framework.domain.property.ColumnProperty;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.jminor.common.item.Items.item;
import static org.jminor.framework.domain.entity.KeyGenerators.increment;
import static org.jminor.framework.domain.property.Properties.*;

public final class TestDomain extends Domain {

  public static final ReportWrapper<Object, String, Map<String, Object>> REPORT = new AbstractReportWrapper<Object, String, Map<String, Object>>("report.path") {
    @Override
    public String fillReport(final Connection connection, final Map<String, Object> parameters) throws ReportException {
      return "result";
    }

    @Override
    public Object loadReport() throws ReportException {
      return null;
    }
  };

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
    noPkEntity();
    registerDomain();
    ReportWrapper.REPORT_PATH.set("path/to/reports");
    addReport(REPORT);
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
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .searchPropertyIds(DEPARTMENT_NAME)
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .conditionProvider(DEPARTMENT_CONDITION_ID, (propertyIds, values) -> {
              final StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .conditionProvider(DEPARTMENT_CONDITION_SALES_ID, (propertyIds, values) -> "dname = 'SALES'")
            .conditionProvider(DEPARTMENT_CONDITION_INVALID_COLUMN_ID, (propertyIds, values) -> "no_column is null")
            .caption("Department");
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
                    .maximumLength(10).nullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK, T_DEPARTMENT,
                    columnProperty(EMP_DEPARTMENT))
                    .nullable(false),
            valueListProperty(EMP_JOB, Types.VARCHAR, EMP_JOB,
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN"))),
            columnProperty(EMP_SALARY, Types.DOUBLE, EMP_SALARY)
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION)
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK, T_EMP,
                    columnProperty(EMP_MGR))
                    //not really soft, just for testing purposes
                    .softReference(true),
            columnProperty(EMP_HIREDATE, Types.DATE, EMP_HIREDATE)
                    .nullable(false),
            columnProperty(EMP_HIRETIME, Types.TIMESTAMP, EMP_HIRETIME),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION).preferredColumnWidth(100),
            columnProperty(EMP_DATA_LAZY, Types.BLOB),
            blobProperty(EMP_DATA)
                    .eagerlyLoaded(true))
            .stringProvider(new StringProvider(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .searchPropertyIds(EMP_NAME, EMP_JOB)
            .conditionProvider(EMP_NAME_IS_BLAKE_CONDITION_ID, (propertyIds, values) -> "ename = 'BLAKE'")
            .conditionProvider(EMP_MGR_GREATER_THAN_CONDITION_ID, (propertyIds, values) -> "mgr > ?")
            .caption("Employee");
  }

  public static final String T_UUID_TEST_DEFAULT = "scott.uuid_test_default";
  public static final String UUID_TEST_DEFAULT_ID = "id";
  public static final String UUID_TEST_DEFAULT_DATA = "data";

  private void uuidTestDefaultValue() {
    final KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void afterInsert(final Entity entity, final List<ColumnProperty> primaryKeyProperties,
                              final DatabaseConnection connection, final Statement insertStatement) throws SQLException {
        final ResultSet generatedKeys = insertStatement.getGeneratedKeys();
        if (generatedKeys.next()) {
          entity.put(UUID_TEST_DEFAULT_ID, generatedKeys.getObject(1));
        }
      }
      @Override
      public boolean returnGeneratedKeys() {
        return true;
      }
    };
    define(T_UUID_TEST_DEFAULT,
            primaryKeyProperty(UUID_TEST_DEFAULT_ID, Types.JAVA_OBJECT, "Id"),
            columnProperty(UUID_TEST_DEFAULT_DATA, Types.VARCHAR, "Data"))
            .keyGenerator(uuidKeyGenerator);
  }

  public static final String T_UUID_TEST_NO_DEFAULT = "scott.uuid_test_no_default";
  public static final String UUID_TEST_NO_DEFAULT_ID = "id";
  public static final String UUID_TEST_NO_DEFAULT_DATA = "data";

  private void uuidTestNoDefaultValue() {
    final KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void beforeInsert(final Entity entity, final List<ColumnProperty> primaryKeyProperties,
                               final DatabaseConnection connection) throws SQLException {
        entity.put(UUID_TEST_NO_DEFAULT_ID, UUID.randomUUID());
      }
    };
    define(T_UUID_TEST_NO_DEFAULT,
            primaryKeyProperty(UUID_TEST_NO_DEFAULT_ID, Types.JAVA_OBJECT, "Id"),
            columnProperty(UUID_TEST_NO_DEFAULT_DATA, Types.VARCHAR, "Data"))
            .keyGenerator(uuidKeyGenerator);
  }

  private void operations() {
    addOperation(new AbstractDatabaseProcedure<EntityConnection>(PROCEDURE_ID, "executeProcedure") {
      @Override
      public void execute(final EntityConnection connection, final Object... arguments) {}
    });
    addOperation(new AbstractDatabaseFunction<EntityConnection, List>(FUNCTION_ID, "executeFunction") {
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
                    .primaryKeyIndex(0)
                    .groupingColumn(true))
            .havingClause("job <> 'PRESIDENT'");
  }

  public static final String T_NO_PK = "scott.no_pk_table";
  public static final String NO_PK_COL1 = "col1";
  public static final String NO_PK_COL2 = "col2";
  public static final String NO_PK_COL3 = "col3";
  public static final String NO_PK_COL4 = "col4";

  private void noPkEntity() {
    define(T_NO_PK,
            columnProperty(NO_PK_COL1, Types.INTEGER),
            columnProperty(NO_PK_COL2, Types.VARCHAR),
            columnProperty(NO_PK_COL3, Types.VARCHAR),
            columnProperty(NO_PK_COL4, Types.INTEGER));
  }

  public static final String JOINED_QUERY_ENTITY_ID = "joinedQueryEntityID";

  private void joinedQuery() {
    define(JOINED_QUERY_ENTITY_ID,
            primaryKeyProperty("e.empno"),
            columnProperty("d.deptno", Types.INTEGER))
            .selectQuery("select e.empno, d.deptno from scott.emp e, scott.dept d where e.deptno = d.deptno", true)
            .conditionProvider(JOINED_QUERY_CONDITION_ID, (propertyIds, values) -> "d.deptno = 10");
  }
}
