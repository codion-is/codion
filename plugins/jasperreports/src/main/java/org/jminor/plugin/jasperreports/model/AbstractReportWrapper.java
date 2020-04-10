/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;

import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

abstract class AbstractReportWrapper implements ReportWrapper<JasperReport, JasperPrint> {

  private static final long serialVersionUID = 1;

  private static final Map<AbstractReportWrapper, JasperReport> REPORT_CACHE = Collections.synchronizedMap(new HashMap<>());

  private final Map<String, Object> reportParameters;
  protected final String reportPath;

  protected AbstractReportWrapper(final String reportPath, final Map<String, Object> reportParameters) {
    this.reportPath = requireNonNull(reportPath, "reportPath");
    this.reportParameters = requireNonNull(reportParameters, "reportParameters");
  }

  /** {@inheritDoc} */
  @Override
  public final JasperPrint fillReport(final Connection connection) throws ReportException {
    requireNonNull(connection, "connection");
    try {
      return JasperFillManager.fillReport(loadReportInternal(), reportParameters, connection);
    }
    catch (final Exception e) {
      throw new ReportException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String getReportName() {
    return reportPath;
  }

  private JasperReport loadReportInternal() throws ReportException {
    final Boolean cacheReports = CACHE_REPORTS.get();
    if (cacheReports && REPORT_CACHE.containsKey(this)) {
      return REPORT_CACHE.get(this);
    }
    final JasperReport report = loadReport();
    if (cacheReports) {
      REPORT_CACHE.put(this, report);
    }

    return report;
  }
}
