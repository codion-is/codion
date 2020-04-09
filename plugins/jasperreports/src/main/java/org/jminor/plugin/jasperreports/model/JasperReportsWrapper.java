/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.Configuration;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.common.value.PropertyValue;

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

import static java.util.Objects.requireNonNull;

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
  public static final PropertyValue<Boolean> CACHE_REPORTS = Configuration.booleanValue("jminor.report.cacheReports", true);

  private final String reportPath;
  private final Map<String, Object> reportParameters;
  private static final Map<String, JasperReport> REPORT_CACHE = Collections.synchronizedMap(new HashMap<>());

  /**
   * @param reportPath the report path
   * @throws ReportException in case of an exception while loading the report
   */
  public JasperReportsWrapper(final String reportPath) throws ReportException {
    this(reportPath, new HashMap<>());
  }

  /**
   * @param reportPath the report path
   * @param reportParameters the report parameters
   * @throws ReportException in case of an exception while loading the report
   */
  public JasperReportsWrapper(final String reportPath, final Map<String, Object> reportParameters) throws ReportException {
    this.reportPath = requireNonNull(reportPath, "reportPath");
    this.reportParameters = requireNonNull(reportParameters, "reportParameters");
  }

  /** {@inheritDoc} */
  @Override
  public String getReportName() {
    return reportPath;
  }

  /** {@inheritDoc} */
  @Override
  public JasperPrint fillReport(final Connection connection) throws ReportException {
    requireNonNull(connection, "connection");
    try {
      return JasperFillManager.fillReport(loadJasperReport(reportPath), reportParameters, connection);
    }
    catch (final JRException | MalformedURLException e) {
      throw new ReportException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public JasperPrint fillReport(final JRDataSource dataSource) throws ReportException {
    requireNonNull(dataSource, "dataWrapper");
    try {
      return JasperFillManager.fillReport(loadJasperReport(reportPath), reportParameters, dataSource);
    }
    catch (final JRException | MalformedURLException e) {
      throw new ReportException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder(getReportName());
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
    requireNonNull(reportPath, "reportPath");
    if (reportPath.isEmpty()) {
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
