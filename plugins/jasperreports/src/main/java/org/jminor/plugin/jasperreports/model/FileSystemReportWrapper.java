/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.reports.ReportWrapper;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import java.io.File;
import java.util.Map;

final class FileSystemReportWrapper extends AbstractReportWrapper {

  private static final long serialVersionUID = 1;

  FileSystemReportWrapper(final String reportPath, final Map<String, Object> reportParameters) {
    super(reportPath, reportParameters);
  }

  @Override
  public int hashCode() {
    return getFullReportPath().hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof FileSystemReportWrapper && ((FileSystemReportWrapper) obj).getFullReportPath().equals(getFullReportPath());
  }

  @Override
  public String toString() {
    return getFullReportPath();
  }

  @Override
  protected JasperReport loadReport() throws JRException {
    final File reportFile = new File(getFullReportPath());
    if (reportFile.exists()) {
      return (JasperReport) JRLoader.loadObject(reportFile);
    }

    throw new JRException("Report '" + reportFile + "' not found in filesystem");
  }

  private String getFullReportPath() {
    String reportDirectory = ReportWrapper.getReportPath();
    if (!reportDirectory.endsWith("/") && !reportPath.startsWith("/")) {
      reportDirectory = reportDirectory + "/";
    }

    return reportDirectory + reportPath;
  }
}
