/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports;

import is.codion.common.db.report.ReportType;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.io.Serializable;
import java.util.Map;

final class DefaultJRReportType implements JRReportType, Serializable {

  private static final long serialVersionUID = 1;

  private final ReportType<JasperReport, JasperPrint, Map<String, Object>> reportType;

  DefaultJRReportType(String name) {
    this.reportType = ReportType.reportType(name);
  }

  @Override
  public String name() {
    return reportType.name();
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
