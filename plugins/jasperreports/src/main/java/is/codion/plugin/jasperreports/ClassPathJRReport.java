/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports;

import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import static java.util.Objects.requireNonNull;

final class ClassPathJRReport extends AbstractJRReport {

  private final Class<?> resourceClass;

  ClassPathJRReport(Class<?> resourceClass, String reportPath) {
    super(reportPath, true);
    this.resourceClass = requireNonNull(resourceClass);
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
  protected String fullReportPath() {
    return resourceClass.getName() + " " + reportPath;
  }
}
