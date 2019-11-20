/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.reports;

import org.jminor.common.Configuration;
import org.jminor.common.value.PropertyValue;

import java.sql.Connection;

import static org.jminor.common.Util.nullOrEmpty;

/**
 * A simple wrapper for a report
 * @param <R> the type of the report being wrapped.
 * @param <D> the type of the report datasource
 */
public interface ReportWrapper<R, D> {

  /**
   * The report path used for the default report generation,
   * either file or http based
   */
  PropertyValue<String> REPORT_PATH = Configuration.stringValue("jminor.report.path", null);

  /**
   * @return the name of the report
   */
  String getReportName();

  /**
   * Fills the report using the given database connection
   * @param connection the connection to use for the report generation
   * @return a filled report ready for display
   * @throws ReportException in case of an exception
   */
  ReportResult<R> fillReport(final Connection connection) throws ReportException;

  /**
   * Fills the report using the data source wrapped by the given data wrapper
   * @param dataWrapper the data provider to use for the report generation
   * @return a filled report ready for display
   * @throws ReportException in case of an exception
   */
  ReportResult<R> fillReport(final ReportDataWrapper<D> dataWrapper) throws ReportException;

  /**
   * @return the value associated with {@link ReportWrapper#REPORT_PATH}
   * @throws IllegalArgumentException in case it is not specified
   */
  static String getReportPath() {
    final String path = REPORT_PATH.get();
    if (nullOrEmpty(path)) {
      throw new IllegalArgumentException(REPORT_PATH + " property is not specified");
    }

    return path;
  }
}
