/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.ui;

import org.jminor.common.model.reports.ReportResult;
import org.jminor.swing.common.ui.reports.ReportUIWrapper;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;

import javax.swing.JComponent;
import java.util.Objects;

public final class JasperReportsUIWrapper implements ReportUIWrapper<JasperPrint> {

  @Override
  public JComponent createReportComponent(final ReportResult<JasperPrint> result) {
    Objects.requireNonNull(result, "result");
    return new JRViewer(result.getResult());
  }
}
