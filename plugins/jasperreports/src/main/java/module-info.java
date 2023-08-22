/**
 * JasperReports implementation of {@link is.codion.common.db.report.Report}<br>
 * <br>
 * {@link is.codion.plugin.jasperreports.model.JasperReports}<br>
 * {@link is.codion.plugin.jasperreports.model.JRReport}<br>
 * {@link is.codion.plugin.jasperreports.model.JRReportType}<br>
 */
module is.codion.plugin.jasperreports {
  requires jasperreports;
  requires is.codion.common.db;

  exports is.codion.plugin.jasperreports.model;
}