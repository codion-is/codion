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
package is.codion.framework.domain.entity.attribute;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.proxy.ProxyBuilder;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static is.codion.framework.domain.entity.attribute.Column.Generator.preserving;
import static is.codion.framework.domain.entity.attribute.Column.Generator.uuid;
import static org.junit.jupiter.api.Assertions.*;

public final class UuidGeneratorTest {

	private static final DomainType DOMAIN = DomainType.domainType("uuidGeneratorTest");
	private static final EntityType TYPE = DOMAIN.entityType("uuid");
	private static final Column<Integer> ID = TYPE.integerColumn("id");
	private static final Column<UUID> MADE = TYPE.column("made", UUID.class);
	private static final Column<UUID> PRESERVED = TYPE.column("preserved", UUID.class);

	private static final class TestDomain extends DomainModel {

		private TestDomain() {
			super(DOMAIN);
			add(TYPE.as()
							.attributes(
											ID.as()
															.primaryKey(),
											MADE.as()
															.column()
															.generator(uuid()),
											PRESERVED.as()
															.column()
															.generator(preserving(uuid())))
							.build());
		}
	}

	@Test
	void madeAndPreserved() throws SQLException {
		UUID given = UUID.randomUUID();
		Entity entity = new TestDomain().entities().entity(TYPE)
						.with(ID, 1)
						.with(MADE, given)
						.with(PRESERVED, given)
						.build();
		beforeInsert(entity);
		// made anew, replacing the one given, which preserving() keeps
		assertNotNull(entity.get(MADE));
		assertNotEquals(given, entity.get(MADE));
		assertEquals(given, entity.get(PRESERVED));

		Entity without = new TestDomain().entities().entity(TYPE)
						.with(ID, 2)
						.build();
		beforeInsert(without);
		assertNotNull(without.get(MADE));
		assertNotNull(without.get(PRESERVED));
		assertNotEquals(without.get(MADE), without.get(PRESERVED));
		assertNotEquals(entity.get(MADE), without.get(MADE));
	}

	private static void beforeInsert(Entity entity) throws SQLException {
		Database database = ProxyBuilder.of(Database.class).build();
		Connection connection = ProxyBuilder.of(Connection.class).build();
		for (Column<UUID> column : List.of(MADE, PRESERVED)) {
			entity.definition().columns().definition(column).generator().beforeInsert(entity, column, database, connection);
		}
	}
}
