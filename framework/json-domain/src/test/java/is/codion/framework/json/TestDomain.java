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
package is.codion.framework.json;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ConditionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  public static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    super(DOMAIN);
    testEntity();
    department();
    employee();
  }

  public interface TestEntity {
    EntityType TYPE = DOMAIN.entityType("test.entity");
    Column<BigDecimal> DECIMAL = TYPE.bigDecimalColumn("id");
    Column<LocalDateTime> DATE_TIME = TYPE.localDateTimeColumn("date_time");
    Column<OffsetDateTime> OFFSET_DATE_TIME = TYPE.offsetDateTimeColumn("offset_date_time");
    Column<byte[]> BLOB = TYPE.byteArrayColumn("blob");
    Column<String> READ_ONLY = TYPE.stringColumn("read_only");
    Column<Boolean> BOOLEAN = TYPE.booleanColumn("boolean");
    Column<LocalTime> TIME = TYPE.localTimeColumn("time");
    Attribute<Entity> ENTITY = TYPE.entityAttribute("entity");
    ConditionType CONDITION_TYPE = TYPE.conditionType("entityConditionType");
  }

  void testEntity() {
    add(TestEntity.TYPE.define(
            TestEntity.DECIMAL.define()
                    .primaryKey(0),
            TestEntity.DATE_TIME.define()
                    .primaryKey(1),
            TestEntity.OFFSET_DATE_TIME.define()
                    .column(),
            TestEntity.BLOB.define()
                    .column(),
            TestEntity.READ_ONLY.define()
                    .column()
                    .readOnly(true),
            TestEntity.BOOLEAN.define()
                    .column(),
            TestEntity.TIME.define()
                    .column(),
            TestEntity.ENTITY.define()
                    .attribute())
            .condition(TestEntity.CONDITION_TYPE, (attributes, values) -> "1 = 2"));
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("employees.department");
    Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");
    Column<byte[]> LOGO = TYPE.byteArrayColumn("logo");
  }

  void department() {
    add(Department.TYPE.define(
            Department.DEPTNO.define()
                    .primaryKey()
                    .updatable(true).nullable(false),
            Department.NAME.define()
                    .column()
                    .searchable(true)
                    .maximumLength(14)
                    .nullable(false),
            Department.LOCATION.define()
                    .column()
                    .maximumLength(13),
            Department.LOGO.define()
                    .column())
            .smallDataset(true)
            .caption("Department"));
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("employees.employee");
    Column<Integer> EMPNO = TYPE.integerColumn("empno");
    Column<String> NAME = TYPE.stringColumn("ename");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Integer> MGR = TYPE.integerColumn("mgr");
    Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
    Column<BigDecimal> SALARY = TYPE.bigDecimalColumn("sal");
    Column<Double> COMMISSION = TYPE.doubleColumn("comm");
    Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.DEPTNO);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, EMPNO);
    Column<String> EMP_DEPARTMENT_LOCATION = TYPE.stringColumn("location");
  }

  void employee() {
    add(Employee.TYPE.define(
            Employee.EMPNO.define()
                    .primaryKey(),
            Employee.NAME.define()
                    .column()
                    .searchable(true).maximumLength(10).nullable(false),
            Employee.DEPARTMENT.define()
                    .column()
                    .nullable(false),
            Employee.DEPARTMENT_FK.define()
                    .foreignKey(),
            Employee.JOB.define()
                    .column()
                    .items(asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchable(true),
            Employee.SALARY.define()
                    .column()
                    .nullable(false).valueRange(1000, 10000).maximumFractionDigits(2),
            Employee.COMMISSION.define()
                    .column()
                    .valueRange(100, 2000).maximumFractionDigits(2),
            Employee.MGR.define()
                    .column(),
            Employee.MGR_FK.define()
                    .foreignKey(),
            Employee.HIREDATE.define()
                    .column()
                    .nullable(false),
            Employee.EMP_DEPARTMENT_LOCATION.define()
                    .denormalized(Employee.DEPARTMENT_FK, Department.LOCATION))
            .stringFactory(Employee.NAME)
            .keyGenerator(KeyGenerator.sequence("employees.employee_seq"))
            .caption("Employee"));
  }
}
