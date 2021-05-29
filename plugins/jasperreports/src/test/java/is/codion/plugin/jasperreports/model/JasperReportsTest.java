/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.common.http.server.HttpServer;
import is.codion.common.http.server.HttpServerConfiguration;
import is.codion.common.http.server.ServerHttps;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;

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
import static org.junit.jupiter.api.Assertions.*;

public class JasperReportsTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final String REPORT_PATH = "build/resources/test";

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          new H2DatabaseFactory().createDatabase("jdbc:h2:mem:JasperReportsWrapperTest",
                  Database.DATABASE_INIT_SCRIPTS.get()))
          .setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @AfterAll
  public static void tearDown() {
    CONNECTION_PROVIDER.close();
  }

  @Test
  public void fillJdbcReport() throws ReportException, DatabaseException {
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set(REPORT_PATH);
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    final LocalEntityConnection connection = (LocalEntityConnection) CONNECTION_PROVIDER.getConnection();
    final JasperPrint print = TestDomain.EMPLOYEE_CLASSPATH_REPORT.fillReport(connection.getDatabaseConnection().getConnection(), reportParameters);
    assertNotNull(print);
  }

  @Test
  public void fillDataSourceReport() throws ReportException, MalformedURLException, JRException {
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set(REPORT_PATH);
    final Report<JasperReport, JasperPrint, Map<String, Object>> wrapper = JasperReports.fileReport("empdept_employees.jasper");
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
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set(REPORT_PATH);
    final ReportType<Object, Object, Object> nonExisting = ReportType.reportType("test");
    assertThrows(IllegalArgumentException.class, () -> CONNECTION_PROVIDER.getConnection().fillReport(nonExisting, new HashMap<>()));
  }

  @Test
  public void urlReport() throws Exception {
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set("http://localhost:1234");
    final HttpServerConfiguration configuration = HttpServerConfiguration.configuration(1234, ServerHttps.FALSE);
    configuration.setDocumentRoot(REPORT_PATH);
    final HttpServer server = new HttpServer(configuration);
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
    final JRReportType report = JasperReports.reportType("report");
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    final LocalEntityConnection connection = (LocalEntityConnection) CONNECTION_PROVIDER.getConnection();
    report.fillReport(connection.getDatabaseConnection().getConnection(), TestDomain.EMPLOYEE_CLASSPATH_REPORT, reportParameters);

    assertThrows(ReportException.class, () -> new ClassPathJRReport(JasperReportsTest.class, "non-existing.jasper").loadReport());
  }

  @Test
  public void fileReport() throws DatabaseException, ReportException {
    final JRReportType report = JasperReports.reportType("report");
    final Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    final LocalEntityConnection connection = (LocalEntityConnection) CONNECTION_PROVIDER.getConnection();
    report.fillReport(connection.getDatabaseConnection().getConnection(), TestDomain.EMPLOYEE_FILE_REPORT, reportParameters);

    assertThrows(ReportException.class, () -> new FileJRReport("/non-existing.jasper").loadReport());
  }

  @Test
  public void reportType() {
    assertNotEquals(JasperReports.reportType("name"), JasperReports.reportType("another"));
  }
}
