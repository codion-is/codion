/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.reporting;

import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.framework.db.provider.EntityDbProvider;

import java.util.Map;

/**
 * A static utility class for working with reports.<br>
 * User: Bjorn Darri<br>
 * Date: 30.7.2009<br>
 * Time: 17:58:09<br>
 */
public class EntityReportUtil {

  private EntityReportUtil() {}

  /**
   * Takes a ReportWrapper which uses a JDBC datasource and returns an initialized ReportResult object
   * @param reportWrapper the report wrapper
   * @param dbProvider the EntityDbProvider instance to use when filling the report
   * @param reportParameters the report parameters
   * @return an initialized ReportResult object
   * @throws ReportException in case of a report exception
   */
  public static ReportResult fillReport(final ReportWrapper reportWrapper, final EntityDbProvider dbProvider,
                                        final Map reportParameters) throws ReportException {
    try {
      return dbProvider.getEntityDb().fillReport(reportWrapper, reportParameters);
    }
    catch (ReportException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Takes a ReportWrapper object and returns an initialized ReportResult object
   * @param reportWrapper the report wrapper
   * @param dataSource the ReportDataWrapper to use
   * @param reportParameters the report parameters
   * @return an initialized ReportResult object
   * @throws ReportException in case of a report exception
   */
  public static ReportResult fillReport(final ReportWrapper reportWrapper, final ReportDataWrapper dataSource,
                                        final Map reportParameters) throws ReportException {
    return reportWrapper.fillReport(reportParameters, dataSource);
  }
}
