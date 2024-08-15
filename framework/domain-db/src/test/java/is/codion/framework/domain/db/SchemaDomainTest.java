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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.db;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.framework.domain.db.SchemaDomain.SchemaSettings;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.AuditColumn.AuditAction;
import is.codion.framework.domain.entity.attribute.AuditColumnDefinition;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SchemaDomainTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void petstore() throws Exception {
		try (Connection connection = Database.instance().createConnection(UNIT_TEST_USER)) {
			SchemaDomain petstore = SchemaDomain.schemaDomain(connection, "PETSTORE", SchemaSettings.builder()
							.auditInsertUserColumnName("insert_user")
							.auditInsertTimeColumnName("insert_time")
							.auditUpdateUserColumnName("update_user")
							.auditUpdateTimeColumnName("update_time")
							.build());
			List<EntityDefinition> tableEntities = petstore.entities().definitions().stream()
							.filter(definition -> !definition.readOnly())
							.collect(toList());
			for (EntityDefinition entityDefinition : tableEntities) {
				EntityDefinition.Attributes attributes = entityDefinition.attributes();

				AuditColumnDefinition<String> insertUserDefinition =
								(AuditColumnDefinition<String>) attributes.definition(attributes.<String>get("INSERT_USER"));
				assertEquals(AuditAction.INSERT, insertUserDefinition.auditAction());
				assertEquals("Insert user", insertUserDefinition.caption());

				AuditColumnDefinition<String> insertTimeDefinition =
								(AuditColumnDefinition<String>) attributes.definition(attributes.<String>get("INSERT_TIME"));
				assertEquals(AuditAction.INSERT, insertTimeDefinition.auditAction());
				assertEquals("Insert time", insertTimeDefinition.caption());

				AuditColumnDefinition<String> updateUserDefinition =
								(AuditColumnDefinition<String>) attributes.definition(attributes.<String>get("UPDATE_USER"));
				assertEquals(AuditAction.UPDATE, updateUserDefinition.auditAction());
				assertEquals("Update user", updateUserDefinition.caption());

				AuditColumnDefinition<String> updateTimeDefinition =
								(AuditColumnDefinition<String>) attributes.definition(attributes.<String>get("UPDATE_TIME"));
				assertEquals(AuditAction.UPDATE, updateTimeDefinition.auditAction());
				assertEquals("Update time", updateTimeDefinition.caption());
			}
		}
	}

	@Test
	void chinook() throws Exception {
		try (Connection connection = Database.instance().createConnection(UNIT_TEST_USER)) {
			SchemaDomain.schemaDomain(connection, "CHINOOK");
		}
	}

	@Test
	void world() throws Exception {
		try (Connection connection = Database.instance().createConnection(UNIT_TEST_USER)) {
			SchemaDomain.schemaDomain(connection, "WORLD");
		}
	}
}
