/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;

final class DefaultJRReportType implements JRReportType, Serializable {

  private static final long serialVersionUID = 1;

  private final ReportType<JasperReport, JasperPrint, Map<String, Object>> reportType;

  DefaultJRReportType(String name) {
    this.reportType = ReportType.reportType(name);
  }

  @Override
  public String getName() {
    return reportType.getName();
  }

  @Override
  public JasperPrint fillReport(Connection connection, Report<JasperReport, JasperPrint,
            Map<String, Object>> report, Map<String, Object> parameters) throws ReportException {
    return reportType.fillReport(connection, report, parameters);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultJRReportType)) {
      return false;
    }

    DefaultJRReportType that = (DefaultJRReportType) o;

    return reportType.equals(that.reportType);
  }

  @Override
  public int hashCode() {
    return reportType.hashCode();
  }

  @Override
  public String toString() {
    return reportType.toString();
  }
}
