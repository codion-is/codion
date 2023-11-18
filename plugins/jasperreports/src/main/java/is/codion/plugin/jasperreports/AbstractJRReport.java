/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports;

import is.codion.common.db.report.AbstractReport;
import is.codion.common.db.report.ReportException;

import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

abstract class AbstractJRReport extends AbstractReport<JasperReport, JasperPrint, Map<String, Object>> implements JRReport {

  protected AbstractJRReport(String reportPath, boolean cacheReport) {
    super(reportPath, cacheReport);
  }

  @Override
  public final JasperPrint fill(Connection connection, Map<String, Object> parameters) throws ReportException {
    requireNonNull(connection, "connection");
    try {
      return JasperFillManager.fillReport(loadAndCacheReport(), parameters == null ? new HashMap<>() : parameters, connection);
    }
    catch (Exception e) {
      throw new ReportException(e);
    }
  }
}
