/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperPrint;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for {@link ReportWrapper} based on JasperReports.
 */
public final class JasperReports {

  /**
   * Instantiates a ReportWrapper for a classpath based report.
   * @param resourceClass the class owning the report resource
   * @param reportPath the report classpath
   * @return a report wrapper
   * @throws ReportException in case of an exception
   */
  public static ReportWrapper<JasperPrint, JRDataSource> classPathReport(final Class resourceClass, final String reportPath) {
    return classPathReport(resourceClass, reportPath, new HashMap<>());
  }

  /**
   * Instantiates a ReportWrapper for a classpath based report.
   * @param resourceClass the class owning the report resource
   * @param reportPath the report classpath
   * @param reportParameters the report parameters
   * @return a report wrapper
   * @throws ReportException in case of an exception
   */
  public static ReportWrapper<JasperPrint, JRDataSource> classPathReport(final Class resourceClass, final String reportPath,
                                                                         final Map<String, Object> reportParameters) {
    return new ClassPathReportWrapper(resourceClass, reportPath, reportParameters);
  }

  /**
   * Instantiates a ReportWrapper for a URL based report.
   * @param reportUrl the report URL
   * @return a report wrapper
   * @throws ReportException in case of an exception
   */
  public static ReportWrapper<JasperPrint, JRDataSource> urlReport(final String reportUrl) {
    return urlReport(reportUrl, new HashMap<>());
  }

  /**
   * Instantiates a ReportWrapper for a URL based report.
   * @param reportUrl the report URL
   * @param reportParameters the report parameters
   * @return a report wrapper
   * @throws ReportException in case of an exception
   */
  public static ReportWrapper<JasperPrint, JRDataSource> urlReport(final String reportUrl, final Map<String, Object> reportParameters) {
    return new UrlReportWrapper(reportUrl, reportParameters);
  }

  /**
   * Instantiates a ReportWrapper for a filesystem based report.
   * @param reportPath the report path, relative to the central report path {@link ReportWrapper#REPORT_PATH}
   * @return a report wrapper
   * @throws ReportException in case of an exception
   */
  public static ReportWrapper<JasperPrint, JRDataSource> fileSystemReport(final String reportPath) {
    return fileSystemReport(reportPath, new HashMap<>());
  }

  /**
   * Instantiates a ReportWrapper for a filesystem based report.
   * @param reportPath the report path, relative to the central report path {@link ReportWrapper#REPORT_PATH}
   * @param reportParameters the report parameters
   * @return a report wrapper
   * @throws ReportException in case of an exception
   */
  public static ReportWrapper<JasperPrint, JRDataSource> fileSystemReport(final String reportPath, final Map<String, Object> reportParameters) {
    return new FileSystemReportWrapper(reportPath, reportParameters);
  }
}
