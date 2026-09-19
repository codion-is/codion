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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.exception;

import java.util.Locale;

/**
 * The types of errors a database implementation can recognize, each associated with a user-friendly message.
 * @see DatabaseException#errorType()
 */
public enum ErrorType {
	/**
	 * A unique or primary key constraint was violated
	 */
	UNIQUE_CONSTRAINT,
	/**
	 * A foreign key constraint was violated, for databases which do not report which way,
	 * see {@link #PARENT_MISSING} and {@link #CHILD_EXISTS}, the message then being based on the operation
	 */
	REFERENTIAL_INTEGRITY,
	/**
	 * A foreign key constraint was violated, the referenced row does not exist
	 */
	PARENT_MISSING,
	/**
	 * A foreign key constraint was violated, the row being deleted or updated is referenced
	 */
	CHILD_EXISTS,
	/**
	 * A value is missing for a column which does not allow null
	 */
	NULL_VALUE,
	/**
	 * A check constraint was violated
	 */
	CHECK_CONSTRAINT,
	/**
	 * A value is too long or too large for its column
	 */
	VALUE_TOO_LARGE,
	/**
	 * The user lacks the privileges required
	 */
	MISSING_PRIVILEGES,
	/**
	 * The login credentials are incorrect
	 */
	AUTHENTICATION,
	/**
	 * The account is locked or disabled, an authentication error
	 */
	ACCOUNT_LOCKED,
	/**
	 * The password has expired or must be changed, an authentication error
	 */
	PASSWORD_EXPIRED,
	/**
	 * A row is locked by another transaction
	 */
	ROW_LOCKED,
	/**
	 * A statement timed out
	 */
	TIMEOUT,
	/**
	 * A table or view does not exist
	 */
	TABLE_NOT_FOUND;

	String messageKey() {
		return name().toLowerCase(Locale.ROOT);
	}
}
