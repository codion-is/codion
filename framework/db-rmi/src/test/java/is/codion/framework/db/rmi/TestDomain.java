/*
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.rmi;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.time.LocalDate;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

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
    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
    Column<String> DEPARTMENT_LOCATION = TYPE.stringColumn("location");
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
            .caption("Employee"));
  }
}
