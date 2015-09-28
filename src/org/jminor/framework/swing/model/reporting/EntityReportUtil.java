/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.swing.model.reporting;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.framework.db.EntityConnectionProvider;

/**
 * A static utility class for working with reports.
 */
public final class EntityReportUtil {

  private EntityReportUtil() {}

  /**
   * Takes a ReportWrapper which uses a JDBC datasource and returns an initialized ReportResult object
   * @param reportWrapper the report wrapper
   * @param connectionProvider the EntityConnectionProvider instance to use when filling the report
   * @param <R> the type of the actual report object being wrapped
   * @param <D> the type of the data source used to fill the report
   * @return an initialized ReportResult object
   * @throws ReportException in case of a report exception
   */
  public static <R, D> ReportResult<R> fillReport(final ReportWrapper<R, D> reportWrapper, final EntityConnectionProvider connectionProvider) throws ReportException {
    try {
      return connectionProvider.getConnection().fillReport(reportWrapper);
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Takes a ReportWrapper object and returns an initialized ReportResult object
   * @param reportWrapper the report wrapper
   * @param dataSource the ReportDataWrapper to use
   * @param <R> the type of the actual report object being wrapped
   * @param <D> the type of the data source used to fill the report
   * @return an initialized ReportResult object
   * @throws ReportException in case of a report exception
   */
  public static <R, D> ReportResult<R> fillReport(final ReportWrapper<R, D> reportWrapper, final ReportDataWrapper<D> dataSource) throws ReportException {
    return reportWrapper.fillReport(dataSource);
  }
}
