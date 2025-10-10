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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.db;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.framework.domain.db.SchemaDomain.SchemaSettings;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public final class SchemaDomainTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void petstore() throws Exception {
		try (Connection connection = Database.instance().createConnection(UNIT_TEST_USER)) {
			SchemaDomain petstore = SchemaDomain.schemaDomain(connection.getMetaData(), "PETSTORE", SchemaSettings.builder()
							.primaryKeyColumnSuffix("_id")
							.auditColumnNames("insert_user", "insert_time", "update_user", "update_time")
							.hideAuditColumns(true)
							.build());
			List<EntityDefinition> tableEntities = petstore.entities().definitions().stream()
							.filter(definition -> !definition.readOnly())
							.collect(toList());
			for (EntityDefinition entityDefinition : tableEntities) {
				entityDefinition.foreignKeys().get().forEach(foreignKey -> assertFalse(foreignKey.name().endsWith("_id_fk")));
				List<AttributeDefinition<?>> attributeDefinitions = new ArrayList<>(entityDefinition.attributes().definitions());
				List<AttributeDefinition<?>> auditColumns = attributeDefinitions.subList(attributeDefinitions.size() - 4, attributeDefinitions.size());
				ColumnDefinition<?> insertUser = (ColumnDefinition<?>) auditColumns.get(0);
				assertEquals("insert_user", insertUser.attribute().name());
				assertTrue(insertUser.readOnly());
				assertTrue(insertUser.hidden());
				ColumnDefinition<?> insertTime = (ColumnDefinition<?>) auditColumns.get(1);
				assertEquals("insert_time", insertTime.attribute().name());
				assertTrue(insertTime.readOnly());
				assertTrue(insertTime.hidden());
				ColumnDefinition<?> updateUser = (ColumnDefinition<?>) auditColumns.get(2);
				assertEquals("update_user", updateUser.attribute().name());
				assertTrue(updateUser.readOnly());
				assertTrue(updateUser.hidden());
				ColumnDefinition<?> updateTime = (ColumnDefinition<?>) auditColumns.get(3);
				assertEquals("update_time", updateTime.attribute().name());
				assertTrue(updateTime.readOnly());
				assertTrue(updateTime.hidden());
			}
		}
	}

	@Test
	void chinook() throws Exception {
		try (Connection connection = Database.instance().createConnection(UNIT_TEST_USER)) {
			SchemaDomain chinook = SchemaDomain.schemaDomain(connection.getMetaData(), "CHINOOK", SchemaSettings.builder()
							.lowerCaseIdentifiers(false)
							.build());
			EntityDefinition customer = chinook.entities().definition("CHINOOK.CUSTOMER");
			assertTrue(customer.attributes().definitions().stream()
							.anyMatch(definition -> definition.attribute().name().equals("LASTNAME")));
		}
	}

	@Test
	void world() throws Exception {
		try (Connection connection = Database.instance().createConnection(UNIT_TEST_USER)) {
			SchemaDomain world = SchemaDomain.schemaDomain(connection.getMetaData(), "WORLD", SchemaSettings.builder()
							.viewSuffix("_v")
							.build());
			EntityDefinition countryCity = world.entities().definition("world.country_city");
			assertEquals("world.country_city", countryCity.type().name());
			assertEquals("Country city", countryCity.caption());
		}
	}
}
