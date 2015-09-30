/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.model.reports.ReportException;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;
import org.jminor.swing.framework.model.reporting.EntityReportUtil;

import net.sf.jasperreports.engine.JasperPrint;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;

public class JasperReportsWrapperTest {

  static {
    TestDomain.init();
  }

  @Test
  public void fillJdbcReport() throws Exception {
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final JasperPrint print = EntityReportUtil.fillReport(
            new JasperReportsWrapper("demos/src/main/reports/empdept/empdept_employees.jasper", reportParameters),
            EntityConnectionProvidersTest.CONNECTION_PROVIDER).getResult();
    assertNotNull(print);
  }

  @Test(expected = ReportException.class)
  public void fillJdbcReportInvalidReport() throws Exception {
    EntityReportUtil.fillReport( new JasperReportsWrapper("demos/src/main/reports/empdept/non_existing_report.jasper",
            new HashMap<String, Object>()), EntityConnectionProvidersTest.CONNECTION_PROVIDER).getResult();
  }
}
