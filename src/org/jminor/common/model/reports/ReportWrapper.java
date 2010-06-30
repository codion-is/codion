/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.reports;

import java.sql.Connection;

/**
 * User: Bjorn Darri<br>
 * Date: 23.5.2010<br>
 */
public interface ReportWrapper<R> {
  String getReportName();

  ReportResult<R> fillReport(final Connection connection) throws ReportException;

  ReportResult<R> fillReport(final ReportDataWrapper dataWrapper) throws ReportException;
}
