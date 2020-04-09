/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.ui;

import org.jminor.swing.common.ui.reports.ReportUiWrapper;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import javax.swing.JComponent;

import static java.util.Objects.requireNonNull;

/**
 * A class responsible for displaying a JasperReport
 */
public final class JasperReportsUiWrapper implements ReportUiWrapper<JasperPrint> {

  /**
   * @param result the result to display
   * @return the component containing the report
   */
  @Override
  public JComponent createReportComponent(final JasperPrint result) {
    requireNonNull(result, "result");
    return new JRViewer(result);
  }
}
