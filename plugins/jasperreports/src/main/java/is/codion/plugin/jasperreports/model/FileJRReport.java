/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import java.io.File;
import java.net.URL;

final class FileJRReport extends AbstractJRReport {

  FileJRReport(final String reportPath) {
    super(reportPath);
  }

  @Override
  public JasperReport loadReport() throws ReportException {
    final String fullReportPath = getFullReportPath();
    try {
      if (fullReportPath.toLowerCase().startsWith("http")) {
        return (JasperReport) JRLoader.loadObject(new URL(fullReportPath));
      }
      final File reportFile = new File(fullReportPath);
      if (!reportFile.exists()) {
        throw new ReportException("Report '" + reportFile + "' not found in filesystem");
      }

      return (JasperReport) JRLoader.loadObject(reportFile);
    }
    catch (final Exception e) {
      throw new ReportException(e);
    }
  }
}
