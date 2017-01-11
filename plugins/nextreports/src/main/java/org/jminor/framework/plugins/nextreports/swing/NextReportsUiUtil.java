/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.swing;

import org.jminor.common.model.reports.ReportResult;
import org.jminor.framework.plugins.nextreports.model.NextReportsResult;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public final class NextReportsUiUtil {

  private NextReportsUiUtil() {}

  public static void openReport(final ReportResult<NextReportsResult> reportsResult, final File file) throws IOException {
    Desktop.getDesktop().open(reportsResult.getResult().writeResultToFile(file));
  }
}
