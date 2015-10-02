/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.swing;

import org.jminor.framework.plugins.nextreports.model.NextReportsResult;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public final class NextReportsUiUtil {

  public static void openReport(final NextReportsResult reportsResult, final File file) throws IOException {
    Desktop.getDesktop().open(reportsResult.writeResultToFile(file));
  }
}
