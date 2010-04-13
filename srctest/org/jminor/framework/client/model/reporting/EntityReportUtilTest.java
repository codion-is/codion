/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.reporting;

import org.jminor.framework.db.EntityDbConnectionTest;

import net.sf.jasperreports.engine.JasperPrint;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

public class EntityReportUtilTest {

  @Test
  public void fillJdbcReport() throws Exception {
    final HashMap<String, Object> reportParameters = new HashMap<String, Object>();
    reportParameters.put("DEPTNO", Arrays.asList(10, 20));
    final JasperPrint print = EntityReportUtil.fillJdbcReport(EntityDbConnectionTest.DB_PROVIDER.getEntityDb(),
            "resources/demos/empdept/reports/empdept_employees.jasper", reportParameters);
    assertNotNull(print);
  }

  @Test
  public void loadReport() throws Exception {
    assertNotNull(EntityReportUtil.loadJasperReport("resources/demos/empdept/reports/empdept_employees.jasper"));
  }
}
