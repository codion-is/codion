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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.exception;

import org.jspecify.annotations.Nullable;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * An exception indicating a referential integrity failure
 */
public final class ReferentialIntegrityException extends DatabaseException {

	private final Operation operation;

	/**
	 * Instantiates a new {@link ReferentialIntegrityException}
	 * @param cause the underlying cause
	 * @param message the error message
	 * @param operation the operation causing this exception
	 */
	public ReferentialIntegrityException(SQLException cause, @Nullable String message, Operation operation) {
		super(cause, message);
		this.operation = requireNonNull(operation);
	}

	/**
	 * Instantiates a new {@link ReferentialIntegrityException}, for a client reconstructing
	 * this exception from a message, having no {@link SQLException} to hand.
	 * @param message the error message
	 * @param operation the operation causing this exception
	 */
	public ReferentialIntegrityException(@Nullable String message, Operation operation) {
		super(message);
		this.operation = requireNonNull(operation);
	}

	/**
	 * Instantiates a new {@link ReferentialIntegrityException}, its message the one associated with the error type.
	 * For {@link ErrorType#REFERENTIAL_INTEGRITY}, which does not specify which way the constraint
	 * was violated, the message is based on the operation.
	 * @param cause the underlying cause, if any
	 * @param errorType the error type
	 * @param detail the detail, null if none
	 * @param operation the operation causing this exception
	 */
	public ReferentialIntegrityException(@Nullable SQLException cause, ErrorType errorType, @Nullable String detail, Operation operation) {
		super(cause, errorType, detail, messageKey(requireNonNull(errorType), requireNonNull(operation)));
		this.operation = operation;
	}

	/**
	 * @return the {@link Operation} causing this exception
	 */
	public Operation operation() {
		return operation;
	}

	@Override
	String messageKey() {
		return messageKey(errorType().orElseThrow(), operation);
	}

	private static String messageKey(ErrorType errorType, Operation operation) {
		if (errorType == ErrorType.REFERENTIAL_INTEGRITY) {
			// the database does not report which way, the operation does in all cases but an update
			switch (operation) {
				case INSERT:
					return ErrorType.PARENT_MISSING.messageKey();
				case DELETE:
					return ErrorType.CHILD_EXISTS.messageKey();
				default:
					break;
			}
		}

		return errorType.messageKey();
	}
}
