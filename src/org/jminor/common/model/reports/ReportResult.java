/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.reports;

/**
 * User: Björn Darri
 * Date: 23.5.2010
 * Time: 21:09:30
 */
public interface ReportResult<R> {

  /**
   * @return a populated report, ready for display
   */
  R getResult();
}
