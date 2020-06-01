/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.AbstractDatabaseFunction;
import is.codion.common.db.operation.AbstractDatabaseProcedure;
import is.codion.common.db.reports.AbstractReportWrapper;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportWrapper;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Identity;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.Attributes;
import is.codion.framework.domain.property.BlobAttribute;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.EntityAttribute;

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
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.property.Attributes.integerAttribute;
import static is.codion.framework.domain.property.Attributes.stringAttribute;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

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
    ReportWrapper.REPORT_PATH.set("path/to/reports");
    addReport(REPORT);
  }

  public static final Identity T_DEPARTMENT = Identity.identity("scott.dept");
  public static final Attribute<Integer> DEPARTMENT_ID = integerAttribute("deptno", T_DEPARTMENT);
  public static final Attribute<String> DEPARTMENT_NAME = stringAttribute("dname", T_DEPARTMENT);
  public static final Attribute<String> DEPARTMENT_LOCATION = stringAttribute("loc", T_DEPARTMENT);

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

  public static final Identity T_EMP = Identity.identity("scott.emp");
  public static final Attribute<Integer> EMP_ID = Attributes.integerAttribute("empno", T_EMP);
  public static final Attribute<String> EMP_NAME = Attributes.stringAttribute("ename", T_EMP);
  public static final Attribute<String> EMP_JOB = Attributes.stringAttribute("job", T_EMP);
  public static final Attribute<Integer> EMP_MGR = Attributes.integerAttribute("mgr", T_EMP);
  public static final Attribute<LocalDate> EMP_HIREDATE = Attributes.localDateAttribute("hiredate", T_EMP);
  public static final Attribute<LocalDateTime> EMP_HIRETIME = Attributes.localDateTimeAttribute("hiretime", T_EMP);
  public static final Attribute<Double> EMP_SALARY = Attributes.doubleAttribute("sal", T_EMP);
  public static final Attribute<Double> EMP_COMMISSION = Attributes.doubleAttribute("comm", T_EMP);
  public static final Attribute<Integer> EMP_DEPARTMENT = Attributes.integerAttribute("deptno", T_EMP);
  public static final EntityAttribute EMP_DEPARTMENT_FK = Attributes.entityAttribute("dept_fk", T_EMP);
  public static final EntityAttribute EMP_MGR_FK = Attributes.entityAttribute("mgr_fk", T_EMP);
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = Attributes.stringAttribute("location", T_EMP);
  public static final BlobAttribute EMP_DATA_LAZY = Attributes.blobAttribute("data_lazy", T_EMP);
  public static final BlobAttribute EMP_DATA = Attributes.blobAttribute("data", T_EMP);

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

  public static final Identity T_UUID_TEST_DEFAULT = Identity.identity("scott.uuid_test_default");
  public static final Attribute<UUID> UUID_TEST_DEFAULT_ID = Attributes.attribute("id", UUID.class, T_UUID_TEST_DEFAULT);
  public static final Attribute<String> UUID_TEST_DEFAULT_DATA = Attributes.stringAttribute("data", T_UUID_TEST_DEFAULT);

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

  public static final Identity T_UUID_TEST_NO_DEFAULT = Identity.identity("scott.uuid_test_no_default");
  public static final Attribute<UUID> UUID_TEST_NO_DEFAULT_ID = Attributes.attribute("id", UUID.class, T_UUID_TEST_NO_DEFAULT);
  public static final Attribute<String> UUID_TEST_NO_DEFAULT_DATA = Attributes.stringAttribute("data", T_UUID_TEST_NO_DEFAULT);

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

  public static final Identity GROUP_BY_QUERY_ENTITY_ID = Identity.identity("groupByQueryEntityID");
  public static final String JOINED_QUERY_CONDITION_ID = "conditionId";

  private void groupByQuery() {
    define(GROUP_BY_QUERY_ENTITY_ID, "scott.emp",
            columnProperty(Attributes.stringAttribute("job", GROUP_BY_QUERY_ENTITY_ID))
                    .primaryKeyIndex(0)
                    .groupingColumn(true))
            .havingClause("job <> 'PRESIDENT'");
  }

  public static final Identity T_NO_PK = Identity.identity("scott.no_pk_table");
  public static final Attribute<Integer> NO_PK_COL1 = Attributes.integerAttribute("col1", T_NO_PK);
  public static final Attribute<String> NO_PK_COL2 = Attributes.stringAttribute("col2", T_NO_PK);
  public static final Attribute<String> NO_PK_COL3 = Attributes.stringAttribute("col3", T_NO_PK);
  public static final Attribute<Integer> NO_PK_COL4 = Attributes.integerAttribute("col4", T_NO_PK);

  private void noPkEntity() {
    define(T_NO_PK,
            columnProperty(NO_PK_COL1),
            columnProperty(NO_PK_COL2),
            columnProperty(NO_PK_COL3),
            columnProperty(NO_PK_COL4));
  }

  public static final Identity JOINED_QUERY_ENTITY_ID = Identity.identity("joinedQueryEntityID");
  public static final Attribute<Integer> JOINED_EMPNO = Attributes.integerAttribute("e.empno", JOINED_QUERY_ENTITY_ID);
  public static final Attribute<Integer> JOINED_DEPTNO = Attributes.integerAttribute("d.deptno", JOINED_QUERY_ENTITY_ID);

  private void joinedQuery() {
    define(JOINED_QUERY_ENTITY_ID,
            primaryKeyProperty(JOINED_EMPNO),
            columnProperty(JOINED_DEPTNO))
            .selectQuery("select e.empno, d.deptno from scott.emp e, scott.dept d where e.deptno = d.deptno", true)
            .conditionProvider(JOINED_QUERY_CONDITION_ID, (attributes, values) -> "d.deptno = 10");
  }
}
