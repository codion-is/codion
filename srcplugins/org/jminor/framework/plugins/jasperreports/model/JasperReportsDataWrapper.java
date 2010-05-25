/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.model.reports.ReportDataWrapper;

import net.sf.jasperreports.engine.JRDataSource;

public class JasperReportsDataWrapper implements ReportDataWrapper<JRDataSource> {

  private final JRDataSource dataSource;

  public JasperReportsDataWrapper(final JRDataSource dataSource) {
    this.dataSource = dataSource;
  }

  public JRDataSource getDataSource() {
    return dataSource;
  }
}
