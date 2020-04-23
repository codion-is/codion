/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.common.remote.server.http.HttpServer;
import org.jminor.common.remote.server.http.HttpServerConfiguration;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.dbms.h2database.H2Database;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnection;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JasperReportsTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  private static final String REPORT_PATH = "build/resources/test";

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          new H2Database("JasperReportsWrapperTest.fillJdbcReport", System.getProperty("jminor.db.initScript")))
          .setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @AfterAll
  public static void tearDown() {
    CONNECTION_PROVIDER.disconnect();
  }

  @Test
  public void fillJdbcReport() throws ReportException, DatabaseException {
    ReportWrapper.CACHE_REPORTS.set(false);
    ReportWrapper.REPORT_PATH.set(REPORT_PATH);
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    final LocalEntityConnection connection = (LocalEntityConnection) CONNECTION_PROVIDER.getConnection();
    final JasperPrint print = TestDomain.EMPLOYEE_CLASSPATH_REPORT.fillReport(connection.getDatabaseConnection().getConnection(), reportParameters);
    assertNotNull(print);
  }

  @Test
  public void fillDataSourceReport() throws ReportException, MalformedURLException, JRException {
    ReportWrapper.CACHE_REPORTS.set(false);
    ReportWrapper.REPORT_PATH.set(REPORT_PATH);
    final ReportWrapper<JasperReport, JasperPrint, Map<String, Object>> wrapper = JasperReports.fileReport("empdept_employees.jasper");
    final JRDataSource dataSource = new JRDataSource() {
      boolean done = false;
      @Override
      public boolean next() throws JRException {
        if (done) {
          return false;
        }

        return done = true;
      }

      @Override
      public Object getFieldValue(final JRField jrField) throws JRException {
        return null;
      }
    };
    JasperReports.fillReport(wrapper, dataSource);
  }

  @Test
  public void fillJdbcReportInvalidReport() throws Exception {
    ReportWrapper.CACHE_REPORTS.set(false);
    ReportWrapper.REPORT_PATH.set(REPORT_PATH);
    assertThrows(ReportException.class, () -> CONNECTION_PROVIDER.getConnection().fillReport(
            JasperReports.fileReport("non_existing.jasper"), new HashMap<>()));
  }

  @Test
  public void urlReport() throws Exception {
    ReportWrapper.CACHE_REPORTS.set(false);
    ReportWrapper.REPORT_PATH.set("http://localhost:1234");
    final HttpServer server = new HttpServer(new HttpServerConfiguration(1234, false).documentRoot(REPORT_PATH));
    try {
      server.startServer();
      final HashMap<String, Object> reportParameters = new HashMap<>();
      reportParameters.put("DEPTNO", asList(10, 20));
      final LocalEntityConnection connection = (LocalEntityConnection) CONNECTION_PROVIDER.getConnection();
      TestDomain.EMPLOYEE_FILE_REPORT.fillReport(connection.getDatabaseConnection().getConnection(), reportParameters);
    }
    finally {
      server.stopServer();
    }
  }

  @Test
  public void classPathReport() throws DatabaseException, ReportException {
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    final LocalEntityConnection connection = (LocalEntityConnection) CONNECTION_PROVIDER.getConnection();
    TestDomain.EMPLOYEE_CLASSPATH_REPORT.fillReport(connection.getDatabaseConnection().getConnection(), reportParameters);
  }
}
