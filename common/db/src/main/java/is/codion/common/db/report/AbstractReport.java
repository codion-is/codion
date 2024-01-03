/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.report;

import static java.util.Objects.requireNonNull;

/**
 * A base class for wrapping reports, handles caching.
 * @param <T> the report type
 * @param <R> the report result type
 * @param <P> the report parameters type
 */
public abstract class AbstractReport<T, R, P> implements Report<T, R, P> {

  protected final String reportPath;

  private final boolean cacheReport;

  private T cachedReport;

  /**
   * Instantiates a new AbstractReport.
   * @param reportPath the report path, relative to the central report path {@link Report#REPORT_PATH}.
   * @param cacheReport true if the report should be cached when loaded
   */
  protected AbstractReport(String reportPath, boolean cacheReport) {
    this.reportPath = requireNonNull(reportPath, "reportPath");
    this.cacheReport = cacheReport;
  }

  @Override
  public final String toString() {
    return fullReportPath();
  }

  @Override
  public final boolean equals(Object obj) {
    return obj instanceof AbstractReport && ((AbstractReport<?, ?, ?>) obj).fullReportPath().equals(fullReportPath());
  }

  @Override
  public final int hashCode() {
    return fullReportPath().hashCode();
  }

  @Override
  public final synchronized boolean cached() {
    return cachedReport != null;
  }

  @Override
  public final synchronized void clearCache() {
    cachedReport = null;
  }

  /**
   * This default implementation uses {@link Report#fullReportPath(String)}.
   * @return a unique path for this report
   */
  protected String fullReportPath() {
    return Report.fullReportPath(reportPath);
  }

  /**
   * Returns the underlying report, either from the cache, if enabled or via {@link #load()}.
   * @return the report
   * @throws ReportException in case of an exception
   * @see Report#CACHE_REPORTS
   */
  protected final T loadAndCacheReport() throws ReportException {
    if (cacheReport) {
      return cachedReport();
    }

    return load();
  }

  private synchronized T cachedReport() {
    if (cachedReport == null) {
      try {
        cachedReport = load();
      }
      catch (ReportException e) {
        throw new RuntimeException(e);
      }
    }

    return cachedReport;
  }
}
