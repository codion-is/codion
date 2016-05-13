/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.model;

import org.jminor.common.db.Database;
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
import java.nio.file.Files;
import java.sql.Connection;
import java.util.Map;

public final class NextReportsWrapper implements ReportWrapper<NextReportsResult, Void>, Serializable {

  static {
    DialectFactory.addDialect(Database.Type.H2.toString().toUpperCase(), OracleDialect.class.getName());
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
  public ReportResult<NextReportsResult> fillReport(final Connection connection) throws ReportException {
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

      final byte[] bytes = Files.readAllBytes(file.toPath());

      return new NextReportsResultWrapper(new NextReportsResult(bytes, format));
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
  public ReportResult<NextReportsResult> fillReport(final ReportDataWrapper<Void> dataWrapper) throws ReportException {
    throw new UnsupportedOperationException();
  }

  private Report loadReport(final String reportPath) throws FileNotFoundException, LoadReportException {
    return ReportUtil.loadReport(new FileInputStream(reportPath));
  }
}
