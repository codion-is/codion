/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.property.Property.*;
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
    add(REPORT, new AbstractReport<Object, String, Map<String, Object>>("report.path", false) {
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
    master();
    detail();
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
    add(definition(
            primaryKeyProperty(Department.DEPTNO, Department.DEPTNO.name())
                    .updatable(true)
                    .nullable(false)
                    .beanProperty("id"),
            columnProperty(Department.DNAME, Department.DNAME.name())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false)
                    .beanProperty("name"),
            columnProperty(Department.LOC, Department.LOC.name())
                    .preferredColumnWidth(150).maximumLength(13)
                    .beanProperty("location"))
            .smallDataset(true)
            .stringFactory(Department.DNAME)
            .conditionProvider(Department.DEPARTMENT_CONDITION_TYPE, (attributes, values) -> {
              StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .conditionProvider(Department.DEPARTMENT_CONDITION_SALES_TYPE, (attributes, values) -> "dname = 'SALES'")
            .conditionProvider(Department.DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE, (attributes, values) -> "no_column is null")
            .caption("Department"));
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
    add(definition(
            primaryKeyProperty(Employee.ID, Employee.ID.name()),
            columnProperty(Employee.NAME, Employee.NAME.name())
                    .searchProperty(true).maximumLength(10).nullable(false),
            columnProperty(Employee.DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(Employee.DEPARTMENT_FK, Employee.DEPARTMENT_FK.name()),
            itemProperty(Employee.JOB, Employee.JOB.name(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(Employee.SALARY, Employee.SALARY.name())
                    .nullable(false).valueRange(1000, 10000).maximumFractionDigits(2),
            columnProperty(Employee.COMMISSION, Employee.COMMISSION.name())
                    .valueRange(100, 2000).maximumFractionDigits(2),
            columnProperty(Employee.MGR),
            foreignKeyProperty(Employee.MGR_FK, Employee.MGR_FK.name())
                    //not really soft, just for testing purposes
                    .softReference(true),
            columnProperty(Employee.HIREDATE, Employee.HIREDATE.name())
                    .nullable(false),
            columnProperty(Employee.HIRETIME, Employee.HIRETIME.name()),
            denormalizedViewProperty(Employee.DEPARTMENT_LOCATION, Department.LOC.name(), Employee.DEPARTMENT_FK, Department.LOC).preferredColumnWidth(100),
            columnProperty(Employee.DATA_LAZY),
            blobProperty(Employee.DATA)
                    .eagerlyLoaded(true))
            .stringFactory(Employee.NAME)
            .keyGenerator(increment("scott.emp", "empno"))
            .conditionProvider(Employee.NAME_IS_BLAKE_CONDITION_ID, (attributes, values) -> "ename = 'BLAKE'")
            .conditionProvider(Employee.MGR_GREATER_THAN_CONDITION_ID, (attributes, values) -> "mgr > ?")
            .caption("Employee"));
  }

  public interface DepartmentFk extends Entity {
    EntityType TYPE = DOMAIN.entityType("scott.deptfk");

    Attribute<Integer> DEPTNO = TYPE.integerAttribute("deptno");
    Attribute<String> DNAME = TYPE.stringAttribute("dname");
    Attribute<String> LOC = TYPE.stringAttribute("loc");
  }

  void departmentFk() {
    add(definition(
            primaryKeyProperty(DepartmentFk.DEPTNO, Department.DEPTNO.name()),
            columnProperty(DepartmentFk.DNAME, DepartmentFk.DNAME.name()),
            columnProperty(DepartmentFk.LOC, DepartmentFk.LOC.name()))
            .tableName("scott.dept")
            .stringFactory(DepartmentFk.DNAME));
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
    add(definition(
            primaryKeyProperty(EmployeeFk.ID, EmployeeFk.ID.name()),
            columnProperty(EmployeeFk.NAME, EmployeeFk.NAME.name())
                    .nullable(false),
            columnProperty(EmployeeFk.DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(EmployeeFk.DEPARTMENT_FK, EmployeeFk.DEPARTMENT_FK.name())
                    .selectAttributes(DepartmentFk.DNAME),
            columnProperty(EmployeeFk.JOB, EmployeeFk.JOB.name()),
            columnProperty(EmployeeFk.SALARY, EmployeeFk.SALARY.name())
                    .maximumFractionDigits(2),
            columnProperty(EmployeeFk.COMMISSION, EmployeeFk.COMMISSION.name()),
            columnProperty(EmployeeFk.MGR),
            foreignKeyProperty(EmployeeFk.MGR_FK, EmployeeFk.MGR_FK.name())
                    .selectAttributes(EmployeeFk.NAME, EmployeeFk.JOB, EmployeeFk.DEPARTMENT_FK),
            columnProperty(EmployeeFk.HIREDATE, EmployeeFk.HIREDATE.name())
                    .nullable(false),
            columnProperty(EmployeeFk.HIRETIME, EmployeeFk.HIRETIME.name()))
            .tableName("scott.emp")
            .stringFactory(EmployeeFk.NAME)
            .keyGenerator(increment("scott.emp", "empno"))
            .caption("Employee"));
  }

  public interface UUIDTestDefault {
    EntityType TYPE = DOMAIN.entityType("scott.uuid_test_default");

    Attribute<UUID> ID = TYPE.attribute("id", UUID.class);
    Attribute<String> DATA = TYPE.stringAttribute("data");
  }

  private void uuidTestDefaultValue() {
    KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void afterInsert(Entity entity, DatabaseConnection connection, Statement insertStatement) throws SQLException {
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
    add(definition(
            primaryKeyProperty(UUIDTestDefault.ID, "Id"),
            columnProperty(UUIDTestDefault.DATA, "Data"))
            .keyGenerator(uuidKeyGenerator));
  }

  public interface UUIDTestNoDefault {
    EntityType TYPE = DOMAIN.entityType("scott.uuid_test_no_default");

    Attribute<UUID> ID = TYPE.attribute("id", UUID.class);
    Attribute<String> DATA = TYPE.stringAttribute("data");
  }

  private void uuidTestNoDefaultValue() {
    KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void beforeInsert(Entity entity, DatabaseConnection connection) throws SQLException {
        entity.put(UUIDTestNoDefault.ID, UUID.randomUUID());
      }
    };
    add(definition(
            primaryKeyProperty(UUIDTestNoDefault.ID, "Id"),
            columnProperty(UUIDTestNoDefault.DATA, "Data"))
            .keyGenerator(uuidKeyGenerator));
  }

  private void operations() {
    add(PROCEDURE_ID, (connection, arguments) -> {});
    add(FUNCTION_ID, (connection, arguments) -> null);
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
    add(definition(
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
            .tableName("scott.emp")
            .selectQuery(SelectQuery.builder()
                    .having("job <> 'PRESIDENT'")
                    .build()));
  }

  public interface NoPrimaryKey {
    EntityType TYPE = DOMAIN.entityType("scott.no_pk_table");

    Attribute<Integer> COL_4 = TYPE.integerAttribute("col4");
    Attribute<String> COL_3 = TYPE.stringAttribute("col3");
    Attribute<String> COL_2 = TYPE.stringAttribute("col2");
    Attribute<Integer> COL_1 = TYPE.integerAttribute("col1");
  }

  private void noPkEntity() {
    add(definition(
            columnProperty(NoPrimaryKey.COL_1),
            columnProperty(NoPrimaryKey.COL_2),
            columnProperty(NoPrimaryKey.COL_3),
            columnProperty(NoPrimaryKey.COL_4)));
  }

  public interface EmpnoDeptno {
    EntityType TYPE = DOMAIN.entityType("joinedQueryEntityType");

    Attribute<Integer> DEPTNO = TYPE.integerAttribute("d.deptno");
    Attribute<Integer> EMPNO = TYPE.integerAttribute("e.empno");

    ConditionType CONDITION = EmpnoDeptno.TYPE.conditionType("condition");
  }

  private void empnoDeptno() {
    add(definition(
            columnProperty(EmpnoDeptno.DEPTNO),
            primaryKeyProperty(EmpnoDeptno.EMPNO))
            .selectQuery(SelectQuery.builder()
                    .from("scott.emp e, scott.dept d")
                    .where("e.deptno = d.deptno")
                    .orderBy("e.deptno, e.ename")
                    .build())
            .conditionProvider(EmpnoDeptno.CONDITION, (attributes, values) -> "d.deptno = 10"));
  }

  public interface Query {
    EntityType TYPE = DOMAIN.entityType("query");

    Attribute<Integer> EMPNO = TYPE.integerAttribute("empno");
    Attribute<String> ENAME = TYPE.stringAttribute("ename");
  }

  private void query() {
    add(definition(
            columnProperty(Query.EMPNO),
            columnProperty(Query.ENAME))
            .tableName("scott.emp")
            .orderBy(OrderBy.descending(Query.ENAME))
            .selectTableName("scott.emp e")
            .selectQuery(SelectQuery.builder()
                    .columns("empno, ename")
                    .orderBy("ename")
                    .build()));
  }

  public interface QueryColumnsWhereClause {
    EntityType TYPE = DOMAIN.entityType("query_where");

    Attribute<Integer> EMPNO = TYPE.integerAttribute("empno");
    Attribute<String> ENAME = TYPE.stringAttribute("ename");
  }

  private void queryColumnsWhereClause() {
    add(definition(
            columnProperty(QueryColumnsWhereClause.EMPNO),
            columnProperty(QueryColumnsWhereClause.ENAME))
            .tableName("scott.emp e")
            .orderBy(OrderBy.descending(QueryColumnsWhereClause.ENAME))
            .selectQuery(SelectQuery.builder()
                    .columns("e.empno, e.ename")
                    .where("e.deptno > 10")
                    .build()));
  }

  public interface QueryFromClause {
    EntityType TYPE = DOMAIN.entityType("query_from");

    Attribute<Integer> EMPNO = TYPE.integerAttribute("empno");
    Attribute<String> ENAME = TYPE.stringAttribute("ename");
  }

  private void queryFromClause() {
    add(definition(
            columnProperty(QueryFromClause.EMPNO),
            columnProperty(QueryFromClause.ENAME))
            .orderBy(OrderBy.descending(QueryFromClause.ENAME))
            .selectQuery(SelectQuery.builder()
                    .from("scott.emp")
                    .orderBy("ename")
                    .build()));
  }

  public interface QueryFromWhereClause {
    EntityType TYPE = DOMAIN.entityType("query_from_where");

    Attribute<Integer> EMPNO = TYPE.integerAttribute("empno");
    Attribute<String> ENAME = TYPE.stringAttribute("ename");
  }

  private void queryFromWhereClause() {
    add(definition(
            columnProperty(QueryFromWhereClause.EMPNO),
            columnProperty(QueryFromWhereClause.ENAME))
            .orderBy(OrderBy.descending(QueryFromWhereClause.ENAME))
            .selectQuery(SelectQuery.builder()
                    .from("scott.emp")
                    .where("deptno > 10")
                    .orderBy("deptno")
                    .build()));
  }

  public interface Master {
    EntityType TYPE = DOMAIN.entityType("scott.master");

    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> DATA = TYPE.stringAttribute("data");
  }

  void master() {
    add(definition(
            primaryKeyProperty(Master.ID),
            columnProperty(Master.DATA))
            .keyGenerator(identity()));
  }

  public interface Detail {
    EntityType TYPE = DOMAIN.entityType("scott.detail");

    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<Integer> MASTER_1_ID = TYPE.integerAttribute("master_1_id");
    Attribute<Integer> MASTER_2_ID = TYPE.integerAttribute("master_2_id");

    ForeignKey MASTER_1_FK = TYPE.foreignKey("master_1_fk", MASTER_1_ID, Master.ID);
    ForeignKey MASTER_2_FK = TYPE.foreignKey("master_2_fk", MASTER_2_ID, Master.ID);
  }

  void detail() {
    add(definition(
            primaryKeyProperty(Detail.ID),
            columnProperty(Detail.MASTER_1_ID),
            foreignKeyProperty(Detail.MASTER_1_FK),
            columnProperty(Detail.MASTER_2_ID),
            foreignKeyProperty(Detail.MASTER_2_FK))
            .keyGenerator(identity()));
  }
}
