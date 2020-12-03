/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.time.LocalDate;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
import static is.codion.framework.domain.property.Properties.*;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static is.codion.plugin.jasperreports.model.JasperReports.fileReport;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    super(DOMAIN);
    department();
    employee();
  }

  public static final EntityType<Entity> T_DEPARTMENT = DOMAIN.entityType("scott.dept");
  public static final Attribute<Integer> DEPARTMENT_ID = T_DEPARTMENT.integerAttribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = T_DEPARTMENT.stringAttribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = T_DEPARTMENT.stringAttribute("loc");

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.getName())
                    .searchProperty().preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset()
            .stringFactory(stringFactory(DEPARTMENT_NAME))
            .caption("Department");
  }


  public static final EntityType<Entity> T_EMP = DOMAIN.entityType("scott.emp");
  public static final Attribute<Integer> EMP_ID = T_EMP.integerAttribute("empno");
  public static final Attribute<String> EMP_NAME = T_EMP.stringAttribute("ename");
  public static final Attribute<String> EMP_JOB = T_EMP.stringAttribute("job");
  public static final Attribute<Integer> EMP_MGR = T_EMP.integerAttribute("mgr");
  public static final Attribute<LocalDate> EMP_HIREDATE = T_EMP.localDateAttribute("hiredate");
  public static final Attribute<Double> EMP_SALARY = T_EMP.doubleAttribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = T_EMP.doubleAttribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = T_EMP.integerAttribute("deptno");
  public static final ForeignKey EMP_DEPARTMENT_FK = T_EMP.foreignKey("dept_fk", EMP_DEPARTMENT, DEPARTMENT_ID);
  public static final ForeignKey EMP_MGR_FK = T_EMP.foreignKey("mgr_fk", EMP_MGR, EMP_ID);
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = T_EMP.stringAttribute("location");

  public static final JRReport EMPLOYEE_FILE_REPORT =
          fileReport("/empdept_employees.jasper");
  public static final JRReport EMPLOYEE_CLASSPATH_REPORT =
          classPathReport(TestDomain.class, "/empdept_employees.jasper");

  void employee() {
    define(T_EMP,
            primaryKeyProperty(EMP_ID, EMP_ID.getName()),
            columnProperty(EMP_NAME, EMP_NAME.getName())
                    .searchProperty().maximumLength(10).nullable(false),
            columnProperty(EMP_DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK.getName()),
            valueListProperty(EMP_JOB, EMP_JOB.getName(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(),
            columnProperty(EMP_SALARY, EMP_SALARY.getName())
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2),
            columnProperty(EMP_COMMISSION, EMP_COMMISSION.getName())
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2),
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
