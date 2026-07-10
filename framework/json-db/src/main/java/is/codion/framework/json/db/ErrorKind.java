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
package is.codion.framework.json.db;

import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

/**
 * The kind of error an {@link ErrorEnvelope} carries, a closed set.
 * <p>Each kind determines the HTTP status of the error response and the severity at which the server logs it,
 * and it is the only type information crossing the wire, the client mapping it to a known exception constructor.
 * A class name plus reflective instantiation would reintroduce the arbitrary-instantiation problem the envelope
 * exists to remove, so an unrecognized kind, which a client older than the server encounters, must be treated as
 * a generic error rather than resolved.
 * @see ErrorEnvelope
 */
public enum ErrorKind {

	/**
	 * Authentication failed, or the credentials, client id or client type headers are missing or malformed.
	 */
	AUTHENTICATION(401, Severity.DEBUG),
	/**
	 * The request is malformed, an unparseable body, an unknown entity type or attribute name, a malformed header.
	 */
	BAD_REQUEST(400, Severity.DEBUG),
	/**
	 * The request is at odds with the state of the connection, committing when no transaction is open,
	 * or with the state of the domain, calling an operation whose parameter type is not registered.
	 * <p>Logged at {@link Severity#WARN}, being a programming error on one side or the other, and reported
	 * as {@link IllegalStateException}, which {@link is.codion.framework.db.EntityConnection}'s transaction
	 * methods document.
	 */
	ILLEGAL_STATE(409, Severity.WARN),
	/**
	 * The server is at its connection limit.
	 */
	CONNECTION_UNAVAILABLE(503, Severity.WARN),
	/**
	 * An entity being updated has been modified or deleted since it was loaded.
	 */
	CONFLICT_MODIFIED(409, Severity.DEBUG),
	/**
	 * A referential integrity constraint prevented the operation.
	 */
	CONFLICT_REFERENTIAL(409, Severity.DEBUG),
	/**
	 * A unique constraint prevented the operation.
	 */
	CONFLICT_UNIQUE(409, Severity.DEBUG),
	/**
	 * An expected entity was not found.
	 */
	NOT_FOUND(404, Severity.DEBUG),
	/**
	 * One entity was expected but many were found.
	 */
	MULTIPLE_FOUND(409, Severity.DEBUG),
	/**
	 * An insert failed.
	 */
	INSERT(409, Severity.DEBUG),
	/**
	 * An update failed.
	 */
	UPDATE(409, Severity.DEBUG),
	/**
	 * A delete failed.
	 */
	DELETE(409, Severity.DEBUG),
	/**
	 * A statement timed out or was cancelled.
	 */
	QUERY_TIMEOUT(504, Severity.WARN),
	/**
	 * A database error with no more specific kind.
	 */
	DATABASE(500, Severity.ERROR),
	/**
	 * Anything else, the server is at fault. The message is replaced with a generic one,
	 * the {@link ErrorEnvelope#correlationId()} being the client's only link to the server log.
	 */
	INTERNAL(500, Severity.ERROR);

	private final int status;
	private final Severity severity;

	ErrorKind(int status, Severity severity) {
		this.status = status;
		this.severity = severity;
	}

	/**
	 * @return the HTTP status of an error response of this kind
	 */
	public int status() {
		return status;
	}

	/**
	 * @return the severity at which the server logs an error of this kind
	 */
	public Severity severity() {
		return severity;
	}

	/**
	 * Returns the {@link ErrorKind} with the given name, an empty {@link Optional} if none exists,
	 * which is how a client older than the server encounters a kind it does not know.
	 * @param name the kind name
	 * @return the {@link ErrorKind} with the given name
	 */
	public static Optional<ErrorKind> of(String name) {
		requireNonNull(name);

		return stream(values())
						.filter(kind -> kind.name().equals(name))
						.findFirst();
	}

	/**
	 * The severity at which the server logs an error.
	 * <p>{@link #DEBUG} is for the outcomes a correctly functioning application produces, a failed login,
	 * a row someone else changed first, a delete a dependency prevents. Logging those at a higher level
	 * buries the genuine faults, which are the {@link #ERROR} ones.
	 */
	public enum Severity {
		/**
		 * An expected outcome, not a fault.
		 */
		DEBUG,
		/**
		 * A fault, but not the server's.
		 */
		WARN,
		/**
		 * A server fault.
		 */
		ERROR
	}
}
