/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.report;

import java.io.Serializable;
import java.sql.Connection;

import static java.util.Objects.requireNonNull;

final class DefaultReportType<T, R, P> implements ReportType<T, R, P>, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;

  DefaultReportType(String name) {
    this.name = requireNonNull(name);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public R fill(Report<T, R, P> report, Connection connection, P parameters) throws ReportException {
    return report.fill(connection, parameters);
  }

  @Override
  public boolean equals(Object o) {
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
