/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jasperreports;

import is.codion.common.db.database.Database;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.common.user.User;
import is.codion.dbms.h2.H2DatabaseFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.plugin.jasperreports.TestDomain.Employee;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperPrint;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

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
                  .domain(new TestDomain())
                  .user(UNIT_TEST_USER)
                  .build();

  @AfterAll
  public static void tearDown() {
    CONNECTION_PROVIDER.close();
  }

  @Test
  void fillJdbcReport() throws ReportException {
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set(REPORT_PATH);
    HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
    JasperPrint print = Employee.CLASS_PATH_REPORT.fill(connection.databaseConnection().getConnection(), reportParameters);
    assertNotNull(print);
  }

  @Test
  void fillDataSourceReport() throws ReportException {
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set(REPORT_PATH);
    JRReport wrapper = JasperReports.fileReport("employees.jasper");
    JRDataSource dataSource = new JRDataSource() {
      boolean done = false;
      @Override
      public boolean next() {
        if (done) {
          return false;
        }

        return done = true;
      }

      @Override
      public Object getFieldValue(JRField jrField) {
        return null;
      }
    };
    JasperReports.fillReport(wrapper, dataSource);
  }

  @Test
  void fillJdbcReportInvalidReport() {
    Report.CACHE_REPORTS.set(false);
    Report.REPORT_PATH.set(REPORT_PATH);
    ReportType<Object, Object, Object> nonExisting = ReportType.reportType("test");
    assertThrows(IllegalArgumentException.class, () -> CONNECTION_PROVIDER.connection().report(nonExisting, new HashMap<>()));
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
      Employee.FILE_REPORT.fill(connection.databaseConnection().getConnection(), reportParameters);
    }
    finally {
      server.stop();
    }
  }

  @Test
  void classPathReport() throws ReportException {
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
    Employee.CLASS_PATH_REPORT.fill(connection.databaseConnection().getConnection(), reportParameters);

    assertThrows(ReportException.class, () -> new ClassPathJRReport(JasperReportsTest.class, "non-existing.jasper").load());
  }

  @Test
  void fileReport() throws ReportException {
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    LocalEntityConnection connection = CONNECTION_PROVIDER.connection();
    Employee.FILE_REPORT.fill(connection.databaseConnection().getConnection(), reportParameters);
    assertTrue(Employee.FILE_REPORT.cached());
    Employee.FILE_REPORT.clearCache();
    assertFalse(Employee.FILE_REPORT.cached());

    assertThrows(ReportException.class, () -> new FileJRReport("/non-existing.jasper", false).load());
  }

  @Test
  void reportType() {
    assertNotEquals(JasperReports.reportType("name"), JasperReports.reportType("another"));
  }
}
