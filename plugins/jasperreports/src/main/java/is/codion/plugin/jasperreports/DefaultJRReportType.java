/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jasperreports;

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
  public String name() {
    return reportType.name();
  }

  @Override
  public JasperPrint fill(Report<JasperReport, JasperPrint, Map<String, Object>> report,
                          Connection connection, Map<String, Object> parameters) throws ReportException {
    return reportType.fill(report, connection, parameters);
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
