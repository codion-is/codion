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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.report;

import java.io.Serial;

/**
 * An exception occurring during report generation.
 */
public class ReportException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new ReportException
	 * @param message the exception message
	 */
	public ReportException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new ReportException
	 * @param message the exception message
	 * @param cause the root cause
	 */
	public ReportException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new ReportException
	 * @param cause the root cause
	 */
	public ReportException(Throwable cause) {
		super(cause);
	}
}
