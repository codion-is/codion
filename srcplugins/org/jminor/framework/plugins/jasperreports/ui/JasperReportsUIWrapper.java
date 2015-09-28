/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.ui;

import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.swing.ui.reports.ReportUIWrapper;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;

import javax.swing.JComponent;

public final class JasperReportsUIWrapper implements ReportUIWrapper<JasperPrint> {

  @Override
  public JComponent createReportComponent(final ReportResult<JasperPrint> result) {
    Util.rejectNullValue(result, "result");
    return new JRViewer(result.getResult());
  }
}
