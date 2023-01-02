/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReportTest {

  @Test
  void reportPathNotSpecified() {
    Report.REPORT_PATH.set(null);
    assertThrows(IllegalStateException.class, Report::reportPath);
  }

  @Test
  void reportPath() {
    final String path = "test/path";
    Report.REPORT_PATH.set(path);
    assertEquals(path, Report.reportPath());
  }
}
