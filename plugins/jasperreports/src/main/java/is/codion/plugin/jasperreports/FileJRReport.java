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

import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import java.io.File;
import java.net.URL;

final class FileJRReport extends AbstractJRReport {

  FileJRReport(String reportPath, boolean cacheReport) {
    super(reportPath, cacheReport);
  }

  @Override
  public JasperReport loadReport() throws ReportException {
    String fullReportPath = fullReportPath();
    try {
      if (fullReportPath.toLowerCase().startsWith("http")) {
        return (JasperReport) JRLoader.loadObject(new URL(fullReportPath));
      }
      File reportFile = new File(fullReportPath);
      if (!reportFile.exists()) {
        throw new ReportException("Report '" + reportFile + "' not found in filesystem");
      }

      return (JasperReport) JRLoader.loadObject(reportFile);
    }
    catch (Exception e) {
      throw new ReportException(e);
    }
  }
}
