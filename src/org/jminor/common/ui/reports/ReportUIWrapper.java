/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.reports;

import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;

import javax.swing.JComponent;

/**
 * User: darri
 * Date: 25.5.2010
 * Time: 14:42:03
 */
public interface ReportUIWrapper {
  JComponent createReportComponent(final ReportResult result) throws ReportException;
}
