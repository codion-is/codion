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
package is.codion.common.db.exception;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * An exception coming from a database-layer.
 * <p>Note that the underlying {@link SQLException} is not preserved as the cause ({@link #getCause()}
 * returns null); instead its stack trace is copied and its error code and sql state captured. This keeps
 * the exception deserializable on a client without the database driver classes on its classpath.
 * <p>An exception based on an {@link ErrorType} carries the error type and the detail, if any, instead of a message,
 * the message being put together when read, see {@link #getMessage()} and {@link #message(Locale)}. The message
 * thereby follows the language of the one reading it, that of the client, not the server the exception came from.
 */
public class DatabaseException extends RuntimeException {

	/**
	 * SQLException state indicating that a query did not return a result
	 */
	public static final String SQL_STATE_NO_DATA = "02000";

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * The underlying error code, if any, transient so that it's not
	 * available client side if running in a server/client environment
	 */
	private transient int errorCode;

	/**
	 * The underlying sql state, if any, transient so that it's not
	 * available client side if running in a server/client environment
	 */
	private final transient @Nullable String sqlState;

	/**
	 * The name of the error type, not the error type itself, an error type
	 * unknown to the one deserializing must not prevent the deserialization
	 */
	private final @Nullable String errorType;

	/**
	 * The error detail, if any, the name of a constraint, column or table for example
	 */
	private final @Nullable String detail;

	/**
	 * Constructs a new DatabaseException instance
	 * @param message the exception message
	 */
	public DatabaseException(@Nullable String message) {
		super(message);
		this.errorCode = -1;
		this.sqlState = null;
		this.errorType = null;
		this.detail = null;
	}

	/**
	 * Constructs a new DatabaseException instance
	 * @param cause the root cause, the stack trace is copied and used
	 */
	public DatabaseException(SQLException cause) {
		this(cause, cause.getMessage());
	}

	/**
	 * Constructs a new DatabaseException instance
	 * @param cause the root cause, the stack trace is copied and used
	 * @param message the exception message
	 */
	public DatabaseException(SQLException cause, @Nullable String message) {
		this(cause, message, null, null);
	}

	/**
	 * Constructs a new DatabaseException instance, based on an error type, its message the one
	 * associated with the error type, along with the detail, if any.
	 * @param errorType the error type
	 * @param detail the detail, a constraint, column or table name for example, null if none
	 * @see #getMessage()
	 */
	public DatabaseException(ErrorType errorType, @Nullable String detail) {
		this(null, errorType, detail);
	}

	/**
	 * Constructs a new DatabaseException instance, based on an error type, its message the one
	 * associated with the error type, along with the detail, if any.
	 * @param cause the root cause, the stack trace is copied and used
	 * @param errorType the error type
	 * @param detail the detail, a constraint, column or table name for example, null if none
	 * @see #getMessage()
	 */
	public DatabaseException(@Nullable SQLException cause, ErrorType errorType, @Nullable String detail) {
		this(cause, errorType, detail, requireNonNull(errorType).messageKey());
	}

	/**
	 * @param cause the root cause, if any
	 * @param errorType the error type
	 * @param detail the detail, if any
	 * @param messageKey the message key, the one of the error type, unless the message depends on more than that
	 */
	DatabaseException(@Nullable SQLException cause, ErrorType errorType, @Nullable String detail, String messageKey) {
		// the message in the language of the one throwing, for a reader
		// not knowing the error type, an older client for example
		this(cause, message(messageKey, detail, Locale.getDefault()), requireNonNull(errorType), detail);
	}

	private DatabaseException(@Nullable SQLException cause, @Nullable String message,
														@Nullable ErrorType errorType, @Nullable String detail) {
		super(message);
		this.errorType = errorType == null ? null : errorType.name();
		this.detail = detail;
		if (cause != null) {
			errorCode = cause.getErrorCode();
			sqlState = cause.getSQLState();
			setStackTrace(cause.getStackTrace());
		}
		else {
			errorCode = -1;
			sqlState = null;
		}
	}

	/**
	 * Returns the error type this exception is based on, if any. Note that an exception coming from a newer
	 * server may be based on an error type not known to the client, in which case this is empty, the message
	 * then being the one put together by the server.
	 * @return the error type, an empty Optional if this exception is not based on a known error type
	 */
	public final Optional<ErrorType> errorType() {
		if (errorType == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(ErrorType.valueOf(errorType));
		}
		catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/**
	 * Returns the error detail, such as the name of the constraint, column or table involved.
	 * @return the error detail, an empty Optional if none is available
	 */
	public final Optional<String> detail() {
		return Optional.ofNullable(detail);
	}

	/**
	 * Returns the message, for an exception based on an error type the one associated with the error type, in the
	 * default locale of the one reading it, along with the detail, if any, see {@link #message(Locale)}.
	 * @return the exception message
	 * @see #errorType()
	 * @see #detail()
	 */
	@Override
	public final @Nullable String getMessage() {
		return message(Locale.getDefault());
	}

	/**
	 * Returns the message in the given locale, for an exception based on an error type, otherwise the message
	 * this exception was constructed with, which is not affected by the locale. Useful where the default locale is
	 * not the one of the reader, a web server serving users of different languages for example.
	 * @param locale the locale
	 * @return the exception message in the given locale
	 */
	public @Nullable String message(Locale locale) {
		requireNonNull(locale);
		if (errorType().isPresent()) {
			try {
				return message(messageKey(), detail, locale);
			}
			catch (RuntimeException e) {
				// reading a message must never throw
			}
		}

		return super.getMessage();
	}

	/**
	 * @return the key of the message associated with this exception, only called for one based on an error type
	 */
	String messageKey() {
		return errorType().orElseThrow().messageKey();
	}

	/**
	 * Returns the underlying error code, note that this is only available when running with
	 * a local database connection.
	 * @return the underlying error code, -1 if not available
	 */
	public final int errorCode() {
		return errorCode;
	}

	/**
	 * Returns the underlying sql state, note that this is only available when running with
	 * a local database connection.
	 * @return the underlying sql state, an empty Optional if not available
	 */
	public final Optional<String> sqlState() {
		return Optional.ofNullable(sqlState);
	}

	@Override
	public final void setStackTrace(StackTraceElement[] stackTrace) {
		super.setStackTrace(stackTrace);
	}

	@Serial
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		// errorCode is transient and deserializes to 0; normalize to the documented "not available" value
		errorCode = -1;
	}

	private static String message(String messageKey, @Nullable String detail, Locale locale) {
		String message = messageBundle(DatabaseException.class, bundle(locale)).getString(messageKey);

		return detail == null ? message : message + ": " + detail;
	}

	private static ResourceBundle bundle(Locale locale) {
		ResourceBundle bundle = getBundle(DatabaseException.class.getName(), locale);
		String language = bundle.getLocale().getLanguage();
		if (!language.isEmpty() && !language.equals(locale.getLanguage())) {
			// a locale without a bundle falls back on the one of the default locale, before the root one,
			// an english reader on an icelandic web server getting icelandic, the root bundle is the one to use
			return getBundle(DatabaseException.class.getName(), Locale.ROOT);
		}

		return bundle;
	}
}