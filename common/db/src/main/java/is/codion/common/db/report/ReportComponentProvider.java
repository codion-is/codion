/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.report;

/**
 * A report presentation UI provider.
 * @param <R> the type of the report result
 * @param <C> the component type
 */
public interface ReportComponentProvider<R, C> {

  /**
   * Returns a component showing a report based on the given report result.
   * @param result the report result to base the report on.
   * @return a component showing a report.
   */
  C createReportComponent(R result);
}
