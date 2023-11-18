/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.db.report;

import java.sql.Connection;

/**
 * Identifies a report.
 * A factory for {@link ReportType} instances.
 * @param <T> the report type
 * @param <R> the report result type
 * @param <P> the report parameters type
 */
public interface ReportType<T, R, P> {

  /**
   * @return the report name
   */
  String name();

  /**
   * Fills the given report.
   * @param report the report to fill
   * @param connection the connection to use
   * @param parameters the report parameters
   * @return a report result
   * @throws ReportException in case of an exception
   */
  R fill(Report<T, R, P> report, Connection connection, P parameters) throws ReportException;

  /**
   * Instantiates a new Report instance with the given name.
   * @param name the report name
   * @param <T> the report type
   * @param <R> the report result type
   * @param <P> the report parameters type
   * @return a report
   */
  static <T, R, P> ReportType<T, R, P> reportType(String name) {
    return new DefaultReportType<>(name);
  }
}
