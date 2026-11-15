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
package is.codion.framework.server;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Column;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class NotSerializableDomainTest {

	@Test
	void serializable() {
		assertTrue(EntityServer.notSerializable(new TestDomain()).isEmpty());
	}

	@Test
	void notSerializable() {
		List<String> notSerializable = EntityServer.notSerializable(new NotSerializableDomain());
		assertEquals(5, notSerializable.size());
		String validator = NotSerializableDomain.PlainValidator.class.getName();
		String formatter = NotSerializableDomain.PlainFormatter.class.getName();
		assertEquals("not_serializable.first validator (" + validator + ")", notSerializable.get(0));
		assertTrue(notSerializable.get(1).startsWith("not_serializable.first comparator ("));
		assertTrue(notSerializable.get(2).startsWith("not_serializable.first exists ("));
		assertEquals("not_serializable.first formatter (" + formatter + ")", notSerializable.get(3));
		assertTrue(notSerializable.get(4).startsWith("not_serializable.second.name comparator ("));
	}

	private static final class NotSerializableDomain extends DomainModel {

		private static final DomainType DOMAIN = DomainType.domainType(NotSerializableDomain.class);

		interface First {
			EntityType TYPE = DOMAIN.entityType("not_serializable.first");

			Column<Integer> ID = TYPE.integerColumn("id");
			Column<String> NAME = TYPE.stringColumn("name");
		}

		interface Second {
			EntityType TYPE = DOMAIN.entityType("not_serializable.second");

			Column<Integer> ID = TYPE.integerColumn("id");
			Column<String> NAME = TYPE.stringColumn("name");
		}

		private NotSerializableDomain() {
			super(DOMAIN);
			// Each one fine with a local connection, none of them serializable
			add(First.TYPE.as()
							.attributes(
											First.ID.as()
															.primaryKey(),
											First.NAME.as()
															.column())
							.validator(new PlainValidator())
							.comparator((first, second) -> 0)
							.exists(entity -> true)
							.formatter(new PlainFormatter())
							.build());
			add(Second.TYPE.as()
							.attributes(
											Second.ID.as()
															.primaryKey(),
											Second.NAME.as()
															.column()
															.comparator((first, second) -> first.compareToIgnoreCase(second)))
							.build());
		}

		private static final class PlainValidator implements EntityValidator {}

		private static final class PlainFormatter implements Function<Entity, String> {

			@Override
			public String apply(Entity entity) {
				return "first";
			}
		}
	}
}
