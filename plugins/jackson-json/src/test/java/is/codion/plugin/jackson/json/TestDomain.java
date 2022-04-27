/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
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

  public static final EntityType T_ENTITY = DOMAIN.entityType("test.entity");
  public static final Attribute<BigDecimal> ENTITY_DECIMAL = T_ENTITY.bigDecimalAttribute("id");
  public static final Attribute<LocalDateTime> ENTITY_DATE_TIME = T_ENTITY.localDateTimeAttribute("date_time");
  public static final Attribute<OffsetDateTime> ENTITY_OFFSET_DATE_TIME = T_ENTITY.offsetDateTimeAttribute("offset_date_time");
  public static final Attribute<byte[]> ENTITY_BLOB = T_ENTITY.byteArrayAttribute("blob");
  public static final Attribute<String> ENTITY_READ_ONLY = T_ENTITY.stringAttribute("read_only");
  public static final Attribute<Boolean> ENTITY_BOOLEAN = T_ENTITY.booleanAttribute("boolean");
  public static final Attribute<LocalTime> ENTITY_TIME = T_ENTITY.localTimeAttribute("time");
  public static final ConditionType ENTITY_CONDITION_TYPE = T_ENTITY.conditionType("entityConditionId");

  void testEntity() {
    define(T_ENTITY,
            columnProperty(ENTITY_DECIMAL).primaryKeyIndex(0),
            columnProperty(ENTITY_DATE_TIME).primaryKeyIndex(1),
            columnProperty(ENTITY_OFFSET_DATE_TIME),
            columnProperty(ENTITY_BLOB),
            columnProperty(ENTITY_READ_ONLY)
                    .readOnly(true),
            columnProperty(ENTITY_BOOLEAN),
            columnProperty(ENTITY_TIME))
            .conditionProvider(ENTITY_CONDITION_TYPE, (attributes, values) -> "1 = 2");
  }

  public static final EntityType T_DEPARTMENT = DOMAIN.entityType("scott.dept");
  public static final Attribute<Integer> DEPARTMENT_ID = T_DEPARTMENT.integerAttribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = T_DEPARTMENT.stringAttribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = T_DEPARTMENT.stringAttribute("loc");
  public static final Attribute<byte[]> DEPARTMENT_LOGO = T_DEPARTMENT.byteArrayAttribute("logo");

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.getName())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13),
            columnProperty(DEPARTMENT_LOGO))
            .smallDataset(true)
            .caption("Department");
  }

  public static final EntityType T_EMP = DOMAIN.entityType("scott.emp");
  public static final Attribute<Integer> EMP_ID = T_EMP.integerAttribute("empno");
  public static final Attribute<String> EMP_NAME = T_EMP.stringAttribute("ename");
  public static final Attribute<String> EMP_JOB = T_EMP.stringAttribute("job");
  public static final Attribute<Integer> EMP_MGR = T_EMP.integerAttribute("mgr");
  public static final Attribute<LocalDate> EMP_HIREDATE = T_EMP.localDateAttribute("hiredate");
  public static final Attribute<BigDecimal> EMP_SALARY = T_EMP.bigDecimalAttribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = T_EMP.doubleAttribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = T_EMP.integerAttribute("deptno");
  public static final ForeignKey EMP_DEPARTMENT_FK = T_EMP.foreignKey("dept_fk", EMP_DEPARTMENT, DEPARTMENT_ID);
  public static final ForeignKey EMP_MGR_FK = T_EMP.foreignKey("mgr_fk", EMP_MGR, EMP_ID);
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = T_EMP.stringAttribute("location");

  void employee() {
    define(T_EMP,
            primaryKeyProperty(EMP_ID, EMP_ID.getName()),
            columnProperty(EMP_NAME, EMP_NAME.getName())
                    .searchProperty(true).maximumLength(10).nullable(false),
            columnProperty(EMP_DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK.getName()),
            itemProperty(EMP_JOB, EMP_JOB.getName(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(EMP_SALARY, EMP_SALARY.getName())
                    .nullable(false).valueRange(1000, 10000).maximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, EMP_COMMISSION.getName())
                    .valueRange(100, 2000).maximumFractionDigits(2),
            columnProperty(EMP_MGR),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK.getName()),
            columnProperty(EMP_HIREDATE, EMP_HIREDATE.getName())
                    .nullable(false),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName(), EMP_DEPARTMENT_FK, DEPARTMENT_LOCATION).preferredColumnWidth(100))
            .stringFactory(stringFactory(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .caption("Employee");
  }
}
