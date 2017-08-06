/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.ui;

import org.jminor.common.db.reports.ReportResult;
import org.jminor.swing.common.ui.reports.ReportUIWrapper;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;

import javax.swing.JComponent;
import java.util.Objects;

/**
 * A class responsible for displaying a JasperReport
 */
public final class JasperReportsUIWrapper implements ReportUIWrapper<JasperPrint> {

  /**
   * @param result the result to display
   * @return the component containing the report
   */
  @Override
  public JComponent createReportComponent(final ReportResult<JasperPrint> result) {
    Objects.requireNonNull(result, "result");
    return new JRViewer(result.getResult());
  }
}
