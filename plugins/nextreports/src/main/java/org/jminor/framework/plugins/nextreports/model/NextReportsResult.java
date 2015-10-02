/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.model;

import org.jminor.common.model.reports.ReportResult;

import java.io.Serializable;

public class NextReportsResult implements ReportResult<byte[]>, Serializable {

  private final byte[] bytes;

  public NextReportsResult(final byte[] bytes) {
    this.bytes = bytes;
  }

  @Override
  public byte[] getResult() {
    return bytes;
  }
}
