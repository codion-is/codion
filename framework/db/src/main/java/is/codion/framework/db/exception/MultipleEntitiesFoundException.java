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

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.Locale;

/**
 * Exception used when one entity was expected but many were found.
 * <p>The default message is put together when read, in the language of the reader,
 * see {@link #getMessage()} and {@link #message(Locale)}.
 */
public class MultipleEntitiesFoundException extends DatabaseException {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final String MESSAGE_KEY = "multiple_records_found";

	private final boolean defaultMessage;

	/**
	 * Instantiates a new MultipleEntitiesFoundException with a default message
	 */
	public MultipleEntitiesFoundException() {
		// the message in the language of the one throwing, for a reader not knowing the default message flag
		super(Messages.message(MultipleEntitiesFoundException.class, MESSAGE_KEY, Locale.getDefault()));
		this.defaultMessage = true;
	}

	/**
	 * Instantiates a new MultipleEntitiesFoundException
	 * @param message the exception message
	 */
	public MultipleEntitiesFoundException(String message) {
		super(message);
		this.defaultMessage = false;
	}

	@Override
	public @Nullable String message(Locale locale) {
		if (defaultMessage) {
			try {
				return Messages.message(MultipleEntitiesFoundException.class, MESSAGE_KEY, locale);
			}
			catch (RuntimeException e) {
				// reading a message must never throw
			}
		}

		return super.message(locale);
	}
}
