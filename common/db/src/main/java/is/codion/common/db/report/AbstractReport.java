/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.report;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * A base class for wrapping reports, handles caching.
 * @param <T> the report type
 * @param <R> the report result type
 * @param <P> the report parameters type
 */
public abstract class AbstractReport<T, R, P> implements Report<T, R, P> {

  private static final Map<String, Object> REPORT_CACHE = new ConcurrentHashMap<>();

  protected final String reportPath;

  /**
   * Instantiates a new AbstractReport.
   * @param reportPath the report path, relative to the central report path {@link Report#REPORT_PATH}.
   */
  public AbstractReport(String reportPath) {
    this.reportPath = requireNonNull(reportPath, "reportPath");
  }

  @Override
  public final String toString() {
    return getFullReportPath();
  }

  @Override
  public final boolean equals(Object obj) {
    return obj instanceof AbstractReport && ((AbstractReport<?, ?, ?>) obj).getFullReportPath().equals(getFullReportPath());
  }

  @Override
  public final int hashCode() {
    return getFullReportPath().hashCode();
  }

  /**
   * This default implementation uses {@link Report#getFullReportPath(String)}.
   * @return a unique path for this report
   */
  protected String getFullReportPath() {
    return Report.getFullReportPath(reportPath);
  }

  /**
   * Returns the underlying report, either from the cache, if enabled or via {@link #loadReport()}.
   * Caches the report if report caching is enabled.
   * @return the report
   * @throws ReportException in case of an exception
   * @see Report#CACHE_REPORTS
   */
  protected final T loadAndCacheReport() throws ReportException {
    if (CACHE_REPORTS.get()) {
      return (T) REPORT_CACHE.computeIfAbsent(getFullReportPath(), fullPath -> {
        try {
          return loadReport();
        }
        catch (ReportException e) {
          throw new RuntimeException(e);
        }
      });
    }

    return loadReport();
  }
}
