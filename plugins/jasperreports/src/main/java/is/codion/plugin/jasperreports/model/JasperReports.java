/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Factory for {@link Report} based on JasperReports.
 */
public final class JasperReports {

  private JasperReports() {}

  /**
   * @param name the report name
   * @return a JRReport
   */
  public static JRReportType reportType(String name) {
    return new DefaultJRReportType(name);
  }

  /**
   * Instantiates a JRReport for a classpath based report.
   * @param resourceClass the class owning the report resource
   * @param reportPath the report path, relative to the resource class
   * @return a report wrapper
   */
  public static JRReport classPathReport(Class<?> resourceClass, String reportPath) {
    return new ClassPathJRReport(resourceClass, reportPath);
  }

  /**
   * Instantiates a JRReport for a file based report, either loaded from a URL or from the filesystem.
   * @param reportPath the report path, relative to the central report path {@link Report#REPORT_PATH}
   * @return a report wrapper
   */
  public static JRReport fileReport(String reportPath) {
    return new FileJRReport(reportPath);
  }

  /**
   * Fills the report using the data source wrapped by the given data wrapper
   * @param report the report
   * @param dataSource the data provider to use for the report generation
   * @return a filled report ready for display
   * @throws ReportException in case of an exception
   */
  public static JasperPrint fillReport(Report<JasperReport, JasperPrint, Map<String, Object>> report,
                                       JRDataSource dataSource) throws ReportException {
    return fillReport(report, dataSource, new HashMap<>());
  }

  /**
   * Fills the report using the given data.
   * @param report the report
   * @param dataSource the data provider to use for the report generation
   * @param reportParameters the report parameters
   * @return a filled report ready for display
   * @throws ReportException in case of an exception
   */
  public static JasperPrint fillReport(Report<JasperReport, JasperPrint, Map<String, Object>> report,
                                       JRDataSource dataSource, Map<String, Object> reportParameters) throws ReportException {
    requireNonNull(report, "report");
    requireNonNull(dataSource, "dataSource");
    requireNonNull(reportParameters, "reportParameters");
    try {
      return JasperFillManager.fillReport(report.loadReport(), reportParameters, dataSource);
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new ReportException(e);
    }
  }
}
