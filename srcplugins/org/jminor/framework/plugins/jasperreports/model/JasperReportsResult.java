/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportResult;

import net.sf.jasperreports.engine.JasperPrint;

import java.io.Serializable;

/**
 * User: Bjorn Darri<br>
 * Date: 23.5.2010<br>
 * Time: 21:20:16
 */
public class JasperReportsResult implements ReportResult<JasperPrint>, Serializable {
  private static final long serialVersionUID = 1;
  private final JasperPrint jasperPrint;

  public JasperReportsResult(final JasperPrint jasperPrint) {
    Util.rejectNullValue(jasperPrint, "jasperPrint");
    this.jasperPrint = jasperPrint;
  }

  public JasperPrint getResult() {
    return jasperPrint;
  }
}
