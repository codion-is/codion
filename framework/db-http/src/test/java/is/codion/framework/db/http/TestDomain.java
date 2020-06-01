/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.AbstractDatabaseFunction;
import is.codion.common.db.operation.AbstractDatabaseProcedure;
import is.codion.common.db.reports.AbstractReportWrapper;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportWrapper;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.EntityIdentity;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.BlobAttribute;
import is.codion.framework.domain.property.EntityAttribute;
import is.codion.framework.domain.property.Identities;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public final class TestDomain extends Domain {

  public static final ReportWrapper<Object, String, String> REPORT = new AbstractReportWrapper<Object, String, String>("report.path") {
    @Override
    public String fillReport(final Connection connection, final String parameters) throws ReportException {
      return "result";
    }

    @Override
    public Object loadReport() throws ReportException {
      return null;
    }
  };

  public TestDomain() {
    department();
    employee();
    operations();
    addReport(REPORT);
    registerEntities();
  }

  public static final EntityIdentity T_DEPARTMENT = Identities.entityIdentity("scott.dept");
  public static final Attribute<Integer> DEPARTMENT_ID = T_DEPARTMENT.integerAttribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = T_DEPARTMENT.stringAttribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = T_DEPARTMENT.stringAttribute("loc");

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.getName())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .orderBy(orderBy().ascending(DEPARTMENT_NAME))
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .caption("Department");
  }

  public static final EntityIdentity T_EMP = Identities.entityIdentity("scott.emp");
  public static final Attribute<Integer> EMP_ID = T_EMP.integerAttribute("empno");
  public static final Attribute<String> EMP_NAME = T_EMP.stringAttribute("ename");
  public static final Attribute<String> EMP_JOB = T_EMP.stringAttribute("job");
  public static final Attribute<Integer> EMP_MGR = T_EMP.integerAttribute("mgr");
  public static final Attribute<LocalDate> EMP_HIREDATE = T_EMP.localDateAttribute("hiredate");
  public static final Attribute<Double> EMP_SALARY = T_EMP.doubleAttribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = T_EMP.doubleAttribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = T_EMP.integerAttribute("deptno");
  public static final EntityAttribute EMP_DEPARTMENT_FK = T_EMP.entityAttribute("dept_fk");
  public static final EntityAttribute EMP_MGR_FK = T_EMP.entityAttribute("mgr_fk");
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = T_EMP.stringAttribute("location");
  public static final BlobAttribute EMP_DATA = T_EMP.blobAttribute("data");

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
                    DEPARTMENT_LOCATION.getName()).preferredColumnWidth(100),
            columnProperty(EMP_DATA, "Data"))
            .stringProvider(new StringProvider(EMP_NAME))
            .keyGenerator(increment("scott.emp", "empno"))
            .orderBy(orderBy().ascending(EMP_DEPARTMENT, EMP_NAME))
            .caption("Employee");
  }

  public static final String FUNCTION_ID = "functionId";
  public static final String PROCEDURE_ID = "procedureId";

  void operations() {
    addOperation(new AbstractDatabaseProcedure<EntityConnection>(PROCEDURE_ID, "Test Procedure") {
      @Override
      public void execute(final EntityConnection connection, final Object... objects) throws DatabaseException {}
    });

    addOperation(new AbstractDatabaseFunction<EntityConnection, List>(FUNCTION_ID, "Test Function") {
      @Override
      public List execute(final EntityConnection connection, final Object... objects) throws DatabaseException {
        return emptyList();
      }
    });
  }
}
