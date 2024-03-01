/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.AbstractReport;
import is.codion.common.db.report.ReportType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ConditionType;

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
    EntityType TYPE = DOMAIN.entityType("employees.department");

    Column<Integer> ID = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");

    ProcedureType<EntityConnection, Object> PROC = ProcedureType.procedureType("dept_proc");
  }

  void department() {
    add(Department.TYPE.define(
                    Department.ID.define()
                            .primaryKey()
                            .caption(Department.ID.name())
                            .updatable(true).nullable(false),
                    Department.NAME.define()
                            .column()
                            .caption(Department.NAME.name())
                            .searchable(true)
                            .maximumLength(14)
                            .nullable(false),
                    Department.LOCATION.define()
                            .column()
                            .caption(Department.LOCATION.name())
                            .maximumLength(13))
            .smallDataset(true)
            .stringFactory(Department.NAME)
            .caption("Department"));

    add(Department.PROC, (connection, argument) -> {});
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("employees.employee");

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
                    Employee.ID.define()
                            .primaryKey()
                            .caption(Employee.ID.name()),
                    Employee.NAME.define()
                            .column()
                            .caption(Employee.NAME.name())
                            .searchable(true)
                            .maximumLength(10)
                            .nullable(false),
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
                            .searchable(true),
                    Employee.SALARY.define()
                            .column()
                            .caption(Employee.SALARY.name())
                            .nullable(false)
                            .valueRange(1000, 10000)
                            .maximumFractionDigits(2),
                    Employee.COMMISSION.define()
                            .column()
                            .caption(Employee.COMMISSION.name())
                            .valueRange(100, 2000)
                            .maximumFractionDigits(2),
                    Employee.MGR.define()
                            .column(),
                    Employee.MGR_FK.define()
                            .foreignKey()
                            .caption(Employee.MGR_FK.name()),
                    Employee.HIREDATE.define()
                            .column()
                            .caption(Employee.HIREDATE.name())
                            .nullable(false),
                    Employee.DEPARTMENT_LOCATION.define()
                            .denormalized(Employee.DEPARTMENT_FK, Department.LOCATION)
                            .caption(Department.LOCATION.name()))
            .stringFactory(Employee.NAME)
            .keyGenerator(KeyGenerator.sequence("employees.employee_seq"))
            .condition(Employee.MGR_CONDITION_TYPE, (attributes, values) -> "mgr > ?")
            .caption("Employee"));

    add(Employee.EMP_REPORT, new AbstractReport<Object, Object, Object>("path", true) {
      @Override
      public Object fill(Connection connection, Object parameters) {
        return null;
      }
      @Override
      public Object load() {
        return null;
      }
    });

    add(Employee.FUNC, (connection, argument) -> null);
  }
}
