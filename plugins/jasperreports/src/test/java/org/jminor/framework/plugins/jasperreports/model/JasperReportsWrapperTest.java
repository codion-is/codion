/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.model.User;
import org.jminor.common.model.reports.ReportException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.swing.framework.model.reporting.EntityReportUtil;

import net.sf.jasperreports.engine.JasperPrint;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;

public class JasperReportsWrapperTest {

  @Test
  public void fillJdbcReport() throws Exception {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(User.UNIT_TEST_USER,
            new H2Database("h2db", System.getProperty("jminor.db.initScript")));
    final HashMap<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final JasperPrint print = EntityReportUtil.fillReport(
            new JasperReportsWrapper("build/test/empdept_employees.jasper", reportParameters),
            connectionProvider).getResult();
    assertNotNull(print);
  }

  @Test(expected = ReportException.class)
  public void fillJdbcReportInvalidReport() throws Exception {
    EntityReportUtil.fillReport( new JasperReportsWrapper("build/test/non_existing.jasper",
            new HashMap<String, Object>()), EntityConnectionProvidersTest.CONNECTION_PROVIDER).getResult();
  }
}
