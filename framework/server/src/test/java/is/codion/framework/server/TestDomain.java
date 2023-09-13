/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.AbstractReport;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.sql.Connection;
import java.time.LocalDate;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  public static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    super(DOMAIN);
    department();
    employee();
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("scott.dept");

    Column<Integer> ID = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");

    ProcedureType<EntityConnection, Object> PROC = ProcedureType.procedureType("dept_proc");
  }

  void department() {
    add(Department.TYPE.define(
            Department.ID.primaryKey()
                    .caption(Department.ID.name())
                    .updatable(true).nullable(false),
            Department.NAME.column()
                    .caption(Department.NAME.name())
                    .searchColumn(true)
                    .maximumLength(14)
                    .nullable(false),
            Department.LOCATION.column()
                    .caption(Department.LOCATION.name())
                    .maximumLength(13))
            .smallDataset(true)
            .stringFactory(Department.NAME)
            .caption("Department"));

    add(Department.PROC, (connection, argument) -> {});
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

    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);

    ConditionType MGR_CONDITION_TYPE = TYPE.conditionType("mgrConditionType");
    ReportType<Object, Object, Object> EMP_REPORT = ReportType.reportType("emp_report");
    FunctionType<EntityConnection, Object, Object> FUNC = FunctionType.functionType("emp_func");
  }

  void employee() {
    add(Employee.TYPE.define(
            Employee.ID.primaryKey()
                    .caption(Employee.ID.name()),
            Employee.NAME.column()
                    .caption(Employee.NAME.name())
                    .searchColumn(true)
                    .maximumLength(10)
                    .nullable(false),
            Employee.DEPARTMENT.column()
                    .nullable(false),
            Employee.DEPARTMENT_FK.foreignKey()
                    .caption(Employee.DEPARTMENT_FK.name()),
            Employee.JOB.column()
                    .items(asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .caption(Employee.JOB.name())
                    .searchColumn(true),
            Employee.SALARY.column()
                    .caption(Employee.SALARY.name())
                    .nullable(false)
                    .valueRange(1000, 10000)
                    .maximumFractionDigits(2),
            Employee.COMMISSION.column()
                    .caption(Employee.COMMISSION.name())
                    .valueRange(100, 2000)
                    .maximumFractionDigits(2),
            Employee.MGR.column(),
            Employee.MGR_FK.foreignKey()
                    .caption(Employee.MGR_FK.name()),
            Employee.HIREDATE.column()
                    .caption(Employee.HIREDATE.name())
                    .nullable(false),
            Employee.DEPARTMENT_LOCATION.denormalizedAttribute(Employee.DEPARTMENT_FK, Department.LOCATION)
                    .caption(Department.LOCATION.name()))
            .stringFactory(Employee.NAME)
            .keyGenerator(KeyGenerator.sequence("scott.emp_seq"))
            .conditionProvider(Employee.MGR_CONDITION_TYPE, (attributes, values) -> "mgr > ?")
            .caption("Employee"));

    add(Employee.EMP_REPORT, new AbstractReport<Object, Object, Object>("path", true) {
      @Override
      public Object fillReport(Connection connection, Object parameters) {
        return null;
      }
      @Override
      public Object loadReport() {
        return null;
      }
    });

    add(Employee.FUNC, (connection, argument) -> null);
  }
}
