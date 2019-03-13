/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.model;

import org.jminor.common.User;
import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.swing.framework.model.reporting.EntityReportUtil;

import org.junit.jupiter.api.Test;
import ro.nextreports.engine.ReportRunner;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class NextReportsWrapperTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  @Test
  public void fillReport() throws ReportException, IOException {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(
            new H2Database("h2db", System.getProperty("jminor.db.initScript")))
            .setDomainClassName(Entities.class.getName()).setUser(UNIT_TEST_USER);
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
      //throws IllegalArgumentException
      result.getResult().writeResultToFile(tmpDir, filename);
      fail("Should not overwrite file");
    }
    catch (final IllegalArgumentException e) {/*expected*/}
    finally {
      if (file != null) {
        file.delete();
      }
    }
  }
}