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
package is.codion.tools.generator.domain;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.framework.domain.db.SchemaDomain;
import is.codion.framework.domain.db.SchemaDomain.SchemaSettings;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DomainSourceTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void petstore() throws Exception {
		try (Connection connection = Database.instance().createConnection(UNIT_TEST_USER)) {
			SchemaDomain schemaDomain = SchemaDomain.schemaDomain(connection.getMetaData(), "PETSTORE", SchemaSettings.builder()
							.primaryKeyColumnSuffix("_id")
							.auditColumnNames("insert_user", "insert_time")
							.hideAuditColumns(true)
							.build());
			String domainPackage = "is.codion.petstore.domain";
			Set<EntityType> dtos = schemaDomain.entities().definitions().stream()
							.map(EntityDefinition::type)
							.collect(toSet());
			dtos.removeIf(entityType ->
							entityType.name().equalsIgnoreCase("petstore.tag") ||
											entityType.name().equalsIgnoreCase("petstore.tag_item") ||
											entityType.name().equalsIgnoreCase("petstore.address"));
			DomainSource domainSource = DomainSource.builder()
							.domain(schemaDomain)
							.domainPackage(domainPackage)
							.dtos(dtos)
							.i18n(true)
							.build();
			String petstoreApi = textFileContents(DomainSourceTest.class, "PetstoreAPI.java");
			assertEquals(petstoreApi, domainSource.api());
			String petstoreImpl = textFileContents(DomainSourceTest.class, "PetstoreImpl.java");
			assertEquals(petstoreImpl, domainSource.implementation());
			domainSource = DomainSource.builder()
							.domain(schemaDomain)
							.domainPackage(domainPackage)
							.dtos(dtos)
							.build();
			String petstoreCombined = textFileContents(DomainSourceTest.class, "Petstore.java");
			assertEquals(petstoreCombined, domainSource.combined());
			String productProperties = textFileContents(DomainSourceTest.class, "petstore_product.properties");
			assertEquals(productProperties, domainSource.i18n(schemaDomain.entities().definition("petstore.product").type()));
			String petstoreTest = textFileContents(DomainSourceTest.class, "PetstoreTest.java");
			assertEquals(petstoreTest, domainSource.testCombined());
		}
	}

	@Test
	void chinook() throws Exception {
		try (Connection connection = Database.instance().createConnection(UNIT_TEST_USER)) {
			SchemaDomain schemaDomain = SchemaDomain.schemaDomain(connection.getMetaData(), "CHINOOK");
			String domainPackage = "is.codion.chinook.domain";
			DomainSource domainSource = DomainSource.builder()
							.domain(schemaDomain)
							.domainPackage(domainPackage)
							.dtos(entityTypes(schemaDomain))
							.build();
			String chinookApi = textFileContents(DomainSourceTest.class, "ChinookAPI.java");
			assertEquals(chinookApi, domainSource.api());
			String chinookImpl = textFileContents(DomainSourceTest.class, "ChinookImpl.java");
			assertEquals(chinookImpl, domainSource.implementation());
			String chinookCombined = textFileContents(DomainSourceTest.class, "Chinook.java");
			assertEquals(chinookCombined, domainSource.combined());
		}
	}

	@Test
	void world() throws Exception {
		try (Connection connection = Database.instance().createConnection(UNIT_TEST_USER)) {
			SchemaDomain schemaDomain = SchemaDomain.schemaDomain(connection.getMetaData(), "WORLD", SchemaSettings.builder()
							.viewSuffix("_v")
							.build());
			String domainPackage = "is.codion.world.domain";
			DomainSource domainSource = DomainSource.builder()
							.domain(schemaDomain)
							.domainPackage(domainPackage)
							.dtos(entityTypes(schemaDomain))
							.build();
			String worldApi = textFileContents(DomainSourceTest.class, "WorldAPI.java");
			assertEquals(worldApi, domainSource.api());
			String worldImpl = textFileContents(DomainSourceTest.class, "WorldImpl.java");
			assertEquals(worldImpl, domainSource.implementation());
			String worldCombined = textFileContents(DomainSourceTest.class, "World.java");
			assertEquals(worldCombined, domainSource.combined());
		}
	}

	private static String textFileContents(Class<?> resourceClass, String resourceName) throws IOException {
		try (BufferedReader input = new BufferedReader(new InputStreamReader(resourceClass.getResourceAsStream(resourceName)))) {
			return input.lines().collect(joining("\n"));
		}
	}

	@Test
	void underscoreToCamelCase() {
		Assertions.assertEquals("", DomainSource.underscoreToCamelCase(""));
		Assertions.assertEquals("noOfSpeakers", DomainSource.underscoreToCamelCase("noOfSpeakers"));
		Assertions.assertEquals("noOfSpeakers", DomainSource.underscoreToCamelCase("no_of_speakers"));
		Assertions.assertEquals("noOfSpeakers", DomainSource.underscoreToCamelCase("No_OF_speakeRS"));
		Assertions.assertEquals("helloWorld", DomainSource.underscoreToCamelCase("hello_World"));
		Assertions.assertEquals("", DomainSource.underscoreToCamelCase("_"));
		Assertions.assertEquals("aB", DomainSource.underscoreToCamelCase("a_b"));
		Assertions.assertEquals("aB", DomainSource.underscoreToCamelCase("a_b_"));
		Assertions.assertEquals("aBC", DomainSource.underscoreToCamelCase("a_b_c"));
		Assertions.assertEquals("aBaC", DomainSource.underscoreToCamelCase("a_ba_c"));
		Assertions.assertEquals("a", DomainSource.underscoreToCamelCase("a__"));
		Assertions.assertEquals("a", DomainSource.underscoreToCamelCase("__a"));
		Assertions.assertEquals("a", DomainSource.underscoreToCamelCase("__A"));
	}

	private static Set<EntityType> entityTypes(SchemaDomain schemaDomain) {
		return schemaDomain.entities().definitions().stream()
						.map(EntityDefinition::type)
						.collect(toSet());
	}
}
