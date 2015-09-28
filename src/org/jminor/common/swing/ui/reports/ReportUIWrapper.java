/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.swing.ui.reports;

import org.jminor.common.model.reports.ReportResult;

import javax.swing.JComponent;

/**
 * A simple wrapper for a report presentation UI
 * @param <R> the type of the report result being wrapped.
 */
public interface ReportUIWrapper<R> {

  /**
   * Returns a JComponent showing a report based on the given report result.
   * @param result the report result to base the report on.
   * @return a JComponent showing a report.
   */
  JComponent createReportComponent(final ReportResult<R> result);
}
