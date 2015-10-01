/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
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

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
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
  private final Map<String, Object> reportParameters;
  private static final boolean CACHE_REPORTS = Configuration.getBooleanValue(Configuration.CACHE_REPORTS);
  private static final Map<String, JasperReport> REPORT_CACHE = Collections.synchronizedMap(new HashMap<String, JasperReport>());

  public JasperReportsWrapper(final String reportPath) {
    this(reportPath, new HashMap<String, Object>());
  }

  public JasperReportsWrapper(final String reportPath, final Map<String, Object> reportParameters) {
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
    catch (final JRException | MalformedURLException e) {
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
    catch (final JRException | MalformedURLException e) {
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
   * Loads a JasperReport file from the path given, it can be a URL a file or classpath resource.
   * @param reportPath the path to the report file to load
   * @return a loaded JasperReport file
   * @throws JRException in case loading the report fails
   * @throws MalformedURLException in case the report path is a malformed URL
   * @throws IllegalArgumentException in case the report path is not specified
   */
  public static JasperReport loadJasperReport(final String reportPath) throws JRException, MalformedURLException {
    Util.rejectNullValue(reportPath, "reportPath");
    if (reportPath.length() == 0) {
      throw new IllegalArgumentException("Empty report path");
    }
    if (CACHE_REPORTS && REPORT_CACHE.containsKey(reportPath)) {
      return REPORT_CACHE.get(reportPath);
    }
    final JasperReport report;
    if (reportPath.toLowerCase().startsWith("http")) {
      report = (JasperReport) JRLoader.loadObject(new URL(reportPath));
    }
    else {
      final File reportFile = new File(reportPath);
      if (reportFile.exists()) {
        report = (JasperReport) JRLoader.loadObject(reportFile);
      }
      else {
        report = (JasperReport) JRLoader.loadObject(JasperReportsWrapper.class.getResource("/" + reportPath));
      }
    }
    if (CACHE_REPORTS) {
      REPORT_CACHE.put(reportPath, report);
    }

    return report;
  }
}
