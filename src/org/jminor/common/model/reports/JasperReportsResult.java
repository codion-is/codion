/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.reports;

import net.sf.jasperreports.engine.JasperPrint;

/**
 * User: Björn Darri
 * Date: 23.5.2010
 * Time: 21:20:16
 */
public class JasperReportsResult implements ReportResult<JasperPrint> {
  private final JasperPrint jasperPrint;

  public JasperReportsResult(final JasperPrint jasperPrint) {
    this.jasperPrint = jasperPrint;
  }

  public JasperPrint getResult() throws ReportException {
    return jasperPrint;
  }
}
