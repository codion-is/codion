/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.reports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReportTest {

  @Test
  public void getReportPathNotSpecified() {
    Report.REPORT_PATH.set(null);
    assertThrows(IllegalArgumentException.class, Report::getReportPath);
  }

  @Test
  public void getReportPath() {
    final String path = "test/path";
    Report.REPORT_PATH.set(path);
    assertEquals(path, Report.getReportPath());
  }
}
