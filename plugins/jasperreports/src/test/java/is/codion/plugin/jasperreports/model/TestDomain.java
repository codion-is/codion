/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;

import java.sql.Types;
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

  public static final Attribute<Integer> DEPARTMENT_ID = attribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = attribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = attribute("loc");

  public static final String T_DEPARTMENT = "scott.dept";

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME.getName())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .caption("Department");
  }

  public static final Attribute<Integer> EMP_ID = attribute("empno");
  public static final Attribute<String> EMP_NAME = attribute("ename");
  public static final Attribute<String> EMP_JOB = attribute("job");
  public static final Attribute<Integer> EMP_MGR = attribute("mgr");
  public static final Attribute<LocalDate> EMP_HIREDATE = attribute("hiredate");
  public static final Attribute<Double> EMP_SALARY = attribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = attribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = attribute("deptno");
  public static final Attribute<Entity> EMP_DEPARTMENT_FK = attribute("dept_fk");
  public static final Attribute<Entity> EMP_MGR_FK = attribute("mgr_fk");
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = attribute("location");
  public static final String T_EMP = "scott.emp";

  public static final JasperReportWrapper EMPLOYEE_FILE_REPORT =
          fileReport("/empdept_employees.jasper");
  public static final JasperReportWrapper EMPLOYEE_CLASSPATH_REPORT =
            classPathReport(TestDomain.class, "/empdept_employees.jasper");

  void employee() {
    define(T_EMP,
            primaryKeyProperty(EMP_ID, Types.INTEGER, EMP_ID.getName()),
            columnProperty(EMP_NAME, Types.VARCHAR, EMP_NAME.getName())
                    .searchProperty(true).maximumLength(10).nullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK.getName(), T_DEPARTMENT,
                    columnProperty(EMP_DEPARTMENT, Types.INTEGER))
                    .nullable(false),
            valueListProperty(EMP_JOB, Types.VARCHAR, EMP_JOB.getName(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(EMP_SALARY, Types.DOUBLE, EMP_SALARY.getName())
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION.getName())
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK.getName(), T_EMP,
                    columnProperty(EMP_MGR, Types.INTEGER)),
            columnProperty(EMP_HIREDATE, Types.DATE, EMP_HIREDATE.getName())
                    .nullable(false),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION.getName()).preferredColumnWidth(100))
            .stringProvider(new StringProvider(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .caption("Employee");
  }
}
