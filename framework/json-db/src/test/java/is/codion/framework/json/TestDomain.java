/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.property.Property.*;
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
    add(definition(
            columnProperty(TestEntity.DECIMAL).primaryKeyIndex(0),
            columnProperty(TestEntity.DATE_TIME).primaryKeyIndex(1),
            columnProperty(TestEntity.OFFSET_DATE_TIME),
            columnProperty(TestEntity.BLOB),
            columnProperty(TestEntity.READ_ONLY)
                    .readOnly(true),
            columnProperty(TestEntity.BOOLEAN),
            columnProperty(TestEntity.TIME),
            transientProperty(TestEntity.ENTITY))
            .conditionProvider(TestEntity.CONDITION_TYPE, (attributes, values) -> "1 = 2"));
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("scott.dept");
    Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");
    Column<byte[]> LOGO = TYPE.byteArrayColumn("logo");
  }

  void department() {
    add(definition(
            primaryKeyProperty(Department.DEPTNO)
                    .updatable(true).nullable(false),
            columnProperty(Department.NAME)
                    .searchProperty(true)
                    .maximumLength(14)
                    .nullable(false),
            columnProperty(Department.LOCATION)
                    .maximumLength(13),
            columnProperty(Department.LOGO))
            .smallDataset(true)
            .caption("Department"));
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("scott.emp");
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
    add(definition(
            primaryKeyProperty(Employee.EMPNO),
            columnProperty(Employee.NAME)
                    .searchProperty(true).maximumLength(10).nullable(false),
            columnProperty(Employee.DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(Employee.DEPARTMENT_FK),
            itemProperty(Employee.JOB,
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(Employee.SALARY)
                    .nullable(false).valueRange(1000, 10000).maximumFractionDigits(2),
            columnProperty(Employee.COMMISSION)
                    .valueRange(100, 2000).maximumFractionDigits(2),
            columnProperty(Employee.MGR),
            foreignKeyProperty(Employee.MGR_FK),
            columnProperty(Employee.HIREDATE)
                    .nullable(false),
            denormalizedProperty(Employee.EMP_DEPARTMENT_LOCATION, Employee.DEPARTMENT_FK, Department.LOCATION))
            .stringFactory(Employee.NAME)
            .keyGenerator(increment("scott.emp", "empno"))
            .caption("Employee"));
  }
}
