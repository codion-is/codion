/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for {@link ReportWrapper} based on JasperReports.
 */
public final class JasperReports {

  /**
   * @param jasperReport the report object
   * @throws ReportException in case of an exception while loading the report
   */
  public static ReportWrapper<JasperPrint, JRDataSource> jasperReportsWrapper(final JasperReport jasperReport) throws ReportException {
    return jasperReportsWrapper(jasperReport, new HashMap<>());
  }

  /**
   * @param jasperReport the report object
   * @param reportParameters the report parameters
   * @throws ReportException in case of an exception while loading the report
   */
  public static ReportWrapper<JasperPrint, JRDataSource> jasperReportsWrapper(final JasperReport jasperReport, final Map<String, Object> reportParameters) throws ReportException {
    return new JasperReportsWrapper(jasperReport, reportParameters);
  }

  /**
   * @param reportPath the report path, relative to the central report path {@link ReportWrapper#REPORT_PATH}
   * @throws ReportException in case of an exception while loading the report
   */
  public static ReportWrapper<JasperPrint, JRDataSource> jasperReportsWrapper(final String reportPath) throws ReportException {
    return jasperReportsWrapper(reportPath, new HashMap<>());
  }

  /**
   * @param reportPath the report path, relative to the central report path {@link ReportWrapper#REPORT_PATH}
   * @param reportParameters the report parameters
   * @throws ReportException in case of an exception while loading the report
   */
  public static ReportWrapper<JasperPrint, JRDataSource> jasperReportsWrapper(final String reportPath, final Map<String, Object> reportParameters) throws ReportException {
    return new JasperReportsWrapper(reportPath, reportParameters);
  }

  /**
   * Loads a JasperReport file from the path given, it can be a URL, a file path or classpath resource path.
   * @param reportPath the path to the report file to load
   * @return a loaded JasperReport file
   * @throws JRException in case loading the report fails
   * @throws MalformedURLException in case the report path is a malformed URL
   * @throws IllegalArgumentException in case the report path is not specified
   */
  public static JasperReport loadJasperReport(final String reportPath) throws JRException, MalformedURLException {
    return JasperReportsWrapper.loadJasperReport(reportPath);
  }
}
