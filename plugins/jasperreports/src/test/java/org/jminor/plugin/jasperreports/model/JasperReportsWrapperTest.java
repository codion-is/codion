/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.dbms.h2database.H2Database;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperPrint;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JasperReportsWrapperTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  private static final String REPORT_PATH = "build/classes/reports/test/empdept_employees.jasper";

  @Test
  public void fillJdbcReport() throws ReportException, DatabaseException {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(
            new H2Database("JasperReportsWrapperTest.fillJdbcReport",
                    System.getProperty("jminor.db.initScript")))
            .setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", asList(10, 20));
    final JasperPrint print = connectionProvider.getConnection().fillReport(
            new JasperReportsWrapper(REPORT_PATH, reportParameters));
    assertNotNull(print);
    connectionProvider.getConnection().fillReport(new JasperReportsWrapper(REPORT_PATH));
  }

  @Test
  public void fillDataSourceReport() throws ReportException {
    final JasperReportsWrapper wrapper = new JasperReportsWrapper(REPORT_PATH);
    wrapper.fillReport(new JRDataSource() {
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
    });
  }

  @Test
  public void fillJdbcReportInvalidReport() throws Exception {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(
            new H2Database("JasperReportsWrapperTest.fillJdbcReportInvalidReport",
                    System.getProperty("jminor.db.initScript")))
            .setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);
    assertThrows(ReportException.class, () -> connectionProvider.getConnection().fillReport(
            new JasperReportsWrapper("build/classes/reports/test/non_existing.jasper", new HashMap<>())));
  }
}
