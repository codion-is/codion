/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.User;
import org.jminor.common.db.reports.ReportException;
import org.jminor.dbms.h2database.H2Database;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.swing.framework.model.reporting.EntityReportUtil;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperPrint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JasperReportsWrapperTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final String REPORT_PATH = "build/classes/reports/test/empdept_employees.jasper";

  @Test
  public void fillJdbcReport() throws ReportException {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(
            new H2Database("JasperReportsWrapperTest.fillJdbcReport",
                    System.getProperty("jminor.db.initScript")))
            .setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final JasperPrint print = EntityReportUtil.fillReport(
            new JasperReportsWrapper(REPORT_PATH, reportParameters),
            connectionProvider).getResult();
    assertNotNull(print);
    EntityReportUtil.fillReport(
            new JasperReportsWrapper(REPORT_PATH),
            connectionProvider).getResult();
  }

  @Test
  public void fillDataSourceReport() throws ReportException {
    final JasperReportsWrapper wrapper = new JasperReportsWrapper(REPORT_PATH);
    final JasperReportsDataWrapper dataWrapper = new JasperReportsDataWrapper(new JRDataSource() {
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
    wrapper.fillReport(dataWrapper);
  }

  @Test
  public void fillJdbcReportInvalidReport() throws Exception {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(
            new H2Database("JasperReportsWrapperTest.fillJdbcReportInvalidReport",
                    System.getProperty("jminor.db.initScript")))
            .setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);
    assertThrows(ReportException.class, () -> EntityReportUtil.fillReport(new JasperReportsWrapper("build/classes/reports/test/non_existing.jasper",
            new HashMap<>()), connectionProvider).getResult());
  }
}
