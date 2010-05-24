/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.reports;

import java.sql.Connection;
import java.util.Map;

/**
 * User: Björn Darri
 * Date: 23.5.2010
 * Time: 21:09:45
 */
public interface ReportWrapper<R> {
  ReportResult<R> fillReport(final Map reportParameters, final Connection connection) throws ReportException;
}
