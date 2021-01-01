/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports.model;

import is.codion.common.db.reports.AbstractReport;
import is.codion.common.db.reports.ReportException;

import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

abstract class AbstractJRReport extends AbstractReport<JasperReport, JasperPrint, Map<String, Object>> implements JRReport {

  protected AbstractJRReport(final String reportPath) {
    super(reportPath);
  }

  @Override
  public final JasperPrint fillReport(final Connection connection, final Map<String, Object> parameters) throws ReportException {
    requireNonNull(connection, "connection");
    try {
      return JasperFillManager.fillReport(loadAndCacheReport(), parameters == null ? new HashMap<>() : parameters, connection);
    }
    catch (final Exception e) {
      throw new ReportException(e);
    }
  }
}
