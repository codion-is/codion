package is.codion.plugin.jasperreports.model;

import is.codion.common.db.reports.ReportException;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

final class ClassPathJasperReportWrapper extends AbstractJasperReportWrapper {

  private static final long serialVersionUID = 1;

  private final Class resourceClass;

  ClassPathJasperReportWrapper(final Class resourceClass, final String reportPath) {
    super(reportPath);
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
