/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.model;

import org.jminor.common.model.reports.ReportResult;

import java.io.Serializable;

public final class NextReportsResultWrapper implements ReportResult<NextReportsResult>, Serializable {

  private final NextReportsResult result;

  public NextReportsResultWrapper(final NextReportsResult result) {
    this.result = result;
  }

  @Override
  public NextReportsResult getResult() {
    return result;
  }
}
