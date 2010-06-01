/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import java.io.Serializable;
import java.net.URL;
import java.sql.Connection;
import java.util.Map;

/**
 * User: Bjorn Darri<br>
 * Date: 23.5.2010<br>
 * Time: 21:16:12
 */
public class JasperReportsWrapper implements ReportWrapper<JasperPrint>, Serializable {
  private static final long serialVersionUID = 1;
  private final JasperReport report;

  public JasperReportsWrapper(final String reportPath) {
    this(loadJasperReport(reportPath));
  }

  public JasperReportsWrapper(final JasperReport report) {
    this.report = report;
  }

  public String getReportName() {
    return report.getName();
  }

  public ReportResult<JasperPrint> fillReport(final Map reportParameters, final Connection connection) throws ReportException {
    try {
      return new JasperReportsResult(JasperFillManager.fillReport(report, reportParameters, connection));
    }
    catch (JRException e) {
      throw new ReportException(e);
    }
  }

  public ReportResult<JasperPrint> fillReport(final Map reportParameters, final ReportDataWrapper dataWrapper) throws ReportException {
    try {
      return new JasperReportsResult(JasperFillManager.fillReport(report, reportParameters, (JRDataSource) dataWrapper.getDataSource()));
    }
    catch (JRException e) {
      throw new ReportException(e);
    }
  }

  /**
   * Loads a JasperReport file from the path given, it can be a URL or a file path
   * @param reportPath the path to the report file to load
   * @return a loaded JasperReport file
   * @throws IllegalArgumentException in case the report path is not specified
   */
  public static JasperReport loadJasperReport(final String reportPath) {
    if (reportPath == null || reportPath.isEmpty())
      throw new IllegalArgumentException();
    try {
      if (reportPath.toLowerCase().startsWith("http"))
        return (JasperReport) JRLoader.loadObject(new URL(reportPath));
      else
        return (JasperReport) JRLoader.loadObject(reportPath);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
