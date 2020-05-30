/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.BlobAttribute;
import is.codion.framework.domain.property.EntityAttribute;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

public final class TestDomain extends Domain {

  public TestDomain() {
    testEntity();
    department();
    employee();
  }

  public static final String T_ENTITY = "test.entity";
  public static final Attribute<BigDecimal> ENTITY_DECIMAL = attribute("id", Types.DECIMAL);
  public static final Attribute<LocalDateTime> ENTITY_DATE_TIME = attribute("date_time", Types.TIMESTAMP);
  public static final BlobAttribute ENTITY_BLOB = blobAttribute("blob");
  public static final Attribute<String> ENTITY_READ_ONLY = attribute("read_only", Types.VARCHAR);
  public static final Attribute<Boolean> ENTITY_BOOLEAN = attribute("boolean", Types.BOOLEAN);
  public static final Attribute<LocalTime> ENTITY_TIME = attribute("time", Types.TIME);
  public static final String ENTITY_CONDITION_ID = "entityConditionId";

  void testEntity() {
    define(T_ENTITY,
            columnProperty(ENTITY_DECIMAL).primaryKeyIndex(0),
            columnProperty(ENTITY_DATE_TIME).primaryKeyIndex(1),
            columnProperty(ENTITY_BLOB),
            columnProperty(ENTITY_READ_ONLY)
                    .readOnly(true),
            columnProperty(ENTITY_BOOLEAN),
            columnProperty(ENTITY_TIME))
            .conditionProvider(ENTITY_CONDITION_ID, (attributes, values) -> "1 = 2");
  }

  public static final Attribute<Integer> DEPARTMENT_ID = attribute("deptno", Types.INTEGER);
  public static final Attribute<String> DEPARTMENT_NAME = attribute("dname", Types.VARCHAR);
  public static final Attribute<String> DEPARTMENT_LOCATION = attribute("loc", Types.VARCHAR);
  public static final BlobAttribute DEPARTMENT_LOGO = blobAttribute("logo");

  public static final String T_DEPARTMENT = "scott.dept";

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
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .caption("Department");
  }

  public static final Attribute<Integer> EMP_ID = attribute("empno", Types.INTEGER);
  public static final Attribute<String> EMP_NAME = attribute("ename", Types.VARCHAR);
  public static final Attribute<String> EMP_JOB = attribute("job", Types.VARCHAR);
  public static final Attribute<Integer> EMP_MGR = attribute("mgr", Types.INTEGER);
  public static final Attribute<LocalDate> EMP_HIREDATE = attribute("hiredate", Types.DATE);
  public static final Attribute<BigDecimal> EMP_SALARY = attribute("sal", Types.DECIMAL);
  public static final Attribute<Double> EMP_COMMISSION = attribute("comm", Types.DOUBLE);
  public static final Attribute<Integer> EMP_DEPARTMENT = attribute("deptno", Types.INTEGER);
  public static final EntityAttribute EMP_DEPARTMENT_FK = entityAttribute("dept_fk");
  public static final EntityAttribute EMP_MGR_FK = entityAttribute("mgr_fk");
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = attribute("location", Types.VARCHAR);
  public static final String T_EMP = "scott.emp";

  void employee() {
    define(T_EMP,
            primaryKeyProperty(EMP_ID, EMP_ID.getName()),
            columnProperty(EMP_NAME, EMP_NAME.getName())
                    .searchProperty(true).maximumLength(10).nullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK.getName(), T_DEPARTMENT,
                    columnProperty(EMP_DEPARTMENT))
                    .nullable(false),
            valueListProperty(EMP_JOB, EMP_JOB.getName(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(EMP_SALARY, EMP_SALARY.getName())
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, EMP_COMMISSION.getName())
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK.getName(), T_EMP,
                    columnProperty(EMP_MGR)),
            columnProperty(EMP_HIREDATE, EMP_HIREDATE.getName())
                    .nullable(false),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK, DEPARTMENT_LOCATION,
                    DEPARTMENT_LOCATION.getName()).preferredColumnWidth(100))
            .stringProvider(new StringProvider(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .caption("Employee");
  }
}
