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
package is.codion.framework.db.exception;

import is.codion.framework.db.TestDomain;
import is.codion.framework.db.TestDomain.Department;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
import java.util.ResourceBundle;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ExceptionsTest {

	private static final Entities ENTITIES = new TestDomain().entities();

	private static final Locale ENGLISH = new Locale("en", "EN");
	private static final Locale ICELANDIC = new Locale("is", "IS");

	@Test
	void defaultMessages() {
		assertEquals(message(EntityNotFoundException.class, "record_not_found", ENGLISH), new EntityNotFoundException().message(ENGLISH));
		assertEquals(message(EntityNotFoundException.class, "record_not_found", ICELANDIC), new EntityNotFoundException().message(ICELANDIC));
		assertEquals("custom", new EntityNotFoundException("custom").getMessage());
		assertEquals("custom", new EntityNotFoundException("custom").message(ICELANDIC));
		assertEquals(message(MultipleEntitiesFoundException.class, "multiple_records_found", ENGLISH), new MultipleEntitiesFoundException().message(ENGLISH));
		assertEquals(message(MultipleEntitiesFoundException.class, "multiple_records_found", ICELANDIC), new MultipleEntitiesFoundException().message(ICELANDIC));
		assertEquals("custom", new MultipleEntitiesFoundException("custom").getMessage());
	}

	@Test
	void messageFollowsTheReader() throws Exception {
		Locale defaultLocale = Locale.getDefault();
		try {
			// thrown on an english server
			Locale.setDefault(ENGLISH);
			byte[] notFound = serialize(new EntityNotFoundException());
			byte[] custom = serialize(new EntityNotFoundException("custom"));
			byte[] multipleFound = serialize(new MultipleEntitiesFoundException());
			Entity department = department();
			byte[] modified = serialize(new EntityModifiedException(department, null, emptyList()));
			// read on an icelandic client
			Locale.setDefault(ICELANDIC);
			assertEquals(message(EntityNotFoundException.class, "record_not_found", ICELANDIC), deserialize(notFound).getMessage());
			assertEquals("custom", deserialize(custom).getMessage());
			assertEquals(message(MultipleEntitiesFoundException.class, "multiple_records_found", ICELANDIC), deserialize(multipleFound).getMessage());
			assertTrue(deserialize(modified).getMessage().startsWith(message(EntityModifiedException.class, "record_modified", ICELANDIC)));
			assertTrue(deserialize(modified).getMessage().endsWith(message(EntityModifiedException.class, "has_been_deleted", ICELANDIC)));
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@Test
	void entityModified() {
		Entity department = department();
		Entity current = ENTITIES.entity(Department.TYPE)
						.with(Department.ID, 10)
						.with(Department.NAME, "Accounting")
						.build();
		String recordModified = message(EntityModifiedException.class, "record_modified", ENGLISH);

		EntityModifiedException modified = new EntityModifiedException(department, current, singletonList(Department.NAME));
		assertEquals(recordModified + ": " + Department.TYPE + "\n" + Department.NAME + ": Sales -> Accounting", modified.message(ENGLISH));
		assertTrue(modified.message(ICELANDIC).startsWith(message(EntityModifiedException.class, "record_modified", ICELANDIC)));

		// deleted, described by its original state
		Entity original = department.copy().mutable();
		original.revert();
		EntityModifiedException deleted = new EntityModifiedException(department, null, emptyList());
		assertEquals(recordModified + ", " + original + " " + message(EntityModifiedException.class, "has_been_deleted", ENGLISH), deleted.message(ENGLISH));
	}

	private static Entity department() {
		Entity department = ENTITIES.entity(Department.TYPE)
						.with(Department.ID, 10)
						.with(Department.NAME, "Sales")
						.build()
						.copy().mutable();
		department.set(Department.NAME, "Marketing");

		return department;
	}

	// english being the root bundle, which a lookup by locale only reaches when the default locale has no bundle
	private static String message(Class<?> exceptionClass, String key, Locale locale) {
		return ResourceBundle.getBundle(exceptionClass.getName(), ENGLISH.equals(locale) ? Locale.ROOT : locale).getString(key);
	}

	private static byte[] serialize(Exception exception) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream stream = new ObjectOutputStream(bytes)) {
			stream.writeObject(exception);
		}

		return bytes.toByteArray();
	}

	private static Exception deserialize(byte[] bytes) throws Exception {
		try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			return (Exception) stream.readObject();
		}
	}
}
