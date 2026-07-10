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
 * <p>Note that a report type identifies a report by name alone, it says nothing of the reporting
 * engine backing it. The type of the loaded report object, {@link Report}'s {@code T}, is an
 * implementation detail of the {@link Report} the report type is registered with, so a client
 * naming a report type never depends on the engine.
 * @param <P> the report parameters type
 * @param <R> the report result type
 */
public interface ReportType<P, R> {

	/**
	 * @return the report name
	 */
	String name();

	/**
	 * Instantiates a new ReportType instance with the given name.
	 * @param name the report name
	 * @param <P> the report parameters type
	 * @param <R> the report result type
	 * @return a report type
	 */
	static <P, R> ReportType<P, R> reportType(String name) {
		return new DefaultReportType<>(name);
	}
}
