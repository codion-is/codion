/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.reports.ReportDataWrapper;

import net.sf.jasperreports.engine.JRDataSource;

import static java.util.Objects.requireNonNull;

/**
 * A Jasper Reports data wrapper.
 */
public final class JasperReportsDataWrapper implements ReportDataWrapper<JRDataSource> {

  private final JRDataSource dataSource;

  /**
   * @param dataSource the underlying datasource
   */
  public JasperReportsDataWrapper(final JRDataSource dataSource) {
    requireNonNull(dataSource, "dataSource");
    this.dataSource = dataSource;
  }

  /** {@inheritDoc} */
  @Override
  public JRDataSource getDataSource() {
    return dataSource;
  }
}
