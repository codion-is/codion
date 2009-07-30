package org.jminor.framework.client.model.reporting;

import org.jminor.framework.db.IEntityDb;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import java.net.URL;
import java.util.Map;

/**
 * User: Björn Darri
 * Date: 30.7.2009
 * Time: 17:58:09
 */
public class EntityReportUtil {

  private EntityReportUtil() {}

  /**
   * Takes a JasperReport object which uses a JDBC datasource and returns an initialized JasperPrint object
   * @param entityDb the IEntityDb instance to use when filling the report
   * @param reportPath the path to the report to fill
   * @param reportParameters the report parameters
   * @return an initialized JasperPrint object
   * @throws net.sf.jasperreports.engine.JRException in case of a report exception
   * @throws Exception in case of exception
   */
  public static JasperPrint fillJdbcReport(final IEntityDb entityDb, final String reportPath,
                                           final Map reportParameters) throws Exception {
    return entityDb.fillReport(loadJasperReport(reportPath), reportParameters);
  }

  public static JasperReport loadJasperReport(final String reportPath) throws Exception {
    if (reportPath.toUpperCase().startsWith("HTTP"))
      return (JasperReport) JRLoader.loadObject(new URL(reportPath));
    else
      return (JasperReport) JRLoader.loadObject(reportPath);
  }
}
