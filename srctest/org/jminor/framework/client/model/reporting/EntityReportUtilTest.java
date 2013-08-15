/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.reporting;

import org.jminor.framework.db.DefaultEntityConnectionTest;
import org.jminor.framework.plugins.jasperreports.model.JasperReportsWrapper;

import net.sf.jasperreports.engine.JasperPrint;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;

public class EntityReportUtilTest {

  @Test
  public void fillJdbcReport() throws Exception {
    final HashMap<String, Object> reportParameters = new HashMap<String, Object>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final JasperPrint print = EntityReportUtil.fillReport(
            new JasperReportsWrapper("resources/demos/empdept/reports/empdept_employees.jasper", reportParameters),
            DefaultEntityConnectionTest.CONNECTION_PROVIDER).getResult();
    assertNotNull(print);
  }
}
