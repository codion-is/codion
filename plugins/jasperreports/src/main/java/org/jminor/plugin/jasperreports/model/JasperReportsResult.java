/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import org.jminor.common.db.reports.ReportResult;

import net.sf.jasperreports.engine.JasperPrint;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * A Jasper Reports result wrapper.
 */
public final class JasperReportsResult implements ReportResult<JasperPrint>, Serializable {
  private static final long serialVersionUID = 1;
  private final JasperPrint jasperPrint;

  /**
   * @param jasperPrint the print object wrapped by this report result
   */
  public JasperReportsResult(final JasperPrint jasperPrint) {
    requireNonNull(jasperPrint, "jasperPrint");
    this.jasperPrint = jasperPrint;
  }

  /** {@inheritDoc} */
  @Override
  public JasperPrint getResult() {
    return jasperPrint;
  }
}
