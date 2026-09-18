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

import java.util.ResourceBundle;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ExceptionsTest {

	private static final Entities ENTITIES = new TestDomain().entities();

	@Test
	void defaultMessages() {
		assertEquals(message(EntityNotFoundException.class, "record_not_found"), new EntityNotFoundException().getMessage());
		assertEquals("custom", new EntityNotFoundException("custom").getMessage());
		assertEquals(message(MultipleEntitiesFoundException.class, "multiple_records_found"), new MultipleEntitiesFoundException().getMessage());
		assertEquals("custom", new MultipleEntitiesFoundException("custom").getMessage());
	}

	@Test
	void entityModified() {
		Entity department = ENTITIES.entity(Department.TYPE)
						.with(Department.ID, 10)
						.with(Department.NAME, "Sales")
						.build()
						.copy().mutable();
		department.set(Department.NAME, "Marketing");
		Entity current = ENTITIES.entity(Department.TYPE)
						.with(Department.ID, 10)
						.with(Department.NAME, "Accounting")
						.build();
		String recordModified = message(EntityModifiedException.class, "record_modified");

		EntityModifiedException modified = new EntityModifiedException(department, current, singletonList(Department.NAME));
		assertEquals(recordModified + ": " + Department.TYPE + "\n" + Department.NAME + ": Sales -> Accounting", modified.getMessage());

		// deleted, described by its original state
		Entity original = department.copy().mutable();
		original.revert();
		EntityModifiedException deleted = new EntityModifiedException(department, null, emptyList());
		assertEquals(recordModified + ", " + original + " " + message(EntityModifiedException.class, "has_been_deleted"), deleted.getMessage());

		assertEquals("custom", new EntityModifiedException(department, current, singletonList(Department.NAME), "custom").getMessage());
	}

	// independent of the default locale
	private static String message(Class<?> exceptionClass, String key) {
		return ResourceBundle.getBundle(exceptionClass.getName()).getString(key);
	}
}
