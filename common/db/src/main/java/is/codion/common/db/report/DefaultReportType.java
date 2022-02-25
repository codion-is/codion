/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.report;

import java.io.Serializable;
import java.sql.Connection;

import static java.util.Objects.requireNonNull;

final class DefaultReportType<T, R, P> implements ReportType<T, R, P>, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;

  DefaultReportType(final String name) {
    this.name = requireNonNull(name);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public R fillReport(final Connection connection, final Report<T, R, P> report, final P parameters) throws ReportException {
    return report.fillReport(connection, parameters);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultReportType)) {
      return false;
    }

    DefaultReportType<?, ?, ?> that = (DefaultReportType<?, ?, ?>) o;

    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }
}
