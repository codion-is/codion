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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.exception;

import java.sql.SQLException;

/**
 * Exception thrown when a statement has timed out or been cancelled.
 */
public final class QueryTimeoutException extends DatabaseException {

	/**
	 * Instantiates a new {@link QueryTimeoutException}
	 * @param cause the underlying cause
	 * @param message the error message
	 */
	public QueryTimeoutException(SQLException cause, String message) {
		super(cause, message);
	}
}
