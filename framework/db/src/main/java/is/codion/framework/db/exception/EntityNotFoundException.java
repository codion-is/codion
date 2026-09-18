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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.exception;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.resource.MessageBundle;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.util.ResourceBundle.getBundle;

/**
 * Exception used when an expected entity was not found.
 */
public class EntityNotFoundException extends DatabaseException {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityNotFoundException.class, getBundle(EntityNotFoundException.class.getName()));

	/**
	 * Instantiates a new EntityNotFoundException with a default message
	 */
	public EntityNotFoundException() {
		this(MESSAGES.getString("record_not_found"));
	}

	/**
	 * Instantiates a new EntityNotFoundException
	 * @param message the exception message
	 */
	public EntityNotFoundException(String message) {
		super(message);
	}
}
