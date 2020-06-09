/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.common.db.reports.Report;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportWrapper;
import is.codion.common.db.reports.Reports;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.sql.Connection;
import java.util.Map;

final class DefaultJRReport implements JRReport {

  private static final long serialVersionUID = 1;

  private final Report<JasperReport, JasperPrint, Map<String, Object>> report;

  DefaultJRReport(final String name) {
    this.report = Reports.report(name);
  }

  @Override
  public String getName() {
    return report.getName();
  }

  @Override
  public JasperPrint fillReport(final Connection connection, final ReportWrapper<JasperReport, JasperPrint,
          Map<String, Object>> reportWrapper, final Map<String, Object> parameters) throws ReportException {
    return report.fillReport(connection, reportWrapper, parameters);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultJRReport)) {
      return false;
    }

    final DefaultJRReport that = (DefaultJRReport) o;

    return report.equals(that.report);
  }

  @Override
  public int hashCode() {
    return report.hashCode();
  }
}
