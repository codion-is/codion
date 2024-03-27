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

import is.codion.common.db.database.Database.Operation;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * An exception indication a referential integrity failure
 */
public final class ReferentialIntegrityException extends DatabaseException {

	private final Operation operation;

	/**
	 * Instantiates a new {@link ReferentialIntegrityException}
	 * @param cause the underlying cause
	 * @param message the error message
	 * @param operation the operation causing this exception
	 */
	public ReferentialIntegrityException(SQLException cause, String message, Operation operation) {
		super(cause, message);
		this.operation = requireNonNull(operation);
	}

	/**
	 * @return the {@link Operation} causing this exception
	 */
	public Operation operation() {
		return operation;
	}
}
