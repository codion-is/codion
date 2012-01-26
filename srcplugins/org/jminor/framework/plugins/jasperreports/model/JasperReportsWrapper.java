/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.framework.Configuration;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import java.io.Serializable;
import java.net.URL;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A Jasper Reports wrapper.
 */
public final class JasperReportsWrapper implements ReportWrapper<JasperPrint, JRDataSource>, Serializable {
  private static final long serialVersionUID = 1;
  private final String reportPath;
  private final Map reportParameters;
  private static final boolean cacheReports = Configuration.getBooleanValue(Configuration.CACHE_REPORTS);
  private static final Map<String, JasperReport> reportCache = Collections.synchronizedMap(new HashMap<String, JasperReport>());

  public JasperReportsWrapper(final String reportPath) {
    this(reportPath, new HashMap());
  }

  public JasperReportsWrapper(final String reportPath, final Map reportParameters) {
    Util.rejectNullValue(reportPath, "reportPath");
    Util.rejectNullValue(reportParameters, "reportParameters");
    this.reportPath = reportPath;
    this.reportParameters = reportParameters;
  }

  /** {@inheritDoc} */
  @Override
  public String getReportName() {
    return reportPath;
  }

  /** {@inheritDoc} */
  @Override
  public ReportResult<JasperPrint> fillReport(final Connection connection) throws ReportException {
    Util.rejectNullValue(connection, "connection");
    try {
      final JasperReport report = loadJasperReport(reportPath);
      return new JasperReportsResult(JasperFillManager.fillReport(report, reportParameters, connection));
    }
    catch (JRException e) {
      throw new ReportException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public ReportResult<JasperPrint> fillReport(final ReportDataWrapper<JRDataSource> dataWrapper) throws ReportException {
    Util.rejectNullValue(dataWrapper, "dataWrapper");
    try {
      final JasperReport report = loadJasperReport(reportPath);
      return new JasperReportsResult(JasperFillManager.fillReport(report, reportParameters, dataWrapper.getDataSource()));
    }
    catch (JRException e) {
      throw new ReportException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder(reportPath);
    if (reportParameters != null && reportParameters.size() > 0) {
      builder.append(", parameters: ").append(reportParameters.toString());
    }

    return builder.toString();
  }

  /**
   * Loads a JasperReport file from the path given, it can be a URL or a file path
   * @param reportPath the path to the report file to load
   * @return a loaded JasperReport file
   * @throws IllegalArgumentException in case the report path is not specified
   */
  public static JasperReport loadJasperReport(final String reportPath) {
    Util.rejectNullValue(reportPath, "reportPath");
    if (reportPath.isEmpty()) {
      throw new IllegalArgumentException("Empty report path");
    }
    if (cacheReports && reportCache.containsKey(reportPath)) {
      return reportCache.get(reportPath);
    }
    try {
      final JasperReport report;
      if (reportPath.toLowerCase().startsWith("http")) {
        report = (JasperReport) JRLoader.loadObject(new URL(reportPath));
      }
      else {
        report = (JasperReport) JRLoader.loadObject(reportPath);
      }
      if (cacheReports) {
        reportCache.put(reportPath, report);
      }

      return report;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
