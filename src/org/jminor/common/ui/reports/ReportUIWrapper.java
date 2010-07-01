/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.reports;

import org.jminor.common.model.reports.ReportResult;

import javax.swing.JComponent;

/**
 * User: darri<br>
 * Date: 25.5.2010<br>
 * Time: 14:42:03
 */
public interface ReportUIWrapper<R> {

  /**
   * Returns a JComponent showing a report based on the given report result.
   * @param result the report result to base the report on.
   * @return a JComponent showing a report.
   */
  JComponent createReportComponent(final ReportResult<R> result);
}
