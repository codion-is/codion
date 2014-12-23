/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportResult;

import net.sf.jasperreports.engine.JasperPrint;

import java.io.Serializable;

/**
 * A Jasper Reports result wrapper.
 */
public final class JasperReportsResult implements ReportResult<JasperPrint>, Serializable {
  private static final long serialVersionUID = 1;
  private final JasperPrint jasperPrint;

  public JasperReportsResult(final JasperPrint jasperPrint) {
    Util.rejectNullValue(jasperPrint, "jasperPrint");
    this.jasperPrint = jasperPrint;
  }

  /** {@inheritDoc} */
  @Override
  public JasperPrint getResult() {
    return jasperPrint;
  }
}
