package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.reports.ReportException;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import java.util.Map;

final class ClassPathReportWrapper extends AbstractReportWrapper {

  private static final long serialVersionUID = 1;

  private final Class resourceClass;

  ClassPathReportWrapper(final Class resourceClass, final String reportPath, final Map<String, Object> reportParameters) {
    super(reportPath, reportParameters);
    this.resourceClass = resourceClass;
  }

  @Override
  public JasperReport loadReport() throws ReportException {
    try {
      return (JasperReport) JRLoader.loadObject(resourceClass.getResource(reportPath));
    }
    catch (final Exception e) {
      throw new ReportException("Report '" + reportPath + "' not found on classpath");
    }
  }

  @Override
  protected String getFullReportPath() {
    return resourceClass.getName() + " " + reportPath;
  }
}
