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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

public final class DatabaseExceptionTest {

	private static final Locale ENGLISH = new Locale("en", "EN");
	private static final Locale ICELANDIC = new Locale("is", "IS");

	@Test
	void errorTypeAndDetail() {
		DatabaseException exception = new DatabaseException(new SQLException("driver message", "23502", 1400), ErrorType.NULL_VALUE, "NAME");
		assertEquals(ErrorType.NULL_VALUE, exception.errorType().orElseThrow());
		assertEquals("NAME", exception.detail().orElseThrow());
		assertEquals(1400, exception.errorCode());
		assertEquals("23502", exception.sqlState().orElseThrow());
		assertEquals(message("null_value", ENGLISH) + ": NAME", exception.message(ENGLISH));
		// without a detail
		assertEquals(message("timeout", ENGLISH), new QueryTimeoutException(null, ErrorType.TIMEOUT, null).message(ENGLISH));
		// message based
		DatabaseException messageBased = new DatabaseException("message");
		assertFalse(messageBased.errorType().isPresent());
		assertFalse(messageBased.detail().isPresent());
		assertEquals("message", messageBased.getMessage());
		assertEquals("message", messageBased.message(ICELANDIC));
		assertNull(new DatabaseException((String) null).getMessage());
		assertThrows(NullPointerException.class, () -> new DatabaseException((ErrorType) null, "detail"));
	}

	@Test
	void messageFollowsTheReader() throws Exception {
		Locale defaultLocale = Locale.getDefault();
		try {
			// thrown on an english server
			Locale.setDefault(ENGLISH);
			DatabaseException exception = new UniqueConstraintException(new SQLException("unique", "23505"), ErrorType.UNIQUE_CONSTRAINT, "EMP_PK");
			assertEquals(message("unique_constraint", ENGLISH) + ": EMP_PK", exception.getMessage());
			byte[] serialized = serialize(exception);
			// read on an icelandic client
			Locale.setDefault(ICELANDIC);
			DatabaseException deserialized = deserialize(serialized);
			assertInstanceOf(UniqueConstraintException.class, deserialized);
			assertEquals(ErrorType.UNIQUE_CONSTRAINT, deserialized.errorType().orElseThrow());
			assertEquals(message("unique_constraint", ICELANDIC) + ": EMP_PK", deserialized.getMessage());
			assertNotEquals(message("unique_constraint", ENGLISH), message("unique_constraint", ICELANDIC));
			// the same instance follows the default locale, and a locale can be specified
			assertEquals(message("unique_constraint", ICELANDIC) + ": EMP_PK", exception.getMessage());
			assertEquals(message("unique_constraint", ENGLISH) + ": EMP_PK", exception.message(ENGLISH));
			assertTrue(exception.toString().endsWith(exception.getMessage()));
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@Test
	void referentialIntegrityMessageFollowsTheOperation() throws Exception {
		SQLException cause = new SQLException("fk", "23503");
		assertEquals(message("parent_missing", ENGLISH),
						new ReferentialIntegrityException(cause, ErrorType.REFERENTIAL_INTEGRITY, null, Operation.INSERT).message(ENGLISH));
		assertEquals(message("child_exists", ENGLISH),
						new ReferentialIntegrityException(cause, ErrorType.REFERENTIAL_INTEGRITY, null, Operation.DELETE).message(ENGLISH));
		assertEquals(message("referential_integrity", ENGLISH),
						new ReferentialIntegrityException(cause, ErrorType.REFERENTIAL_INTEGRITY, null, Operation.UPDATE).message(ENGLISH));
		// the database reporting which way
		assertEquals(message("child_exists", ENGLISH) + ": FK",
						new ReferentialIntegrityException(cause, ErrorType.CHILD_EXISTS, "FK", Operation.UPDATE).message(ENGLISH));
		// the operation travels, and the message with it
		ReferentialIntegrityException deserialized = (ReferentialIntegrityException) deserialize(serialize(
						new ReferentialIntegrityException(cause, ErrorType.REFERENTIAL_INTEGRITY, null, Operation.DELETE)));
		assertEquals(Operation.DELETE, deserialized.operation());
		assertEquals(message("child_exists", ICELANDIC), deserialized.message(ICELANDIC));
	}

	@Test
	void unknownErrorType() throws Exception {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(ENGLISH);
			byte[] serialized = serialize(new DatabaseException(ErrorType.ROW_LOCKED, null));
			// an error type from a newer server, unknown to this client
			byte[] unknown = new String(serialized, StandardCharsets.ISO_8859_1)
							.replace("ROW_LOCKED", "ROW_LOCKEX").getBytes(StandardCharsets.ISO_8859_1);
			Locale.setDefault(ICELANDIC);
			DatabaseException deserialized = deserialize(unknown);
			assertFalse(deserialized.errorType().isPresent());
			// the message as put together by the server
			assertEquals(message("row_locked", ENGLISH), deserialized.getMessage());
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	private static String message(String key, Locale locale) {
		// english being the root bundle, which a lookup by locale only reaches when the default locale has no bundle
		return ResourceBundle.getBundle(DatabaseException.class.getName(), ENGLISH.equals(locale) ? Locale.ROOT : locale).getString(key);
	}

	private static byte[] serialize(DatabaseException exception) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream stream = new ObjectOutputStream(bytes)) {
			stream.writeObject(exception);
		}

		return bytes.toByteArray();
	}

	private static DatabaseException deserialize(byte[] bytes) throws Exception {
		try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			return (DatabaseException) stream.readObject();
		}
	}
}
