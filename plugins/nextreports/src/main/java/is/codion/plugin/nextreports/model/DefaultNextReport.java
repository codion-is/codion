/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.nextreports.model;

import is.codion.common.db.reports.AbstractReport;
import is.codion.common.db.reports.Report;
import is.codion.common.db.reports.ReportException;

import ro.nextreports.engine.FluentReportRunner;
import ro.nextreports.engine.querybuilder.sql.dialect.DialectFactory;
import ro.nextreports.engine.querybuilder.sql.dialect.OracleDialect;
import ro.nextreports.engine.util.ReportUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A NextReports {@link Report} implementation
 */
final class DefaultNextReport extends AbstractReport<ro.nextreports.engine.Report, NextReportsResult, Map<String, Object>> implements NextReport {

  static {
    DialectFactory.addDialect("H2", OracleDialect.class.getName());
  }

  private final String format;

  DefaultNextReport(final String reportPath, final String format) {
    super(reportPath);
    this.format = requireNonNull(format, "format");
  }

  @Override
  public NextReportsResult fillReport(final Connection connection, final Map<String, Object> parameters) throws ReportException {
    requireNonNull(connection, "connection");
    File file = null;
    try (final OutputStream output = new FileOutputStream(file = File.createTempFile("NextReportsWrapper", null, null))) {
      FluentReportRunner.report(loadReport())
              .connectTo(connection)
              .withQueryTimeout(60)
              .withParameterValues(parameters == null ? new HashMap<>() : parameters)
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
  public ro.nextreports.engine.Report loadReport() throws ReportException {
    try {
      return ReportUtil.loadReport(new FileInputStream(getFullReportPath()));
    }
    catch (final Exception e) {
      throw new ReportException(e);
    }
  }
}
