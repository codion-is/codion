package org.jminor.framework.client.model.reporting;

import org.jminor.framework.db.EntityDb;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import java.net.MalformedURLException;
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
   * Takes a path to a JasperReport which uses a JDBC datasource and returns an initialized JasperPrint object
   * @param entityDb the EntityDb instance to use when filling the report
   * @param reportPath the path to the report to fill
   * @param reportParameters the report parameters
   * @return an initialized JasperPrint object
   * @throws JRException in case of a report exception
   */
  public static JasperPrint fillJdbcReport(final EntityDb entityDb, final String reportPath,
                                           final Map reportParameters) throws JRException {
    try {
      return entityDb.fillReport(loadJasperReport(reportPath), reportParameters);
    }
    catch (JRException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Takes a path to a JasperReport file and returns an initialized JasperPrint object
   * @param reportPath the path to the report to fill
   * @param reportParameters the report parameters
   * @param dataSource the JRDataSource to use
   * @return an initialized JasperPrint object
   * @throws JRException in case of a report exception
   */
  public static JasperPrint fillReport(final String reportPath, final Map reportParameters,
                                       final JRDataSource dataSource) throws JRException {
    return JasperFillManager.fillReport(loadJasperReport(reportPath), reportParameters, dataSource);
  }

  /**
   * Loads a JasperReport file from the path given, it can be a URL or a file path
   * @param reportPath the path to the report file to load
   * @return a loaded JasperReport file
   * @throws JRException in case of an exception
   */
  public static JasperReport loadJasperReport(final String reportPath) throws JRException {
    try {
      if (reportPath.toLowerCase().startsWith("http"))
        return (JasperReport) JRLoader.loadObject(new URL(reportPath));
      else
        return (JasperReport) JRLoader.loadObject(reportPath);
    }
    catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
