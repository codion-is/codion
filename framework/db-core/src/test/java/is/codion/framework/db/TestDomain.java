/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.time.LocalDateTime;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    super(DOMAIN);
    department();
    employee();
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("db.scott.dept");

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
                    .searchColumn(true)
                    .maximumLength(14)
                    .nullable(false),
            Department.LOCATION.define()
                    .column()
                    .caption(Department.LOCATION.name())
                    .maximumLength(13))
            .tableName("scott.dept")
            .smallDataset(true)
            .orderBy(ascending(Department.NAME))
            .stringFactory(Department.NAME)
            .caption("Department"));
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("db.scott.emp");

    Column<Integer> ID = TYPE.integerColumn("emp_id");
    Column<String> NAME = TYPE.stringColumn("emp_name");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Integer> MGR = TYPE.integerColumn("mgr");
    Column<LocalDateTime> HIREDATE = TYPE.localDateTimeColumn("hiredate");
    Column<Double> SALARY = TYPE.doubleColumn("sal");
    Column<Double> COMMISSION = TYPE.doubleColumn("comm");
    Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
    Column<String> DEPARTMENT_LOCATION = TYPE.stringColumn("location");

    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
  }

  void employee() {
    add(Employee.TYPE.define(
            Employee.ID.define()
                    .primaryKey()
                    .caption(Employee.ID.name()).columnName("empno"),
            Employee.NAME.define()
                    .column()
                    .caption(Employee.NAME.name())
                    .searchColumn(true)
                    .columnName("ename")
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
                    .searchColumn(true),
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
                    .denormalizedAttribute(Employee.DEPARTMENT_FK, Department.LOCATION)
                    .caption(Department.LOCATION.name()))
            .tableName("scott.emp")
            .orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
            .stringFactory(Employee.NAME)
            .caption("Employee"));
  }
}
