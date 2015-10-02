/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.model;

import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;

import ro.nextreports.engine.FluentReportRunner;
import ro.nextreports.engine.Report;
import ro.nextreports.engine.querybuilder.sql.dialect.DialectFactory;
import ro.nextreports.engine.querybuilder.sql.dialect.OracleDialect;
import ro.nextreports.engine.util.LoadReportException;
import ro.nextreports.engine.util.ReportUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;

public class NextReportsWrapper implements ReportWrapper<byte[], Void>, Serializable {

  static {
    DialectFactory.addDialect("H2", OracleDialect.class.getName());
  }

  private final String reportPath;
  private final Map<String, Object> reportParameters;
  private final String format;

  public NextReportsWrapper(final String reportPath, final Map<String, Object> reportParameters, final String format) {
    this.reportParameters = reportParameters;
    this.reportPath = reportPath;
    this.format = format;
  }

  @Override
  public ReportResult<byte[]> fillReport(final Connection connection) throws ReportException {
    File file = null;
    OutputStream output = null;
    try {
      file = File.createTempFile("NextReportsWrapper", null, null);
      output = new FileOutputStream(file);
      FluentReportRunner.report(loadReport(reportPath))
              .connectTo(connection)
              .withQueryTimeout(60)
              .withParameterValues(reportParameters)
              .formatAs(format)
              .run(output);
      output.close();

      final byte[] bytes = Util.getBytesFromFile(file);

      return new NextReportsResult(bytes);
    }
    catch (final Exception e) {
      throw new ReportException(e);
    }
    finally {
      Util.closeSilently(output);
      if (file != null) {
        file.delete();
      }
    }
  }

  @Override
  public String getReportName() {
    return reportPath;
  }

  @Override
  public ReportResult<byte[]> fillReport(final ReportDataWrapper<Void> dataWrapper) throws ReportException {
    throw new UnsupportedOperationException();
  }

  private Report loadReport(final String reportPath) throws FileNotFoundException, LoadReportException {
    return ReportUtil.loadReport(new FileInputStream(reportPath));
  }
}
