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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jasperreports;

import is.codion.common.db.report.AbstractReport;
import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

abstract class AbstractJRReport extends AbstractReport<JasperReport, JasperPrint, Map<String, Object>> implements JRReport {

  protected AbstractJRReport(String reportPath, boolean cacheReport) {
    super(reportPath, cacheReport);
  }

  @Override
  public final JasperPrint fill(Connection connection, Map<String, Object> parameters) throws ReportException {
    requireNonNull(connection, "connection");
    try {
      return JasperFillManager.fillReport(loadAndCacheReport(), parameters == null ? new HashMap<>() : parameters, connection);
    }
    catch (Exception e) {
      throw new ReportException(e);
    }
  }
}
