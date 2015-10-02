/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.model;

import org.jminor.common.model.reports.ReportException;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.swing.framework.model.reporting.EntityReportUtil;

import org.junit.Test;
import ro.nextreports.engine.ReportRunner;

import java.util.Collections;

public class NextReportsWrapperTest {

  @Test
  public void fillReport() throws ReportException {
    final byte[] bytes = EntityReportUtil.fillReport(
            new NextReportsWrapper("plugins/nextreports/src/test/reports/test-report.report", Collections.emptyMap(), ReportRunner.CSV_FORMAT),
            EntityConnectionProvidersTest.CONNECTION_PROVIDER).getResult();
    System.out.println(bytes);
  }
}
