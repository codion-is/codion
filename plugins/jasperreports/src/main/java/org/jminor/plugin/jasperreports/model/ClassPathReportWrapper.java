package org.jminor.plugin.jasperreports.model;

import net.sf.jasperreports.engine.JRException;
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
  public int hashCode() {
    return resourceClass.hashCode() + reportPath.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof ClassPathReportWrapper) {
      final ClassPathReportWrapper wrapper = (ClassPathReportWrapper) obj;

      return wrapper.resourceClass.equals(this.resourceClass) && wrapper.reportPath.equals(this.reportPath);
    }

    return false;
  }

  @Override
  protected JasperReport loadReport() throws JRException {
    try {
      return (JasperReport) JRLoader.loadObject(resourceClass.getResource(reportPath));
    }
    catch (final NullPointerException e) {
      throw new JRException("Report '" + reportPath + "' not found on classpath");
    }
  }
}
