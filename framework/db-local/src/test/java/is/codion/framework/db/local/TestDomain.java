/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.reports.AbstractReportWrapper;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportType;
import is.codion.common.db.reports.ReportWrapper;
import is.codion.common.db.reports.Reports;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.ColumnProperty;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.EntityType.entityType;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

public final class TestDomain extends Domain {

  public static final ReportType<Object, String, Map<String, Object>> REPORT = Reports.reportType("report");

  public static final ProcedureType<EntityConnection, Object> PROCEDURE_ID = ProcedureType.procedureType("procedureId");
  public static final FunctionType<EntityConnection, Object, List<Object>> FUNCTION_ID = FunctionType.functionType("functionId");

  public TestDomain() {
    department();
    employee();
    uuidTestDefaultValue();
    uuidTestNoDefaultValue();
    operations();
    joinedQuery();
    groupByQuery();
    noPkEntity();
    ReportWrapper.REPORT_PATH.set("path/to/reports");
    defineReport(REPORT, new AbstractReportWrapper<Object, String, Map<String, Object>>("report.path") {
      @Override
      public String fillReport(final Connection connection, final Map<String, Object> parameters) throws ReportException {
        return "result";
      }

      @Override
      public Object loadReport() throws ReportException {
        return null;
      }
    });
  }

  public static final EntityType T_DEPARTMENT = entityType("scott.dept");
  public static final Attribute<Integer> DEPARTMENT_ID = T_DEPARTMENT.integerAttribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = T_DEPARTMENT.stringAttribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = T_DEPARTMENT.stringAttribute("loc");

  public static final String DEPARTMENT_CONDITION_ID = "condition";
  public static final String DEPARTMENT_CONDITION_SALES_ID = "conditionSalesId";
  public static final String DEPARTMENT_CONDITION_INVALID_COLUMN_ID = "conditionInvalidColumnId";

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.getName())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .conditionProvider(DEPARTMENT_CONDITION_ID, (attributes, values) -> {
              final StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .conditionProvider(DEPARTMENT_CONDITION_SALES_ID, (attributes, values) -> "dname = 'SALES'")
            .conditionProvider(DEPARTMENT_CONDITION_INVALID_COLUMN_ID, (attributes, values) -> "no_column is null")
            .caption("Department");
  }

  public static final EntityType T_EMP = entityType("scott.emp");
  public static final Attribute<Integer> EMP_ID = T_EMP.integerAttribute("empno");
  public static final Attribute<String> EMP_NAME = T_EMP.stringAttribute("ename");
  public static final Attribute<String> EMP_JOB = T_EMP.stringAttribute("job");
  public static final Attribute<Integer> EMP_MGR = T_EMP.integerAttribute("mgr");
  public static final Attribute<LocalDate> EMP_HIREDATE = T_EMP.localDateAttribute("hiredate");
  public static final Attribute<LocalDateTime> EMP_HIRETIME = T_EMP.localDateTimeAttribute("hiretime");
  public static final Attribute<Double> EMP_SALARY = T_EMP.doubleAttribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = T_EMP.doubleAttribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = T_EMP.integerAttribute("deptno");
  public static final Attribute<Entity> EMP_DEPARTMENT_FK = T_EMP.entityAttribute("dept_fk");
  public static final Attribute<Entity> EMP_MGR_FK = T_EMP.entityAttribute("mgr_fk");
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = T_EMP.stringAttribute("location");
  public static final Attribute<byte[]> EMP_DATA_LAZY = T_EMP.blobAttribute("data_lazy");
  public static final Attribute<byte[]> EMP_DATA = T_EMP.blobAttribute("data");

  public static final String EMP_NAME_IS_BLAKE_CONDITION_ID = "condition1Id";
  public static final String EMP_MGR_GREATER_THAN_CONDITION_ID = "condition2Id";

  void employee() {
    define(T_EMP,
            primaryKeyProperty(EMP_ID, EMP_ID.getName()),
            columnProperty(EMP_NAME, EMP_NAME.getName())
                    .searchProperty(true).maximumLength(10).nullable(false),
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
                    columnProperty(EMP_MGR))
                    //not really soft, just for testing purposes
                    .softReference(true),
            columnProperty(EMP_HIREDATE, EMP_HIREDATE.getName())
                    .nullable(false),
            columnProperty(EMP_HIRETIME, EMP_HIRETIME.getName()),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK, DEPARTMENT_LOCATION,
                    DEPARTMENT_LOCATION.getName()).preferredColumnWidth(100),
            columnProperty(EMP_DATA_LAZY),
            blobProperty(EMP_DATA)
                    .eagerlyLoaded(true))
            .stringProvider(new StringProvider(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .conditionProvider(EMP_NAME_IS_BLAKE_CONDITION_ID, (attributes, values) -> "ename = 'BLAKE'")
            .conditionProvider(EMP_MGR_GREATER_THAN_CONDITION_ID, (attributes, values) -> "mgr > ?")
            .caption("Employee");
  }

  public static final EntityType T_UUID_TEST_DEFAULT = entityType("scott.uuid_test_default");
  public static final Attribute<UUID> UUID_TEST_DEFAULT_ID = T_UUID_TEST_DEFAULT.attribute("id", UUID.class);
  public static final Attribute<String> UUID_TEST_DEFAULT_DATA = T_UUID_TEST_DEFAULT.stringAttribute("data");

  private void uuidTestDefaultValue() {
    final KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void afterInsert(final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties,
                              final DatabaseConnection connection, final Statement insertStatement) throws SQLException {
        final ResultSet generatedKeys = insertStatement.getGeneratedKeys();
        if (generatedKeys.next()) {
          entity.put(UUID_TEST_DEFAULT_ID, (UUID) generatedKeys.getObject(1));
        }
      }
      @Override
      public boolean returnGeneratedKeys() {
        return true;
      }
    };
    define(T_UUID_TEST_DEFAULT,
            primaryKeyProperty(UUID_TEST_DEFAULT_ID, "Id"),
            columnProperty(UUID_TEST_DEFAULT_DATA, "Data"))
            .keyGenerator(uuidKeyGenerator);
  }

  public static final EntityType T_UUID_TEST_NO_DEFAULT = entityType("scott.uuid_test_no_default");
  public static final Attribute<UUID> UUID_TEST_NO_DEFAULT_ID = T_UUID_TEST_NO_DEFAULT.attribute("id", UUID.class);
  public static final Attribute<String> UUID_TEST_NO_DEFAULT_DATA = T_UUID_TEST_NO_DEFAULT.stringAttribute("data");

  private void uuidTestNoDefaultValue() {
    final KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void beforeInsert(final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties,
                               final DatabaseConnection connection) throws SQLException {
        entity.put(UUID_TEST_NO_DEFAULT_ID, UUID.randomUUID());
      }
    };
    define(T_UUID_TEST_NO_DEFAULT,
            primaryKeyProperty(UUID_TEST_NO_DEFAULT_ID, "Id"),
            columnProperty(UUID_TEST_NO_DEFAULT_DATA, "Data"))
            .keyGenerator(uuidKeyGenerator);
  }

  private void operations() {
    defineProcedure(PROCEDURE_ID, (connection, arguments) -> {});
    defineFunction(FUNCTION_ID, (connection, arguments) -> null);
  }

  public static final EntityType GROUP_BY_QUERY_ENTITY_TYPE = entityType("groupByQueryEntityType");
  public static final String JOINED_QUERY_CONDITION_ID = "conditionId";

  private void groupByQuery() {
    define(GROUP_BY_QUERY_ENTITY_TYPE, "scott.emp",
            columnProperty(GROUP_BY_QUERY_ENTITY_TYPE.stringAttribute("job"))
                    .primaryKeyIndex(0)
                    .groupingColumn(true))
            .havingClause("job <> 'PRESIDENT'");
  }

  public static final EntityType T_NO_PK = entityType("scott.no_pk_table");
  public static final Attribute<Integer> NO_PK_COL1 = T_NO_PK.integerAttribute("col1");
  public static final Attribute<String> NO_PK_COL2 = T_NO_PK.stringAttribute("col2");
  public static final Attribute<String> NO_PK_COL3 = T_NO_PK.stringAttribute("col3");
  public static final Attribute<Integer> NO_PK_COL4 = T_NO_PK.integerAttribute("col4");

  private void noPkEntity() {
    define(T_NO_PK,
            columnProperty(NO_PK_COL1),
            columnProperty(NO_PK_COL2),
            columnProperty(NO_PK_COL3),
            columnProperty(NO_PK_COL4));
  }

  public static final EntityType JOINED_QUERY_ENTITY_TYPE = entityType("joinedQueryEntityType");
  public static final Attribute<Integer> JOINED_EMPNO = JOINED_QUERY_ENTITY_TYPE.integerAttribute("e.empno");
  public static final Attribute<Integer> JOINED_DEPTNO = JOINED_QUERY_ENTITY_TYPE.integerAttribute("d.deptno");

  private void joinedQuery() {
    define(JOINED_QUERY_ENTITY_TYPE,
            primaryKeyProperty(JOINED_EMPNO),
            columnProperty(JOINED_DEPTNO))
            .selectQuery("select e.empno, d.deptno from scott.emp e, scott.dept d where e.deptno = d.deptno", true)
            .conditionProvider(JOINED_QUERY_CONDITION_ID, (attributes, values) -> "d.deptno = 10");
  }
}
