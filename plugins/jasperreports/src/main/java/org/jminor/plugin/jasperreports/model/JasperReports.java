/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Factory for {@link ReportWrapper} based on JasperReports.
 */
public final class JasperReports {

  private JasperReports() {}

  /**
   * Instantiates a ReportWrapper for a classpath based report.
   * @param resourceClass the class owning the report resource
   * @param reportPath the report classpath
   * @return a report wrapper
   */
  public static ReportWrapper<JasperReport, JasperPrint> classPathReport(final Class resourceClass, final String reportPath) {
    return classPathReport(resourceClass, reportPath, new HashMap<>());
  }

  /**
   * Instantiates a ReportWrapper for a classpath based report.
   * @param resourceClass the class owning the report resource
   * @param reportPath the report classpath
   * @param reportParameters the report parameters
   * @return a report wrapper
   */
  public static ReportWrapper<JasperReport, JasperPrint> classPathReport(final Class resourceClass, final String reportPath,
                                                                         final Map<String, Object> reportParameters) {
    return new ClassPathReportWrapper(resourceClass, reportPath, reportParameters);
  }

  /**
   * Instantiates a ReportWrapper for a file based report, either loaded from a URL or from the filesystem.
   * @param reportPath the report path, relative to the central report path {@link ReportWrapper#REPORT_PATH}
   * @return a report wrapper
   */
  public static ReportWrapper<JasperReport, JasperPrint> fileReport(final String reportPath) {
    return fileReport(reportPath, new HashMap<>());
  }

  /**
   * Instantiates a ReportWrapper for a file based report, either loaded from a URL or from the filesystem.
   * @param reportPath the report path, relative to the central report path {@link ReportWrapper#REPORT_PATH}
   * @param reportParameters the report parameters
   * @return a report wrapper
   */
  public static ReportWrapper<JasperReport, JasperPrint> fileReport(final String reportPath, final Map<String, Object> reportParameters) {
    return new FileReportWrapper(reportPath, reportParameters);
  }

  /**
   * Fills the report using the data source wrapped by the given data wrapper
   * @param reportWrapper the report wrapper
   * @param dataSource the data provider to use for the report generation
   * @return a filled report ready for display
   * @throws ReportException in case of an exception
   */
  public static JasperPrint fillReport(final ReportWrapper<JasperReport, JasperPrint> reportWrapper, final JRDataSource dataSource) throws ReportException {
    return fillReport(reportWrapper, dataSource, new HashMap<>());
  }

  /**
   * Fills the report using the data source wrapped by the given data wrapper
   * @param reportWrapper the report wrapper
   * @param dataSource the data provider to use for the report generation
   * @param reportParameters the report parameters
   * @return a filled report ready for display
   * @throws ReportException in case of an exception
   */
  public static JasperPrint fillReport(final ReportWrapper<JasperReport, JasperPrint> reportWrapper, final JRDataSource dataSource,
                                       final Map<String, Object> reportParameters) throws ReportException {
    requireNonNull(reportWrapper, "reportWrapper");
    requireNonNull(dataSource, "dataSource");
    requireNonNull(reportParameters, "reportParameters");
    try {
      return JasperFillManager.fillReport(reportWrapper.loadReport(), reportParameters, dataSource);
    }
    catch (final Exception e) {
      throw new ReportException(e);
    }
  }
}
