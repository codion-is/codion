/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.plugin.jasperreports.model.TestDomain.Employee;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperPrint;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class JasperReportsTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final String REPORT_PATH = "build/resources/test";

  private static final EntityConnectionProvider CONNECTION_PROVIDER =
          LocalEntityConnectionProvider.builder()
                  .database(H2DatabaseFactory.createDatabase("jdbc:h2:mem:JasperReportsWrapperTest",
                          Database.DATABASE_INIT_SCRIPTS.get()))
                  .domainClassName(TestDomain.class.getName())
                  .user(UNIT_TEST_USER)
                  .build();

  @AfterAll
  public static void tearDown() {
    CONNECTION_PROVIDER.close();
  }

  @Test
  void fillJdbcReport() throws ReportException, DatabaseException {
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set(REPORT_PATH);
    HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
    JasperPrint print = Employee.CLASS_PATH_REPORT.fillReport(connection.databaseConnection().getConnection(), reportParameters);
    assertNotNull(print);
  }

  @Test
  void fillDataSourceReport() throws ReportException, MalformedURLException, JRException {
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set(REPORT_PATH);
    JRReport wrapper = JasperReports.fileReport("empdept_employees.jasper");
    JRDataSource dataSource = new JRDataSource() {
      boolean done = false;
      @Override
      public boolean next() throws JRException {
        if (done) {
          return false;
        }

        return done = true;
      }

      @Override
      public Object getFieldValue(JRField jrField) throws JRException {
        return null;
      }
    };
    JasperReports.fillReport(wrapper, dataSource);
  }

  @Test
  void fillJdbcReportInvalidReport() throws Exception {
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set(REPORT_PATH);
    ReportType<Object, Object, Object> nonExisting = ReportType.reportType("test");
    assertThrows(IllegalArgumentException.class, () -> CONNECTION_PROVIDER.connection().fillReport(nonExisting, new HashMap<>()));
  }

  @Test
  void urlReport() throws Exception {
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set("http://localhost:1234");
    Server server = new Server(1234);
    HandlerList handlers = new HandlerList();
    ResourceHandler fileHandler = new ResourceHandler();
    fileHandler.setResourceBase(REPORT_PATH);
    handlers.addHandler(fileHandler);
    server.setHandler(handlers);
    try {
      server.start();
      Map<String, Object> reportParameters = new HashMap<>();
      reportParameters.put("DEPTNO", asList(10, 20));
      LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
      Employee.FILE_REPORT.fillReport(connection.databaseConnection().getConnection(), reportParameters);
    }
    finally {
      server.stop();
    }
  }

  @Test
  void classPathReport() throws DatabaseException, ReportException {
    JRReportType report = JasperReports.reportType("report");
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
    report.fillReport(Employee.CLASS_PATH_REPORT, connection.databaseConnection().getConnection(), reportParameters);

    assertThrows(ReportException.class, () -> new ClassPathJRReport(JasperReportsTest.class, "non-existing.jasper").loadReport());
  }

  @Test
  void fileReport() throws DatabaseException, ReportException {
    JRReportType report = JasperReports.reportType("report");
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
    report.fillReport(Employee.FILE_REPORT, connection.databaseConnection().getConnection(), reportParameters);
    assertTrue(Employee.FILE_REPORT.isCached());
    Employee.FILE_REPORT.clearCache();
    assertFalse(Employee.FILE_REPORT.isCached());

    assertThrows(ReportException.class, () -> new FileJRReport("/non-existing.jasper", false).loadReport());
  }

  @Test
  void reportType() {
    assertNotEquals(JasperReports.reportType("name"), JasperReports.reportType("another"));
  }
}
