/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

final class ClassPathJRReport extends AbstractJRReport {

  private final Class<?> resourceClass;

  ClassPathJRReport(Class<?> resourceClass, String reportPath) {
    super(reportPath);
    this.resourceClass = resourceClass;
  }

  @Override
  public JasperReport loadReport() throws ReportException {
    try {
      return (JasperReport) JRLoader.loadObject(resourceClass.getResource(reportPath));
    }
    catch (Exception e) {
      throw new ReportException("Unable to load report '" + reportPath + "' from classpath", e);
    }
  }

  @Override
  protected String getFullReportPath() {
    return resourceClass.getName() + " " + reportPath;
  }
}
