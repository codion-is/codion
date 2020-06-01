/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Identity;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.Attributes;
import is.codion.framework.domain.property.EntityAttribute;

import java.time.LocalDate;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.property.Properties.*;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static is.codion.plugin.jasperreports.model.JasperReports.fileReport;
import static java.util.Arrays.asList;

public final class TestDomain extends Domain {

  public TestDomain() {
    department();
    employee();
  }

  public static final Identity T_DEPARTMENT = Identity.identity("scott.dept");
  public static final Attribute<Integer> DEPARTMENT_ID = Attributes.integerAttribute("deptno", T_DEPARTMENT);
  public static final Attribute<String> DEPARTMENT_NAME = Attributes.stringAttribute("dname", T_DEPARTMENT);
  public static final Attribute<String> DEPARTMENT_LOCATION = Attributes.stringAttribute("loc", T_DEPARTMENT);

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.getName())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .caption("Department");
  }


  public static final Identity T_EMP = Identity.identity("scott.emp");
  public static final Attribute<Integer> EMP_ID = Attributes.integerAttribute("empno", T_EMP);
  public static final Attribute<String> EMP_NAME = Attributes.stringAttribute("ename", T_EMP);
  public static final Attribute<String> EMP_JOB = Attributes.stringAttribute("job", T_EMP);
  public static final Attribute<Integer> EMP_MGR = Attributes.integerAttribute("mgr", T_EMP);
  public static final Attribute<LocalDate> EMP_HIREDATE = Attributes.localDateAttribute("hiredate", T_EMP);
  public static final Attribute<Double> EMP_SALARY = Attributes.doubleAttribute("sal", T_EMP);
  public static final Attribute<Double> EMP_COMMISSION = Attributes.doubleAttribute("comm", T_EMP);
  public static final Attribute<Integer> EMP_DEPARTMENT = Attributes.integerAttribute("deptno", T_EMP);
  public static final EntityAttribute EMP_DEPARTMENT_FK = Attributes.entityAttribute("dept_fk", T_EMP);
  public static final EntityAttribute EMP_MGR_FK = Attributes.entityAttribute("mgr_fk", T_EMP);
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = Attributes.stringAttribute("location", T_EMP);

  public static final JasperReportWrapper EMPLOYEE_FILE_REPORT =
          fileReport("/empdept_employees.jasper");
  public static final JasperReportWrapper EMPLOYEE_CLASSPATH_REPORT =
          classPathReport(TestDomain.class, "/empdept_employees.jasper");

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
