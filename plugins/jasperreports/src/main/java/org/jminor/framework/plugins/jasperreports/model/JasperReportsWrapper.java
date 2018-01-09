/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.Configuration;
import org.jminor.common.Value;
import org.jminor.common.db.reports.ReportDataWrapper;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;

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
import java.util.Objects;

/**
 * A Jasper Reports wrapper.
 */
public final class JasperReportsWrapper implements ReportWrapper<JasperPrint, JRDataSource>, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * Specifies whether or not reports are cached when loaded from disk/network,<br>
   * this prevents "hot deploy" of reports.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final Value<Boolean> CACHE_REPORTS = Configuration.booleanValue("jminor.report.cacheReports", true);

  private final String reportPath;
  private final Map<String, Object> reportParameters;
  private static final Map<String, JasperReport> REPORT_CACHE = Collections.synchronizedMap(new HashMap<>());

  /**
   * @param reportPath the report path
   */
  public JasperReportsWrapper(final String reportPath) {
    this(reportPath, new HashMap<>());
  }

  /**
   * @param reportPath the report path
   * @param reportParameters the report parameters
   */
  public JasperReportsWrapper(final String reportPath, final Map<String, Object> reportParameters) {
    Objects.requireNonNull(reportPath, "reportPath");
    Objects.requireNonNull(reportParameters, "reportParameters");
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
    Objects.requireNonNull(connection, "connection");
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
    Objects.requireNonNull(dataWrapper, "dataWrapper");
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
    Objects.requireNonNull(reportPath, "reportPath");
    if (reportPath.length() == 0) {
      throw new IllegalArgumentException("Empty report path");
    }
    if (CACHE_REPORTS.get() && REPORT_CACHE.containsKey(reportPath)) {
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
        try {
          report = (JasperReport) JRLoader.loadObject(JasperReportsWrapper.class.getResource("/" + reportPath));
        }
        catch (final NullPointerException e) {
          throw new JRException("Report '" + reportPath + "' not found in file system or classpath");
        }
      }
    }
    if (CACHE_REPORTS.get()) {
      REPORT_CACHE.put(reportPath, report);
    }

    return report;
  }
}
