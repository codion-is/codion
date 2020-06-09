/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.reports;

import java.sql.Connection;

import static java.util.Objects.requireNonNull;

class DefaultReport<T, R, P> implements Report<T, R, P> {

  private static final long serialVersionUID = 1;

  private final String name;

  DefaultReport(final String name) {
    this.name = requireNonNull(name);
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public R fillReport(final Connection connection, final ReportWrapper<T, R, P> reportWrapper, final P parameters) throws ReportException {
    return reportWrapper.fillReport(connection, parameters);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultReport)) {
      return false;
    }

    final DefaultReport<?, ?, ?> that = (DefaultReport<?, ?, ?>) o;

    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
