/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.User;
import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.model.reports.ReportException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.swing.framework.model.reporting.EntityReportUtil;

import net.sf.jasperreports.engine.JasperPrint;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;

public class JasperReportsWrapperTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  @Test
  public void fillJdbcReport() throws Exception {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(UNIT_TEST_USER,
            new H2Database("JasperReportsWrapperTest.fillJdbcReport", System.getProperty("jminor.db.initScript")));
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final JasperPrint print = EntityReportUtil.fillReport(
            new JasperReportsWrapper("build/test/empdept_employees.jasper", reportParameters),
            connectionProvider).getResult();
    assertNotNull(print);
  }

  @Test(expected = ReportException.class)
  public void fillJdbcReportInvalidReport() throws Exception {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(UNIT_TEST_USER,
            new H2Database("JasperReportsWrapperTest.fillJdbcReportInvalidReport", System.getProperty("jminor.db.initScript")));
    EntityReportUtil.fillReport(new JasperReportsWrapper("build/test/non_existing.jasper",
            new HashMap<>()), connectionProvider).getResult();
  }
}
