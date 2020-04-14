/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.reports.ReportException;

import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

abstract class AbstractReportWrapper implements JasperReportWrapper  {

  private static final long serialVersionUID = 1;

  private static final Map<String, JasperReport> REPORT_CACHE = new ConcurrentHashMap<>();

  protected final String reportPath;

  protected AbstractReportWrapper(final String reportPath) {
    this.reportPath = requireNonNull(reportPath, "reportPath");
  }

  @Override
  public final JasperPrint fillReport(final Connection connection, final Map<String, Object> parameters) throws ReportException {
    requireNonNull(connection, "connection");
    try {
      return JasperFillManager.fillReport(loadReportInternal(), parameters == null ? new HashMap<>() : parameters, connection);
    }
    catch (final Exception e) {
      throw new ReportException(e);
    }
  }

  @Override
  public final String toString() {
    return getFullReportPath();
  }

  /**
   * @return a unique path for this report
   */
  protected abstract String getFullReportPath();

  private JasperReport loadReportInternal() throws ReportException {
    if (CACHE_REPORTS.get()) {
      return REPORT_CACHE.computeIfAbsent(getFullReportPath(), fullPath -> {
        try {
          return loadReport();
        }
        catch (final ReportException e) {
          throw new RuntimeException(e);
        }
      });
    }

    return loadReport();
  }
}
