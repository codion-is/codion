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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.report;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

import java.sql.Connection;
import java.util.Objects;

/**
 * A wrapper for a report
 * @param <T> the report type
 * @param <R> the report result type
 * @param <P> the report parameters type
 */
public interface Report<T, R, P> {

	/**
	 * The report path used for file based report generation.
	 */
	PropertyValue<String> REPORT_PATH = Configuration.stringValue("codion.report.path");

	/**
	 * Specifies whether to cache reports when loaded from disk/network, this prevents "hot deploy" of reports.<br>
	 * Value type: Boolean<br>
	 * Default value: true
	 */
	PropertyValue<Boolean> CACHE_REPORTS = Configuration.booleanValue("codion.report.cacheReports", true);

	/**
	 * Loads and fills the report using the given database connection
	 * @param connection the connection to use for the report generation
	 * @param parameters the report parameters, if any
	 * @return a filled report ready for display
	 * @throws ReportException in case of an exception
	 */
	R fill(Connection connection, P parameters) throws ReportException;

	/**
	 * Loads the report this report wrapper is based on.
	 * @return a loaded report object
	 * @throws ReportException in case of an exception
	 */
	T load() throws ReportException;

	/**
	 * @return true if this report has been cached
	 */
	boolean cached();

	/**
	 * Clears the report cache, if caching is not enabled calling this method has no effect
	 */
	void clearCache();

	/**
	 * @return the value associated with {@link Report#REPORT_PATH}
	 * @throws IllegalStateException in case it is not specified
	 */
	static String reportPath() {
		return REPORT_PATH.getOrThrow();
	}

	/**
	 * Returns a full report path, combined from the report location specified by {@link #REPORT_PATH}
	 * and the given report path.
	 * @param reportPath the report path relative to {@link Report#REPORT_PATH}.
	 * @return a full report path
	 * @throws IllegalStateException in case {@link Report#REPORT_PATH} is not specified
	 */
	static String fullReportPath(String reportPath) {
		Objects.requireNonNull(reportPath);
		String slash = "/";
		String reportLocation = reportPath();
		StringBuilder builder = new StringBuilder(reportLocation);
		if (!reportLocation.endsWith(slash) && !reportPath.startsWith(slash)) {
			builder.append(slash);
		}

		return builder.append(reportPath).toString();
	}
}
