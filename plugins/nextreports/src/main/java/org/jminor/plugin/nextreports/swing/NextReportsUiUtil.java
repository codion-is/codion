/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.nextreports.swing;

import org.jminor.plugin.nextreports.model.NextReportsResult;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * A utility class for viewing a NextReports report
 */
public final class NextReportsUiUtil {

  private NextReportsUiUtil() {}

  /**
   * Writes the given report result to the given file and opens it
   * @param reportsResult the report result
   * @param file the file
   * @throws IOException if writing the file fails
   */
  public static void openReport(final NextReportsResult reportsResult, final File file) throws IOException {
    Desktop.getDesktop().open(reportsResult.writeResultToFile(file));
  }
}
