/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.reports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReportWrapperTest {

  @Test
  public void getReportPathNotSpecified() {
    ReportWrapper.REPORT_PATH.set(null);
    assertThrows(IllegalArgumentException.class, ReportWrapper::getReportPath);
  }

  @Test
  public void getReportPath() {
    final String path = "test/path";
    ReportWrapper.REPORT_PATH.set(path);
    assertEquals(path, ReportWrapper.getReportPath());
  }
}
