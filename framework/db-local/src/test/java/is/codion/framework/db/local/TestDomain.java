/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.reports.AbstractReport;
import is.codion.common.db.reports.Report;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.query.SelectQuery;
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

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public static final ReportType<Object, String, Map<String, Object>> REPORT = ReportType.reportType("report");

  public static final ProcedureType<EntityConnection, Object> PROCEDURE_ID = ProcedureType.procedureType("procedureId");
  public static final FunctionType<EntityConnection, Object, List<Object>> FUNCTION_ID = FunctionType.functionType("functionId");

  public TestDomain() {
    super(DOMAIN);
    department();
    employee();
    uuidTestDefaultValue();
    uuidTestNoDefaultValue();
    operations();
    joinedQuery();
    groupByQuery();
    noPkEntity();
    Report.REPORT_PATH.set("path/to/reports");
    defineReport(REPORT, new AbstractReport<Object, String, Map<String, Object>>("report.path") {
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

  public interface Department extends Entity {
    EntityType<Department> TYPE = DOMAIN.entityType("scott.dept", Department.class);
    Attribute<Integer> DEPTNO = TYPE.integerAttribute("deptno");
    Attribute<String> DNAME = TYPE.stringAttribute("dname");
    Attribute<String> LOC = TYPE.stringAttribute("loc");

    ConditionType DEPARTMENT_CONDITION_TYPE = TYPE.conditionType("condition");
    ConditionType DEPARTMENT_CONDITION_SALES_TYPE = TYPE.conditionType("conditionSalesId");
    ConditionType DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE = TYPE.conditionType("conditionInvalidColumnId");

    void setName(String name);
    String getName();
    void setId(Integer id);
    void setLocation(String location);
  }

  void department() {
    define(Department.TYPE,
            primaryKeyProperty(Department.DEPTNO, Department.DEPTNO.getName())
                    .updatable(true).nullable(false)
                    .beanProperty("id"),
            columnProperty(Department.DNAME, Department.DNAME.getName())
                    .searchProperty().preferredColumnWidth(120).maximumLength(14).nullable(false)
                    .beanProperty("name"),
            columnProperty(Department.LOC, Department.LOC.getName())
                    .preferredColumnWidth(150).maximumLength(13)
                    .beanProperty("location"))
            .smallDataset()
            .stringFactory(stringFactory(Department.DNAME))
            .conditionProvider(Department.DEPARTMENT_CONDITION_TYPE, (attributes, values) -> {
              final StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .conditionProvider(Department.DEPARTMENT_CONDITION_SALES_TYPE, (attributes, values) -> "dname = 'SALES'")
            .conditionProvider(Department.DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE, (attributes, values) -> "no_column is null")
            .caption("Department");
  }

  public static final EntityType<Entity> T_EMP = DOMAIN.entityType("scott.emp");
  public static final Attribute<Integer> EMP_ID = T_EMP.integerAttribute("empno");
  public static final Attribute<String> EMP_NAME = T_EMP.stringAttribute("ename");
  public static final Attribute<String> EMP_JOB = T_EMP.stringAttribute("job");
  public static final Attribute<Integer> EMP_MGR = T_EMP.integerAttribute("mgr");
  public static final Attribute<LocalDate> EMP_HIREDATE = T_EMP.localDateAttribute("hiredate");
  public static final Attribute<LocalDateTime> EMP_HIRETIME = T_EMP.localDateTimeAttribute("hiretime");
  public static final Attribute<Double> EMP_SALARY = T_EMP.doubleAttribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = T_EMP.doubleAttribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = T_EMP.integerAttribute("deptno");
  public static final ForeignKey EMP_DEPARTMENT_FK = T_EMP.foreignKey("dept_fk", EMP_DEPARTMENT, Department.DEPTNO);
  public static final ForeignKey EMP_MGR_FK = T_EMP.foreignKey("mgr_fk", EMP_MGR, EMP_ID);
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = T_EMP.stringAttribute("location");
  public static final Attribute<byte[]> EMP_DATA_LAZY = T_EMP.byteArrayAttribute("data_lazy");
  public static final Attribute<byte[]> EMP_DATA = T_EMP.byteArrayAttribute("data");

  public static final ConditionType EMP_NAME_IS_BLAKE_CONDITION_ID = T_EMP.conditionType("condition1Id");
  public static final ConditionType EMP_MGR_GREATER_THAN_CONDITION_ID = T_EMP.conditionType("condition2Id");

  void employee() {
    define(T_EMP,
            primaryKeyProperty(EMP_ID, EMP_ID.getName()),
            columnProperty(EMP_NAME, EMP_NAME.getName())
                    .searchProperty().maximumLength(10).nullable(false),
            columnProperty(EMP_DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK.getName()),
            valueListProperty(EMP_JOB, EMP_JOB.getName(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(),
            columnProperty(EMP_SALARY, EMP_SALARY.getName())
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, EMP_COMMISSION.getName())
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2),
            columnProperty(EMP_MGR),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK.getName())
                    //not really soft, just for testing purposes
                    .softReference(),
            columnProperty(EMP_HIREDATE, EMP_HIREDATE.getName())
                    .nullable(false),
            columnProperty(EMP_HIRETIME, EMP_HIRETIME.getName()),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, Department.LOC.getName(), EMP_DEPARTMENT_FK, Department.LOC).preferredColumnWidth(100),
            columnProperty(EMP_DATA_LAZY),
            blobProperty(EMP_DATA)
                    .eagerlyLoaded())
            .stringFactory(stringFactory(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .conditionProvider(EMP_NAME_IS_BLAKE_CONDITION_ID, (attributes, values) -> "ename = 'BLAKE'")
            .conditionProvider(EMP_MGR_GREATER_THAN_CONDITION_ID, (attributes, values) -> "mgr > ?")
            .caption("Employee");
  }

  public static final EntityType<Entity> T_UUID_TEST_DEFAULT = DOMAIN.entityType("scott.uuid_test_default");
  public static final Attribute<UUID> UUID_TEST_DEFAULT_ID = T_UUID_TEST_DEFAULT.attribute("id", UUID.class);
  public static final Attribute<String> UUID_TEST_DEFAULT_DATA = T_UUID_TEST_DEFAULT.stringAttribute("data");

  private void uuidTestDefaultValue() {
    final KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void afterInsert(final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties,
                              final DatabaseConnection connection, final Statement insertStatement) throws SQLException {
        try (final ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            entity.put(UUID_TEST_DEFAULT_ID, (UUID) generatedKeys.getObject(1));
          }
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

  public static final EntityType<Entity> T_UUID_TEST_NO_DEFAULT = DOMAIN.entityType("scott.uuid_test_no_default");
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

  public static final EntityType<Entity> GROUP_BY_QUERY_ENTITY_TYPE = DOMAIN.entityType("groupByQueryEntityType");

  private void groupByQuery() {
    define(GROUP_BY_QUERY_ENTITY_TYPE, "scott.emp",
            columnProperty(GROUP_BY_QUERY_ENTITY_TYPE.stringAttribute("job"))
                    .primaryKeyIndex(0)
                    .groupingColumn())
            .havingClause("job <> 'PRESIDENT'");
  }

  public static final EntityType<Entity> T_NO_PK = DOMAIN.entityType("scott.no_pk_table");
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

  public static final EntityType<Entity> JOINED_QUERY_ENTITY_TYPE = DOMAIN.entityType("joinedQueryEntityType");
  public static final Attribute<Integer> JOINED_EMPNO = JOINED_QUERY_ENTITY_TYPE.integerAttribute("e.empno");
  public static final Attribute<Integer> JOINED_DEPTNO = JOINED_QUERY_ENTITY_TYPE.integerAttribute("d.deptno");

  public static final ConditionType JOINED_QUERY_CONDITION_TYPE = JOINED_QUERY_ENTITY_TYPE.conditionType("conditionId");

  private void joinedQuery() {
    define(JOINED_QUERY_ENTITY_TYPE,
            columnProperty(JOINED_DEPTNO),
            primaryKeyProperty(JOINED_EMPNO))
            .selectQuery(SelectQuery.builder().
                    fromClause("scott.emp e, scott.dept d")
                    .whereClause("e.deptno = d.deptno")
                    .build())
            .conditionProvider(JOINED_QUERY_CONDITION_TYPE, (attributes, values) -> "d.deptno = 10");
  }
}
