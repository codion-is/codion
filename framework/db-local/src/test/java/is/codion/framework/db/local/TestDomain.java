/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.AbstractReport;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.query.SelectQuery;
import is.codion.framework.domain.property.ColumnProperty;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
    departmentFk();
    employeeFk();
    uuidTestDefaultValue();
    uuidTestNoDefaultValue();
    operations();
    empnoDeptno();
    job();
    noPkEntity();
    Report.REPORT_PATH.set("path/to/reports");
    defineReport(REPORT, new AbstractReport<Object, String, Map<String, Object>>("report.path") {
      @Override
      public String fillReport(Connection connection, Map<String, Object> parameters) throws ReportException {
        return "result";
      }

      @Override
      public Object loadReport() throws ReportException {
        return null;
      }
    });
    query();
    queryColumnsWhereClause();
    queryFromClause();
    queryFromWhereClause();
  }

  public interface Department extends Entity {
    EntityType TYPE = DOMAIN.entityType("scott.dept", Department.class);

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
                    .updatable(true)
                    .nullable(false)
                    .beanProperty("id"),
            columnProperty(Department.DNAME, Department.DNAME.getName())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false)
                    .beanProperty("name"),
            columnProperty(Department.LOC, Department.LOC.getName())
                    .preferredColumnWidth(150).maximumLength(13)
                    .beanProperty("location"))
            .smallDataset()
            .stringFactory(stringFactory(Department.DNAME))
            .conditionProvider(Department.DEPARTMENT_CONDITION_TYPE, (attributes, values) -> {
              StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .conditionProvider(Department.DEPARTMENT_CONDITION_SALES_TYPE, (attributes, values) -> "dname = 'SALES'")
            .conditionProvider(Department.DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE, (attributes, values) -> "no_column is null")
            .caption("Department");
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("scott.emp");

    Attribute<Integer> ID = TYPE.integerAttribute("empno");
    Attribute<String> NAME = TYPE.stringAttribute("ename");
    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Integer> MGR = TYPE.integerAttribute("mgr");
    Attribute<LocalDate> HIREDATE = TYPE.localDateAttribute("hiredate");
    Attribute<OffsetDateTime> HIRETIME = TYPE.offsetDateTimeAttribute("hiretime");
    Attribute<Double> SALARY = TYPE.doubleAttribute("sal");
    Attribute<Double> COMMISSION = TYPE.doubleAttribute("comm");
    Attribute<Integer> DEPARTMENT = TYPE.integerAttribute("deptno");
    Attribute<String> DEPARTMENT_LOCATION = TYPE.stringAttribute("location");
    Attribute<byte[]> DATA_LAZY = TYPE.byteArrayAttribute("data_lazy");
    Attribute<byte[]> DATA = TYPE.byteArrayAttribute("data");

    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.DEPTNO);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);

    ConditionType NAME_IS_BLAKE_CONDITION_ID = TYPE.conditionType("condition1Id");
    ConditionType MGR_GREATER_THAN_CONDITION_ID = TYPE.conditionType("condition2Id");
  }

  void employee() {
    define(Employee.TYPE,
            primaryKeyProperty(Employee.ID, Employee.ID.getName()),
            columnProperty(Employee.NAME, Employee.NAME.getName())
                    .searchProperty(true).maximumLength(10).nullable(false),
            columnProperty(Employee.DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(Employee.DEPARTMENT_FK, Employee.DEPARTMENT_FK.getName()),
            itemProperty(Employee.JOB, Employee.JOB.getName(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(Employee.SALARY, Employee.SALARY.getName())
                    .nullable(false).range(1000, 10000).maximumFractionDigits(2),
            columnProperty(Employee.COMMISSION, Employee.COMMISSION.getName())
                    .range(100, 2000).maximumFractionDigits(2),
            columnProperty(Employee.MGR),
            foreignKeyProperty(Employee.MGR_FK, Employee.MGR_FK.getName())
                    //not really soft, just for testing purposes
                    .softReference(),
            columnProperty(Employee.HIREDATE, Employee.HIREDATE.getName())
                    .nullable(false),
            columnProperty(Employee.HIRETIME, Employee.HIRETIME.getName()),
            denormalizedViewProperty(Employee.DEPARTMENT_LOCATION, Department.LOC.getName(), Employee.DEPARTMENT_FK, Department.LOC).preferredColumnWidth(100),
            columnProperty(Employee.DATA_LAZY),
            blobProperty(Employee.DATA)
                    .eagerlyLoaded())
            .stringFactory(stringFactory(Employee.NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .conditionProvider(Employee.NAME_IS_BLAKE_CONDITION_ID, (attributes, values) -> "ename = 'BLAKE'")
            .conditionProvider(Employee.MGR_GREATER_THAN_CONDITION_ID, (attributes, values) -> "mgr > ?")
            .caption("Employee");
  }

  public interface DepartmentFk extends Entity {
    EntityType TYPE = DOMAIN.entityType("scott.deptfk");

    Attribute<Integer> DEPTNO = TYPE.integerAttribute("deptno");
    Attribute<String> DNAME = TYPE.stringAttribute("dname");
    Attribute<String> LOC = TYPE.stringAttribute("loc");
  }

  void departmentFk() {
    define(DepartmentFk.TYPE, "scott.dept",
            primaryKeyProperty(DepartmentFk.DEPTNO, Department.DEPTNO.getName()),
            columnProperty(DepartmentFk.DNAME, DepartmentFk.DNAME.getName()),
            columnProperty(DepartmentFk.LOC, DepartmentFk.LOC.getName()))
            .stringFactory(stringFactory(DepartmentFk.DNAME));
  }

  public interface EmployeeFk {
    EntityType TYPE = DOMAIN.entityType("scott.empfk");

    Attribute<Integer> ID = TYPE.integerAttribute("empno");
    Attribute<String> NAME = TYPE.stringAttribute("ename");
    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Integer> MGR = TYPE.integerAttribute("mgr");
    Attribute<LocalDate> HIREDATE = TYPE.localDateAttribute("hiredate");
    Attribute<OffsetDateTime> HIRETIME = TYPE.offsetDateTimeAttribute("hiretime");
    Attribute<Double> SALARY = TYPE.doubleAttribute("sal");
    Attribute<Double> COMMISSION = TYPE.doubleAttribute("comm");
    Attribute<Integer> DEPARTMENT = TYPE.integerAttribute("deptno");

    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, DepartmentFk.DEPTNO);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
  }

  void employeeFk() {
    define(EmployeeFk.TYPE, "scott.emp",
            primaryKeyProperty(EmployeeFk.ID, EmployeeFk.ID.getName()),
            columnProperty(EmployeeFk.NAME, EmployeeFk.NAME.getName())
                    .nullable(false),
            columnProperty(EmployeeFk.DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(EmployeeFk.DEPARTMENT_FK, EmployeeFk.DEPARTMENT_FK.getName())
                    .selectAttributes(DepartmentFk.DNAME),
            columnProperty(EmployeeFk.JOB, EmployeeFk.JOB.getName()),
            columnProperty(EmployeeFk.SALARY, EmployeeFk.SALARY.getName())
                    .maximumFractionDigits(2),
            columnProperty(EmployeeFk.COMMISSION, EmployeeFk.COMMISSION.getName()),
            columnProperty(EmployeeFk.MGR),
            foreignKeyProperty(EmployeeFk.MGR_FK, EmployeeFk.MGR_FK.getName())
                    .selectAttributes(EmployeeFk.NAME, EmployeeFk.JOB, EmployeeFk.DEPARTMENT_FK),
            columnProperty(EmployeeFk.HIREDATE, EmployeeFk.HIREDATE.getName())
                    .nullable(false),
            columnProperty(EmployeeFk.HIRETIME, EmployeeFk.HIRETIME.getName()))
            .stringFactory(stringFactory(EmployeeFk.NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .caption("Employee");
  }

  public interface UUIDTestDefault {
    EntityType TYPE = DOMAIN.entityType("scott.uuid_test_default");

    Attribute<UUID> ID = TYPE.attribute("id", UUID.class);
    Attribute<String> DATA = TYPE.stringAttribute("data");
  }

  private void uuidTestDefaultValue() {
    KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void afterInsert(Entity entity, List<ColumnProperty<?>> primaryKeyProperties,
                              DatabaseConnection connection, Statement insertStatement) throws SQLException {
        try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            entity.put(UUIDTestDefault.ID, (UUID) generatedKeys.getObject(1));
          }
        }
      }
      @Override
      public boolean returnGeneratedKeys() {
        return true;
      }
    };
    define(UUIDTestDefault.TYPE,
            primaryKeyProperty(UUIDTestDefault.ID, "Id"),
            columnProperty(UUIDTestDefault.DATA, "Data"))
            .keyGenerator(uuidKeyGenerator);
  }

  public interface UUIDTestNoDefault {
    EntityType TYPE = DOMAIN.entityType("scott.uuid_test_no_default");

    Attribute<UUID> ID = TYPE.attribute("id", UUID.class);
    Attribute<String> DATA = TYPE.stringAttribute("data");
  }

  private void uuidTestNoDefaultValue() {
    KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void beforeInsert(Entity entity, List<ColumnProperty<?>> primaryKeyProperties,
                               DatabaseConnection connection) throws SQLException {
        entity.put(UUIDTestNoDefault.ID, UUID.randomUUID());
      }
    };
    define(UUIDTestNoDefault.TYPE,
            primaryKeyProperty(UUIDTestNoDefault.ID, "Id"),
            columnProperty(UUIDTestNoDefault.DATA, "Data"))
            .keyGenerator(uuidKeyGenerator);
  }

  private void operations() {
    defineProcedure(PROCEDURE_ID, (connection, arguments) -> {});
    defineFunction(FUNCTION_ID, (connection, arguments) -> null);
  }

  public interface Job {
    EntityType TYPE = DOMAIN.entityType("job");

    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Double> MAX_SALARY = TYPE.doubleAttribute("max_salary");
    Attribute<Double> MIN_SALARY = TYPE.doubleAttribute("min_salary");
    Attribute<Double> MAX_COMMISSION = TYPE.doubleAttribute("max_commission");
    Attribute<Double> MIN_COMMISSION = TYPE.doubleAttribute("min_commission");
  }

  private void job() {
    define(Job.TYPE, "scott.emp",
            columnProperty(Job.JOB)
                    .primaryKeyIndex(0)
                    .groupingColumn(true),
            columnProperty(Job.MAX_SALARY)
                    .columnExpression("max(sal)")
                    .aggregateColumn(true),
            columnProperty(Job.MIN_SALARY)
                    .columnExpression("min(sal)")
                    .aggregateColumn(true),
            columnProperty(Job.MAX_COMMISSION)
                    .columnExpression("max(comm)")
                    .aggregateColumn(true),
            columnProperty(Job.MIN_COMMISSION)
                    .columnExpression("min(comm)")
                    .aggregateColumn(true))
            .selectQuery(SelectQuery.builder()
                    .having("job <> 'PRESIDENT'")
                    .build());
  }

  public interface NoPrimaryKey {
    EntityType TYPE = DOMAIN.entityType("scott.no_pk_table");

    Attribute<Integer> COL_4 = TYPE.integerAttribute("col4");
    Attribute<String> COL_3 = TYPE.stringAttribute("col3");
    Attribute<String> COL_2 = TYPE.stringAttribute("col2");
    Attribute<Integer> COL_1 = TYPE.integerAttribute("col1");
  }

  private void noPkEntity() {
    define(NoPrimaryKey.TYPE,
            columnProperty(NoPrimaryKey.COL_1),
            columnProperty(NoPrimaryKey.COL_2),
            columnProperty(NoPrimaryKey.COL_3),
            columnProperty(NoPrimaryKey.COL_4));
  }

  public interface EmpnoDeptno {
    EntityType TYPE = DOMAIN.entityType("joinedQueryEntityType");

    Attribute<Integer> DEPTNO = TYPE.integerAttribute("d.deptno");
    Attribute<Integer> EMPNO = TYPE.integerAttribute("e.empno");

    ConditionType CONDITION = EmpnoDeptno.TYPE.conditionType("condition");
  }

  private void empnoDeptno() {
    define(EmpnoDeptno.TYPE,
            columnProperty(EmpnoDeptno.DEPTNO),
            primaryKeyProperty(EmpnoDeptno.EMPNO))
            .selectQuery(SelectQuery.builder()
                    .from("scott.emp e, scott.dept d")
                    .where("e.deptno = d.deptno")
                    .orderBy("e.deptno, e.ename")
                    .build())
            .conditionProvider(EmpnoDeptno.CONDITION, (attributes, values) -> "d.deptno = 10");
  }

  public interface Query {
    EntityType TYPE = DOMAIN.entityType("query");

    Attribute<Integer> EMPNO = TYPE.integerAttribute("empno");
    Attribute<String> ENAME = TYPE.stringAttribute("ename");
  }

  private void query() {
    define(Query.TYPE, "scott.emp",
            columnProperty(Query.EMPNO),
            columnProperty(Query.ENAME))
            .orderBy(OrderBy.orderBy().descending(Query.ENAME))
            .selectTableName("scott.emp e")
            .selectQuery(SelectQuery.builder()
                    .columns("empno, ename")
                    .orderBy("ename")
                    .build());
  }

  public interface QueryColumnsWhereClause {
    EntityType TYPE = DOMAIN.entityType("query_where");

    Attribute<Integer> EMPNO = TYPE.integerAttribute("empno");
    Attribute<String> ENAME = TYPE.stringAttribute("ename");
  }

  private void queryColumnsWhereClause() {
    define(QueryColumnsWhereClause.TYPE, "scott.emp e",
            columnProperty(QueryColumnsWhereClause.EMPNO),
            columnProperty(QueryColumnsWhereClause.ENAME))
            .orderBy(OrderBy.orderBy().descending(QueryColumnsWhereClause.ENAME))
            .selectQuery(SelectQuery.builder()
                    .columns("e.empno, e.ename")
                    .where("e.deptno > 10")
                    .build());
  }

  public interface QueryFromClause {
    EntityType TYPE = DOMAIN.entityType("query_from");

    Attribute<Integer> EMPNO = TYPE.integerAttribute("empno");
    Attribute<String> ENAME = TYPE.stringAttribute("ename");
  }

  private void queryFromClause() {
    define(QueryFromClause.TYPE,
            columnProperty(QueryFromClause.EMPNO),
            columnProperty(QueryFromClause.ENAME))
            .orderBy(OrderBy.orderBy().descending(QueryFromClause.ENAME))
            .selectQuery(SelectQuery.builder()
                    .from("scott.emp")
                    .orderBy("ename")
                    .build());
  }

  public interface QueryFromWhereClause {
    EntityType TYPE = DOMAIN.entityType("query_from_where");

    Attribute<Integer> EMPNO = TYPE.integerAttribute("empno");
    Attribute<String> ENAME = TYPE.stringAttribute("ename");
  }

  private void queryFromWhereClause() {
    define(QueryFromWhereClause.TYPE,
            columnProperty(QueryFromWhereClause.EMPNO),
            columnProperty(QueryFromWhereClause.ENAME))
            .orderBy(OrderBy.orderBy().descending(QueryFromWhereClause.ENAME))
            .selectQuery(SelectQuery.builder()
                    .from("scott.emp")
                    .where("deptno > 10")
                    .orderBy("deptno")
                    .build());
  }
}
