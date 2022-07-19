/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
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
import static is.codion.framework.domain.property.Properties.*;
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
    Attribute<BigDecimal> DECIMAL = TYPE.bigDecimalAttribute("id");
    Attribute<LocalDateTime> DATE_TIME = TYPE.localDateTimeAttribute("date_time");
    Attribute<OffsetDateTime> OFFSET_DATE_TIME = TYPE.offsetDateTimeAttribute("offset_date_time");
    Attribute<byte[]> BLOB = TYPE.byteArrayAttribute("blob");
    Attribute<String> READ_ONLY = TYPE.stringAttribute("read_only");
    Attribute<Boolean> BOOLEAN = TYPE.booleanAttribute("boolean");
    Attribute<LocalTime> TIME = TYPE.localTimeAttribute("time");
    Attribute<Entity> ENTITY = TYPE.entityAttribute("entity");
    ConditionType CONDITION_TYPE = TYPE.conditionType("entityConditionId");
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
            columnProperty(TestEntity.ENTITY))
            .conditionProvider(TestEntity.CONDITION_TYPE, (attributes, values) -> "1 = 2"));
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("scott.dept");
    Attribute<Integer> DEPTNO = TYPE.integerAttribute("deptno");
    Attribute<String> NAME = TYPE.stringAttribute("dname");
    Attribute<String> LOCATION = TYPE.stringAttribute("loc");
    Attribute<byte[]> LOGO = TYPE.byteArrayAttribute("logo");
  }

  void department() {
    add(definition(
            primaryKeyProperty(Department.DEPTNO)
                    .updatable(true).nullable(false),
            columnProperty(Department.NAME)
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(Department.LOCATION)
                    .preferredColumnWidth(150).maximumLength(13),
            columnProperty(Department.LOGO))
            .smallDataset(true)
            .caption("Department"));
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("scott.emp");
    Attribute<Integer> EMPNO = TYPE.integerAttribute("empno");
    Attribute<String> NAME = TYPE.stringAttribute("ename");
    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Integer> MGR = TYPE.integerAttribute("mgr");
    Attribute<LocalDate> HIREDATE = TYPE.localDateAttribute("hiredate");
    Attribute<BigDecimal> SALARY = TYPE.bigDecimalAttribute("sal");
    Attribute<Double> COMMISSION = TYPE.doubleAttribute("comm");
    Attribute<Integer> DEPARTMENT = TYPE.integerAttribute("deptno");
    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.DEPTNO);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, EMPNO);
    Attribute<String> EMP_DEPARTMENT_LOCATION = TYPE.stringAttribute("location");
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
            denormalizedViewProperty(Employee.EMP_DEPARTMENT_LOCATION, Employee.DEPARTMENT_FK, Department.LOCATION).preferredColumnWidth(100))
            .stringFactory(Employee.NAME)
            .keyGenerator(increment("scott.emp", "empno"))
            .caption("Employee"));
  }
}
