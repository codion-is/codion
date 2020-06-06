/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.reports;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * A base class for wrapping reports, handles caching.
 * @param <T> the report type
 * @param <R> the report result type
 * @param <P> the report parameters type
 */
public abstract class AbstractReportWrapper<T, R, P> implements ReportWrapper<T, R, P> {

  private static final long serialVersionUID = 1;

  private static final Map<String, Object> REPORT_CACHE = new ConcurrentHashMap<>();

  protected final String reportPath;

  /**
   * Instantiates a new AbstractReportWrapper.
   * @param reportPath the report path, relative to the central report path {@link ReportWrapper#REPORT_PATH}.
   */
  public AbstractReportWrapper(final String reportPath) {
    this.reportPath = requireNonNull(reportPath, "reportPath");
  }

  @Override
  public final String toString() {
    return getFullReportPath();
  }

  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof AbstractReportWrapper && ((AbstractReportWrapper<?, ?, ?>) obj).getFullReportPath().equals(getFullReportPath());
  }

  @Override
  public final int hashCode() {
    return getFullReportPath().hashCode();
  }

  /**
   * This default implementation uses {@link ReportWrapper#getFullReportPath(String)}.
   * @return a unique path for this report
   */
  protected String getFullReportPath() {
    return ReportWrapper.getFullReportPath(reportPath);
  }

  /**
   * Returns the underlying report, either from the cache, if enabled or via {@link #loadReport()}.
   * Caches the report if report caching is enabled.
   * @return the report
   * @throws ReportException in case of an exception
   * @see ReportWrapper#CACHE_REPORTS
   */
  protected final T loadAndCacheReport() throws ReportException {
    if (CACHE_REPORTS.get()) {
      return (T) REPORT_CACHE.computeIfAbsent(getFullReportPath(), fullPath -> {
        try {
          return loadReport();
        }
        catch (final ReportException e) {
          throw new RuntimeException(e);
        }
      });
    }

    return loadReport();
  }
}
