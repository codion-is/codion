/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.ui;

import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.ui.reports.ReportUIWrapper;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;

import javax.swing.JComponent;

public class JasperReportsUIWrapper implements ReportUIWrapper {

  public JComponent createReportComponent(final ReportResult result) {
    return new JRViewer((JasperPrint) result.getResult());
  }
}
