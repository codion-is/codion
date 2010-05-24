/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.reports;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.sql.Connection;
import java.util.Map;

/**
 * User: Björn Darri
 * Date: 23.5.2010
 * Time: 21:16:12
 */
public class JasperReportsWrapper implements ReportWrapper<JasperPrint> {
  private final JasperReport report;

  public JasperReportsWrapper(final JasperReport report) {
    this.report = report;
  }

  public ReportResult<JasperPrint> fillReport(final Map reportParameters, final Connection connection) throws ReportException {
    try {
      return new JasperReportsResult(JasperFillManager.fillReport(report, reportParameters, connection));
    }
    catch (JRException e) {
      throw new ReportException(e);
    }
  }
}
