/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportDataWrapper;

import net.sf.jasperreports.engine.JRDataSource;

/**
 * A Jasper Reports data wrapper.
 */
public final class JasperReportsDataWrapper implements ReportDataWrapper<JRDataSource> {

  private final JRDataSource dataSource;

  public JasperReportsDataWrapper(final JRDataSource dataSource) {
    Util.rejectNullValue(dataSource, "dataSource");
    this.dataSource = dataSource;
  }

  /** {@inheritDoc} */
  @Override
  public JRDataSource getDataSource() {
    return dataSource;
  }
}
