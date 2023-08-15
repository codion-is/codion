/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.AbstractReport;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public static final ReportType<Object, String, String> REPORT = ReportType.reportType("report");

  public TestDomain() {
    super(DOMAIN);
    department();
    employee();
    operations();
    add(REPORT, new AbstractReport<Object, String, String>("report.path", false) {
      @Override
      public String fillReport(Connection connection, String parameters) {
        return "result";
      }

      @Override
      public Object loadReport() {
        return null;
      }
    });
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("scott.dept");

    Column<Integer> ID = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");
  }

  void department() {
    add(Department.TYPE.define(
            Department.ID.primaryKey(Department.ID.name())
                    .updatable(true)
                    .nullable(false),
            Department.NAME.column(Department.NAME.name())
                    .searchColumn(true)
                    .maximumLength(14)
                    .nullable(false),
            Department.LOCATION.column(Department.LOCATION.name())
                    .maximumLength(13))
            .smallDataset(true)
            .orderBy(ascending(Department.NAME))
            .stringFactory(Department.NAME)
            .caption("Department"));
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("scott.emp");

    Column<Integer> ID = TYPE.integerColumn("empno");
    Column<String> NAME = TYPE.stringColumn("ename");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Integer> MGR = TYPE.integerColumn("mgr");
    Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
    Column<Double> SALARY = TYPE.doubleColumn("sal");
    Column<Double> COMMISSION = TYPE.doubleColumn("comm");
    Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
    Column<String> DEPARTMENT_LOCATION = TYPE.stringColumn("location");
    Column<byte[]> DATA = TYPE.byteArrayColumn("data");

    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
  }

  void employee() {
    add(Employee.TYPE.define(
            Employee.ID.primaryKey(Employee.ID.name()),
            Employee.NAME.column(Employee.NAME.name())
                    .searchColumn(true)
                    .maximumLength(10)
                    .nullable(false),
            Employee.DEPARTMENT.column()
                    .nullable(false),
            Employee.DEPARTMENT_FK.foreignKey(Employee.DEPARTMENT_FK.name()),
            Employee.JOB.item(Employee.JOB.name(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchColumn(true),
            Employee.SALARY.column(Employee.SALARY.name())
                    .nullable(false)
                    .valueRange(1000, 10000)
                    .maximumFractionDigits(2),
            Employee.COMMISSION.column(Employee.COMMISSION.name())
                    .valueRange(100, 2000)
                    .maximumFractionDigits(2),
            Employee.MGR.column(),
            Employee.MGR_FK.foreignKey(Employee.MGR_FK.name()),
            Employee.HIREDATE.column(Employee.HIREDATE.name())
                    .nullable(false),
            Employee.DEPARTMENT_LOCATION.denormalized(Department.LOCATION.name(), Employee.DEPARTMENT_FK, Department.LOCATION),
            Employee.DATA.column("Data"))
            .stringFactory(Employee.NAME)
            .keyGenerator(increment("scott.emp", "empno"))
            .orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
            .caption("Employee"));
  }

  public static final FunctionType<EntityConnection, Object, List<Object>> FUNCTION_ID = FunctionType.functionType("functionId");
  public static final ProcedureType<EntityConnection, Object> PROCEDURE_ID = ProcedureType.procedureType("procedureId");

  void operations() {
    add(PROCEDURE_ID, (connection, objects) -> {});
    add(FUNCTION_ID, (connection, objects) -> emptyList());
  }
}
