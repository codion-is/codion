/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import java.io.File;
import java.net.URL;
import java.util.Map;

final class FileReportWrapper extends AbstractReportWrapper {

  private static final long serialVersionUID = 1;

  private static final String SLASH = "/";

  FileReportWrapper(final String reportPath, final Map<String, Object> reportParameters) {
    super(reportPath, reportParameters);
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

  @Override
  protected String getFullReportPath() {
    final String reportLocation = ReportWrapper.getReportPath();
    final StringBuilder builder = new StringBuilder(reportLocation);
    if (!reportLocation.endsWith(SLASH) && !reportPath.startsWith(SLASH)) {
      builder.append(SLASH);
    }

    return builder.append(reportPath).toString();
  }
}
