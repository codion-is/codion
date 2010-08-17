/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.reports;

/**
 * A simple wrapper for a report result
 * @param <R> the type of the report result being wrapped.
 */
public interface ReportResult<R> {

  /**
   * @return a populated report result, ready for display
   */
  R getResult();
}
