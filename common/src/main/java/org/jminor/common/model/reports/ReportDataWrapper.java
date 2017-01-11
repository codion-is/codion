/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.reports;

/**
 * A simple wrapper for a report data source
 * @param <T> the type of the datasource being wrapped.
 */
public interface ReportDataWrapper<T> {

  /**
   * @return the data source being wrapped
   */
  T getDataSource();
}
