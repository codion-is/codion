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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.report;

/**
 * Identifies a report.
 * A factory for {@link ReportType} instances.
 * @param <T> the report type
 * @param <P> the report parameters type
 * @param <R> the report result type
 */
public interface ReportType<T, P, R> {

	/**
	 * @return the report name
	 */
	String name();

	/**
	 * Instantiates a new Report instance with the given name.
	 * @param name the report name
	 * @param <T> the report type
	 * @param <P> the report parameters type
	 * @param <R> the report result type
	 * @return a report
	 */
	static <T, P, R> ReportType<T, P, R> reportType(String name) {
		return new DefaultReportType<>(name);
	}
}
