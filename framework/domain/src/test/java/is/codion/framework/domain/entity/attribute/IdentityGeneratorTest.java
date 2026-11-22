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
import is.codion.common.db.database.GetValue;
import is.codion.common.utilities.proxy.ProxyBuilder;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class IdentityGeneratorTest {

	private static final DomainType DOMAIN = DomainType.domainType("identityGeneratorTest");
	private static final EntityType TYPE = DOMAIN.entityType("identity");
	private static final Column<Integer> ID = TYPE.integerColumn("id");
	private static final Column<String> NAME = TYPE.stringColumn("name");

	private static final class TestDomain extends DomainModel {

		private TestDomain() {
			super(DOMAIN);
			add(TYPE.as()
							.attributes(
											ID.as()
															.primaryKey()
															.generator(identity()),
											NAME.as()
															.column())
							.build());
		}
	}

	@Test
	void readsTheOneColumnReturned() throws SQLException {
		// as SQLite's driver returns the key of an INTEGER PRIMARY KEY
		assertEquals(42, generated(List.of("last_insert_rowid()"), List.of(42)));
	}

	@Test
	void readsTheColumnByNameAmongOthers() throws SQLException {
		// as PostgreSQL's driver returns every column
		assertEquals(42, generated(List.of("name", "id"), List.of("Akureyri", 42)));
	}

	private static Integer generated(List<String> columns, List<Object> values) throws SQLException {
		Entity entity = new TestDomain().entities().entity(TYPE)
						.with(NAME, "Akureyri")
						.build();
		ResultSetMetaData metaData = ProxyBuilder.of(ResultSetMetaData.class)
						.method("getColumnCount", parameters -> columns.size())
						.build();
		ResultSet generatedKeys = ProxyBuilder.of(ResultSet.class)
						.method("next", parameters -> true)
						.method("getMetaData", parameters -> metaData)
						.method("findColumn", String.class, parameters -> columns.indexOf((String) parameters.arguments().get(0)) + 1)
						.method("getInt", int.class, parameters -> values.get((int) parameters.arguments().get(0) - 1))
						.method("wasNull", parameters -> false)
						.method("close", parameters -> null)
						.build();
		Statement statement = ProxyBuilder.of(Statement.class)
						.method("getGeneratedKeys", parameters -> generatedKeys)
						.build();
		Database database = ProxyBuilder.of(Database.class)
						.method("getter", int.class, parameters -> (GetValue<Integer>) (resultSet, index) -> {
							int value = resultSet.getInt(index);

							return resultSet.wasNull() ? null : value;
						})
						.build();
		entity.definition().columns().definition(ID).generator().afterInsert(entity, ID, database, statement);

		return entity.get(ID);
	}
}
