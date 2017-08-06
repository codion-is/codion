/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.db.reports.ReportDataWrapper;

import net.sf.jasperreports.engine.JRDataSource;

import java.util.Objects;

/**
 * A Jasper Reports data wrapper.
 */
public final class JasperReportsDataWrapper implements ReportDataWrapper<JRDataSource> {

  private final JRDataSource dataSource;

  /**
   * @param dataSource the underlying datasource
   */
  public JasperReportsDataWrapper(final JRDataSource dataSource) {
    Objects.requireNonNull(dataSource, "dataSource");
    this.dataSource = dataSource;
  }

  /** {@inheritDoc} */
  @Override
  public JRDataSource getDataSource() {
    return dataSource;
  }
}
