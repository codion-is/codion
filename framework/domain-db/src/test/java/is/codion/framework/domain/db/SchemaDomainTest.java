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
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.db.SchemaDomain.SchemaSettings;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.test.DefaultEntityFactory;
import is.codion.framework.domain.test.DomainTest;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public final class SchemaDomainTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Database DATABASE = Database.instance();

	@Test
	void petstore() throws Exception {
		try (Connection connection = DATABASE.createConnection(UNIT_TEST_USER)) {
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
				if (!entityDefinition.type().name().equals("petstore.tag_item")) {
					assertTrue(entityDefinition.primaryKey().definitions().get(0).generated());
				}
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
			DomainTest domainTest = new DomainTest(petstore, UNIT_TEST_USER);
			for (EntityDefinition entityDefinition : petstore.entities().definitions()) {
				domainTest.test(entityDefinition.type());
			}
		}
	}

	@Test
	void chinook() throws Exception {
		try (Connection connection = DATABASE.createConnection(UNIT_TEST_USER)) {
			SchemaDomain chinook = SchemaDomain.schemaDomain(connection.getMetaData(), "CHINOOK", SchemaSettings.builder()
							.lowerCaseIdentifiers(false)
							.build());
			EntityDefinition customer = chinook.entities().definition("CHINOOK.CUSTOMER");
			assertTrue(customer.attributes().definitions().stream()
							.anyMatch(definition -> definition.attribute().name().equals("LASTNAME")));
			chinook.entities().definitions().forEach(definition ->
							assertTrue(definition.primaryKey().definitions().get(0).generated()));
			DomainTest domainTest = new DomainTest(chinook, ChinookEntityFactory::new, UNIT_TEST_USER);
			for (EntityDefinition entityDefinition : chinook.entities().definitions()) {
				domainTest.test(entityDefinition.type());
			}
		}
	}

	private static final class ChinookEntityFactory extends DefaultEntityFactory {

		private ChinookEntityFactory(EntityConnection connection) {
			super(connection);
		}

		@Override
		public Entity entity(EntityType entityType) {
			Entity entity = super.entity(entityType);
			if (entityType.name().equals("CHINOOK.TRACK")) {
				EntityDefinition.Attributes attributes = entities().definition(entityType).attributes();
				Attribute<Integer> rating = attributes.get("RATING");
				entity.set(rating, 5);
			}

			return entity;
		}

		@Override
		public void modify(Entity entity) {
			if (entity.type().name().equals("CHINOOK.TRACK")) {
				EntityDefinition.Attributes attributes = entities().definition(entity.type()).attributes();
				Attribute<Integer> rating = attributes.get("RATING");
				entity.set(rating, 7);
			}
		}
	}

	@Test
	void world() throws Exception {
		try (Connection connection = DATABASE.createConnection(UNIT_TEST_USER)) {
			SchemaDomain world = SchemaDomain.schemaDomain(connection.getMetaData(), "WORLD", SchemaSettings.builder()
							.viewSuffix("_v")
							.build());
			EntityDefinition countryCity = world.entities().definition("world.country_city_v");
			assertEquals("world.country_city_v", countryCity.type().name());
			assertEquals("Country city", countryCity.caption());
			DomainTest domainTest = new DomainTest(world, WorldEntityFactory::new, UNIT_TEST_USER);
			for (EntityDefinition entityDefinition : world.entities().definitions()) {
				domainTest.test(entityDefinition.type());
			}
		}
	}

	private static final class WorldEntityFactory extends DefaultEntityFactory {

		private final EntityType countryType;
		private final EntityType countryLanguageType;
		private final EntityType cityType;
		private final Attribute<String> code;
		private final Attribute<String> continent;
		private final Attribute<Double> lifeexpectancy;
		private final Attribute<Double> percentage;
		private final Attribute<Integer> cityId;

		private WorldEntityFactory(EntityConnection connection) {
			super(connection);
			EntityDefinition country = connection.entities().definition("world.country");
			EntityDefinition countryLanguage = connection.entities().definition("world.countrylanguage");
			EntityDefinition city = connection.entities().definition("world.city");
			this.countryType = country.type();
			this.countryLanguageType = countryLanguage.type();
			this.code = country.attributes().get("code");
			this.continent = country.attributes().get("continent");
			this.lifeexpectancy = country.attributes().get("lifeexpectancy");
			this.percentage = countryLanguage.attributes().get("percentage");
			this.cityType = city.type();
			this.cityId = city.attributes().get("id");
		}

		@Override
		public Entity entity(EntityType entityType) {
			Entity entity = super.entity(entityType);
			if (entityType.equals(countryType)) {
				entity.set(code, "XYZ");
				entity.set(continent, "Asia");
				entity.set(lifeexpectancy, 88d);
			}
			else if (entityType.equals(countryLanguageType)) {
				entity.set(percentage, 42.2);
			}

			return entity;
		}

		@Override
		public void modify(Entity entity) {
			super.modify(entity);
			if (entity.type().equals(countryType)) {
				entity.set(continent, "Europe");
				entity.set(lifeexpectancy, 85d);
			}
			else if (entity.type().equals(countryLanguageType)) {
				entity.set(percentage, 34.4);
			}
		}

		@Override
		public Optional<Entity> entity(ForeignKey foreignKey) {
			if (foreignKey.referencedType().name().equals("world.country")) {
				return Optional.of(entities().entity(countryType)
								.with(code, "ISL")
								.build());
			}
			if (foreignKey.referencedType().name().equals("world.city")) {
				return Optional.of(entities().entity(cityType)
								.with(cityId, 1449)
								.build());
			}

			return super.entity(foreignKey);
		}
	}
}
