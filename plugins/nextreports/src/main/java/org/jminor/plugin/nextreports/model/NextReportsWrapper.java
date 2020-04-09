/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.nextreports.model;

import org.jminor.common.db.Database;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportWrapper;

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

import static java.util.Objects.requireNonNull;

/**
 * A NextReports {@link ReportWrapper} implementation
 */
public final class NextReportsWrapper implements ReportWrapper<NextReportsResult, Void>, Serializable {

  private static final long serialVersionUID = 1;

  static {
    DialectFactory.addDialect(Database.Type.H2.toString().toUpperCase(), OracleDialect.class.getName());
  }

  private final String reportPath;
  private final Map<String, Object> reportParameters;
  private final String format;

  /**
   * Instantiates a new {@link NextReportsWrapper}.
   * @param reportPath the path to the report
   * @param reportParameters the report parameters
   * @param format the format
   */
  public NextReportsWrapper(final String reportPath, final Map<String, Object> reportParameters, final String format) {
    this.reportPath = requireNonNull(reportPath, "reportPath");
    this.reportParameters = requireNonNull(reportParameters, "reportParameters");
    this.format = format;
  }

  @Override
  public NextReportsResult fillReport(final Connection connection) throws ReportException {
    File file = null;
    try (final OutputStream output = new FileOutputStream(file = File.createTempFile("NextReportsWrapper", null, null))) {
      FluentReportRunner.report(loadReport(reportPath))
              .connectTo(connection)
              .withQueryTimeout(60)
              .withParameterValues(reportParameters)
              .formatAs(format)
              .run(output);
      output.close();

      final byte[] bytes = Files.readAllBytes(file.toPath());

      return new NextReportsResult(bytes, format);
    }
    catch (final Exception e) {
      throw new ReportException(e);
    }
    finally {
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
  public NextReportsResult fillReport(final Void dataWrapper) throws ReportException {
    throw new UnsupportedOperationException();
  }

  private static Report loadReport(final String reportPath) throws FileNotFoundException, LoadReportException {
    return ReportUtil.loadReport(new FileInputStream(reportPath));
  }
}
