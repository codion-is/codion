/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.AbstractReport;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ConditionType;
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
import static is.codion.framework.domain.entity.KeyGenerator.identity;
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
      public String fill(Connection connection, Map<String, Object> parameters) {
        return "result";
      }

      @Override
      public Object load() {
        return null;
      }
    });
    query();
    queryColumnsWhereClause();
    queryFromClause();
    queryFromWhereClause();
    master();
    detail();
    masterFk();
    detailFk();
    employeeNonOpt();
  }

  public interface Department extends Entity {
    EntityType TYPE = DOMAIN.entityType("scott.dept", Department.class);

    Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
    Column<String> DNAME = TYPE.stringColumn("dname");
    Column<String> LOC = TYPE.stringColumn("loc");
    Attribute<Boolean> ACTIVE = TYPE.booleanAttribute("active");
    Attribute<byte[]> DATA = TYPE.byteArrayAttribute("data");

    ConditionType DEPARTMENT_CONDITION_TYPE = TYPE.conditionType("condition");
    ConditionType DEPARTMENT_CONDITION_SALES_TYPE = TYPE.conditionType("conditionSalesId");
    ConditionType DEPARTMENT_CONDITION_INVALID_COLUMN_TYPE = TYPE.conditionType("conditionInvalidColumnId");

    void setName(String name);

    String getName();

    void setId(Integer id);

    void setLocation(String location);
  }

  void department() {
    add(Department.TYPE.define(
            Department.DEPTNO.define()
                    .primaryKey()
                    .caption(Department.DEPTNO.name())
                    .updatable(true)
                    .nullable(false)
                    .beanProperty("id"),
            Department.DNAME.define()
                    .column()
                    .caption(Department.DNAME.name())
                    .searchColumn(true)
                    .maximumLength(14)
                    .nullable(false)
                    .beanProperty("name"),
            Department.LOC.define()
                    .column()
                    .caption(Department.LOC.name())
                    .maximumLength(13)
                    .beanProperty("location"),
            Department.ACTIVE.define()
                    .attribute(),
            Department.DATA.define()
                    .attribute())
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

    Column<Integer> ID = TYPE.integerColumn("empno");
    Column<String> NAME = TYPE.stringColumn("ename");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Integer> MGR = TYPE.integerColumn("mgr");
    Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
    Column<OffsetDateTime> HIRETIME = TYPE.offsetDateTimeColumn("hiretime");
    Column<Double> SALARY = TYPE.doubleColumn("sal");
    Column<Double> COMMISSION = TYPE.doubleColumn("comm");
    Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
    Column<String> DEPARTMENT_LOCATION = TYPE.stringColumn("location");
    Column<byte[]> DATA_LAZY = TYPE.byteArrayColumn("data_lazy");
    Column<byte[]> DATA = TYPE.byteArrayColumn("data");

    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.DEPTNO);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);

    ConditionType NAME_IS_BLAKE_CONDITION = TYPE.conditionType("condition1Id");
    ConditionType MGR_GREATER_THAN_CONDITION = TYPE.conditionType("condition2Id");
  }

  void employee() {
    add(Employee.TYPE.define(
            Employee.ID.define()
                    .primaryKey()
                    .caption(Employee.ID.name()),
            Employee.NAME.define()
                    .column()
                    .caption(Employee.NAME.name())
                    .searchColumn(true).maximumLength(10).nullable(false),
            Employee.DEPARTMENT.define()
                    .column()
                    .nullable(false),
            Employee.DEPARTMENT_FK.define()
                    .foreignKey()
                    .caption(Employee.DEPARTMENT_FK.name()),
            Employee.JOB.define()
                    .column()
                    .items(asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .caption(Employee.JOB.name())
                    .searchColumn(true),
            Employee.SALARY.define()
                    .column()
                    .caption(Employee.SALARY.name())
                    .nullable(false).valueRange(1000, 10000).maximumFractionDigits(2),
            Employee.COMMISSION.define()
                    .column()
                    .caption(Employee.COMMISSION.name())
                    .valueRange(100, 2000).maximumFractionDigits(2),
            Employee.MGR.define()
                    .column(),
            Employee.MGR_FK.define()
                    //not really soft, just for testing purposes
                    .softForeignKey()
                    .caption(Employee.MGR_FK.name()),
            Employee.HIREDATE.define()
                    .column()
                    .caption(Employee.HIREDATE.name())
                    .nullable(false),
            Employee.HIRETIME.define()
                    .column()
                    .caption(Employee.HIRETIME.name()),
            Employee.DEPARTMENT_LOCATION.define()
                    .denormalized(Employee.DEPARTMENT_FK, Department.LOC)
                    .caption(Department.LOC.name()),
            Employee.DATA_LAZY.define()
                    .column(),
            Employee.DATA.define()
                    .blobColumn()
                    .eagerlyLoaded(true))
            .stringFactory(Employee.NAME)
            .keyGenerator(KeyGenerator.sequence("scott.emp_seq"))
            .conditionProvider(Employee.NAME_IS_BLAKE_CONDITION, (attributes, values) -> "ename = 'BLAKE'")
            .conditionProvider(Employee.MGR_GREATER_THAN_CONDITION, (attributes, values) -> "mgr > ?")
            .caption("Employee"));
  }

  public interface DepartmentFk extends Entity {
    EntityType TYPE = DOMAIN.entityType("scott.deptfk");

    Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
    Column<String> DNAME = TYPE.stringColumn("dname");
    Column<String> LOC = TYPE.stringColumn("loc");
  }

  void departmentFk() {
    add(DepartmentFk.TYPE.define(
            DepartmentFk.DEPTNO.define()
                    .primaryKey()
                    .caption(Department.DEPTNO.name()),
            DepartmentFk.DNAME.define()
                    .column()
                    .caption(DepartmentFk.DNAME.name()),
            DepartmentFk.LOC.define()
                    .column()
                    .caption(DepartmentFk.LOC.name()))
            .tableName("scott.dept")
            .stringFactory(DepartmentFk.DNAME));
  }

  public interface EmployeeFk {
    EntityType TYPE = DOMAIN.entityType("scott.empfk");

    Column<Integer> ID = TYPE.integerColumn("empno");
    Column<String> NAME = TYPE.stringColumn("ename");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Integer> MGR = TYPE.integerColumn("mgr");
    Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
    Column<OffsetDateTime> HIRETIME = TYPE.offsetDateTimeColumn("hiretime");
    Column<Double> SALARY = TYPE.doubleColumn("sal");
    Column<Double> COMMISSION = TYPE.doubleColumn("comm");
    Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");

    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, DepartmentFk.DEPTNO);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
  }

  void employeeFk() {
    add(EmployeeFk.TYPE.define(
            EmployeeFk.ID.define()
                    .primaryKey()
                    .caption(EmployeeFk.ID.name()),
            EmployeeFk.NAME.define()
                    .column()
                    .caption(EmployeeFk.NAME.name())
                    .nullable(false),
            EmployeeFk.DEPARTMENT.define()
                    .column()
                    .nullable(false),
            EmployeeFk.DEPARTMENT_FK.define()
                    .foreignKey()
                    .caption(EmployeeFk.DEPARTMENT_FK.name())
                    .attributes(DepartmentFk.DNAME),
            EmployeeFk.JOB.define()
                    .column()
                    .caption(EmployeeFk.JOB.name()),
            EmployeeFk.SALARY.define()
                    .column()
                    .caption(EmployeeFk.SALARY.name())
                    .maximumFractionDigits(2),
            EmployeeFk.COMMISSION.define()
                    .column()
                    .caption(EmployeeFk.COMMISSION.name()),
            EmployeeFk.MGR.define()
                    .column(),
            EmployeeFk.MGR_FK.define()
                    .softForeignKey()
                    .caption(EmployeeFk.MGR_FK.name())
                    .attributes(EmployeeFk.NAME, EmployeeFk.JOB, EmployeeFk.DEPARTMENT_FK),
            EmployeeFk.HIREDATE.define()
                    .column()
                    .caption(EmployeeFk.HIREDATE.name())
                    .nullable(false),
            EmployeeFk.HIRETIME.define()
                    .column()
                    .caption(EmployeeFk.HIRETIME.name()))
            .tableName("scott.emp")
            .stringFactory(EmployeeFk.NAME)
            .keyGenerator(KeyGenerator.sequence("scott.emp_seq"))
            .caption("Employee"));
  }

  public interface UUIDTestDefault {
    EntityType TYPE = DOMAIN.entityType("scott.uuid_test_default");

    Column<UUID> ID = TYPE.column("id", UUID.class);
    Column<String> DATA = TYPE.stringColumn("data");
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
    add(UUIDTestDefault.TYPE.define(
            UUIDTestDefault.ID.define()
                    .primaryKey()
                    .caption("Id"),
            UUIDTestDefault.DATA.define()
                    .column()
                    .caption("Data"))
            .keyGenerator(uuidKeyGenerator));
  }

  public interface UUIDTestNoDefault {
    EntityType TYPE = DOMAIN.entityType("scott.uuid_test_no_default");

    Column<UUID> ID = TYPE.column("id", UUID.class);
    Column<String> DATA = TYPE.stringColumn("data");
  }

  private void uuidTestNoDefaultValue() {
    KeyGenerator uuidKeyGenerator = new KeyGenerator() {
      @Override
      public void beforeInsert(Entity entity, DatabaseConnection connection) {
        entity.put(UUIDTestNoDefault.ID, UUID.randomUUID());
      }
    };
    add(UUIDTestNoDefault.TYPE.define(
            UUIDTestNoDefault.ID.define()
                    .primaryKey()
                    .caption("Id"),
            UUIDTestNoDefault.DATA.define()
                    .column()
                    .caption("Data"))
            .keyGenerator(uuidKeyGenerator));
  }

  private void operations() {
    add(PROCEDURE_ID, (connection, arguments) -> {});
    add(FUNCTION_ID, (connection, arguments) -> null);
  }

  public interface Job {
    EntityType TYPE = DOMAIN.entityType("job");

    Column<String> JOB = TYPE.stringColumn("job");
    Column<Double> MAX_SALARY = TYPE.doubleColumn("max_salary");
    Column<Double> MIN_SALARY = TYPE.doubleColumn("min_salary");
    Column<Double> MAX_COMMISSION = TYPE.doubleColumn("max_commission");
    Column<Double> MIN_COMMISSION = TYPE.doubleColumn("min_commission");
  }

  private void job() {
    add(Job.TYPE.define(
            Job.JOB.define()
                    .primaryKey()
                    .groupBy(true),
            Job.MAX_SALARY.define()
                    .column()
                    .columnExpression("max(sal)")
                    .aggregate(true),
            Job.MIN_SALARY.define()
                    .column()
                    .columnExpression("min(sal)")
                    .aggregate(true),
            Job.MAX_COMMISSION.define()
                    .column()
                    .columnExpression("max(comm)")
                    .aggregate(true),
            Job.MIN_COMMISSION.define()
                    .column()
                    .columnExpression("min(comm)")
                    .aggregate(true))
            .tableName("scott.emp")
            .selectQuery(SelectQuery.builder()
                    .having("job <> 'PRESIDENT'")
                    .build()));
  }

  public interface NoPrimaryKey {
    EntityType TYPE = DOMAIN.entityType("scott.no_pk_table");

    Column<Integer> COL_4 = TYPE.integerColumn("col4");
    Column<String> COL_3 = TYPE.stringColumn("col3");
    Column<String> COL_2 = TYPE.stringColumn("col2");
    Column<Integer> COL_1 = TYPE.integerColumn("col1");
  }

  private void noPkEntity() {
    add(NoPrimaryKey.TYPE.define(
            NoPrimaryKey.COL_1.define()
                    .column(),
            NoPrimaryKey.COL_2.define()
                    .column(),
            NoPrimaryKey.COL_3.define()
                    .column(),
            NoPrimaryKey.COL_4.define()
                    .column()));
  }

  public interface EmpnoDeptno {
    EntityType TYPE = DOMAIN.entityType("joinedQueryEntityType");

    Column<Integer> DEPTNO = TYPE.integerColumn("d.deptno");
    Column<Integer> EMPNO = TYPE.integerColumn("e.empno");

    ConditionType CONDITION = EmpnoDeptno.TYPE.conditionType("condition");
  }

  private void empnoDeptno() {
    add(EmpnoDeptno.TYPE.define(
            EmpnoDeptno.DEPTNO.define()
                    .column(),
            EmpnoDeptno.EMPNO.define()
                    .primaryKey())
            .selectQuery(SelectQuery.builder()
                    .from("scott.emp e, scott.dept d")
                    .where("e.deptno = d.deptno")
                    .orderBy("e.deptno, e.ename")
                    .build())
            .conditionProvider(EmpnoDeptno.CONDITION, (attributes, values) -> "d.deptno = 10"));
  }

  public interface Query {
    EntityType TYPE = DOMAIN.entityType("query");

    Column<Integer> EMPNO = TYPE.integerColumn("empno");
    Column<String> ENAME = TYPE.stringColumn("ename");
  }

  private void query() {
    add(Query.TYPE.define(
            Query.EMPNO.define()
                    .column(),
            Query.ENAME.define()
                    .column())
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

    Column<Integer> EMPNO = TYPE.integerColumn("empno");
    Column<String> ENAME = TYPE.stringColumn("ename");
  }

  private void queryColumnsWhereClause() {
    add(QueryColumnsWhereClause.TYPE.define(
            QueryColumnsWhereClause.EMPNO.define()
                    .column(),
            QueryColumnsWhereClause.ENAME.define()
                    .column())
            .tableName("scott.emp e")
            .orderBy(OrderBy.descending(QueryColumnsWhereClause.ENAME))
            .selectQuery(SelectQuery.builder()
                    .columns("e.empno, e.ename")
                    .where("e.deptno > 10")
                    .build()));
  }

  public interface QueryFromClause {
    EntityType TYPE = DOMAIN.entityType("query_from");

    Column<Integer> EMPNO = TYPE.integerColumn("empno");
    Column<String> ENAME = TYPE.stringColumn("ename");
  }

  private void queryFromClause() {
    add(QueryFromClause.TYPE.define(
            QueryFromClause.EMPNO.define()
                    .column(),
            QueryFromClause.ENAME.define()
                    .column())
            .orderBy(OrderBy.descending(QueryFromClause.ENAME))
            .selectQuery(SelectQuery.builder()
                    .from("scott.emp")
                    .orderBy("ename")
                    .build()));
  }

  public interface QueryFromWhereClause {
    EntityType TYPE = DOMAIN.entityType("query_from_where");

    Column<Integer> EMPNO = TYPE.integerColumn("empno");
    Column<String> ENAME = TYPE.stringColumn("ename");
  }

  private void queryFromWhereClause() {
    add(QueryFromWhereClause.TYPE.define(
            QueryFromWhereClause.EMPNO.define()
                    .column(),
            QueryFromWhereClause.ENAME.define()
                    .column())
            .orderBy(OrderBy.descending(QueryFromWhereClause.ENAME))
            .selectQuery(SelectQuery.builder()
                    .from("scott.emp")
                    .where("deptno > 10")
                    .orderBy("deptno")
                    .build()));
  }

  public interface Master {
    EntityType TYPE = DOMAIN.entityType("scott.master");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<String> DATA = TYPE.stringColumn("data");
  }

  void master() {
    add(Master.TYPE.define(
            Master.ID.define()
                    .primaryKey(),
            Master.DATA.define()
                    .column())
            .keyGenerator(identity()));
  }

  public interface Detail {
    EntityType TYPE = DOMAIN.entityType("scott.detail");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<Integer> MASTER_1_ID = TYPE.integerColumn("master_1_id");
    Column<Integer> MASTER_2_ID = TYPE.integerColumn("master_2_id");

    ForeignKey MASTER_1_FK = TYPE.foreignKey("master_1_fk", MASTER_1_ID, Master.ID);
    ForeignKey MASTER_2_FK = TYPE.foreignKey("master_2_fk", MASTER_2_ID, Master.ID);
  }

  void detail() {
    add(Detail.TYPE.define(
            Detail.ID.define()
                    .primaryKey(),
            Detail.MASTER_1_ID.define()
                    .column(),
            Detail.MASTER_1_FK.define()
                    .foreignKey(),
            Detail.MASTER_2_ID.define()
                    .column(),
            Detail.MASTER_2_FK.define()
                    .foreignKey())
            .keyGenerator(identity()));
  }

  public interface MasterFk {
    EntityType TYPE = DOMAIN.entityType("scott.master_fk");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<String> NAME = TYPE.stringColumn("name");
  }

  void masterFk() {
    add(MasterFk.TYPE.define(
            MasterFk.ID.define()
                    .primaryKey(),
            MasterFk.NAME.define()
                    .column()));
  }

  public interface DetailFk {
    EntityType TYPE = DOMAIN.entityType("scott.detail_fk");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<String> MASTER_NAME = TYPE.stringColumn("master_name");

    ForeignKey MASTER_FK = TYPE.foreignKey("master_fk", MASTER_NAME, MasterFk.NAME);
  }

  void detailFk() {
    add(DetailFk.TYPE.define(
            DetailFk.ID.define()
                    .primaryKey(),
            DetailFk.MASTER_NAME.define()
                    .column(),
            DetailFk.MASTER_FK.define()
                    .foreignKey()));
  }

  public interface EmployeeNonOpt {
    EntityType TYPE = DOMAIN.entityType("empnonopt");

    Column<Integer> ID = TYPE.integerColumn("empno");
    Column<String> NAME = TYPE.stringColumn("ename");
  }

  void employeeNonOpt() {
    add(EmployeeNonOpt.TYPE.define(
            EmployeeNonOpt.ID.define()
                    .primaryKey(),
            EmployeeNonOpt.NAME.define()
                    .column())
            .tableName("scott.emp")
            .optimisticLocking(false));
  }
}
