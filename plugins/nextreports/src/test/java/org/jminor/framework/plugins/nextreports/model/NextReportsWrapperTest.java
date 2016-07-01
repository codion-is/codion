/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.model;

import org.jminor.common.User;
import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.swing.framework.model.reporting.EntityReportUtil;

import org.junit.Test;
import ro.nextreports.engine.ReportRunner;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class NextReportsWrapperTest {

  @Test
  public void fillReport() throws ReportException, IOException {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(User.UNIT_TEST_USER,
            new H2Database("h2db", System.getProperty("jminor.db.initScript")));
    final ReportResult<NextReportsResult> result = EntityReportUtil.fillReport(
            new NextReportsWrapper("src/test/reports/test-report.report",
                    Collections.emptyMap(), ReportRunner.CSV_FORMAT), connectionProvider);
    File file = null;
    try {
      final String tmpDir = System.getProperty("java.io.tmpdir");
      final String filename = "NextReportsWrapperTest" + System.currentTimeMillis();
      file = result.getResult().writeResultToFile(tmpDir, filename);
      file.deleteOnExit();
      assertEquals(file.length(), result.getResult().getResult().length);
    }
    finally {
      if (file != null) {
        file.delete();
      }
    }
  }
}