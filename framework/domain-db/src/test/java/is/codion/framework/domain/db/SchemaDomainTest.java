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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.db;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.db.SchemaDomain.SchemaSettings;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.test.AbstractEntityFactory;
import is.codion.framework.domain.test.DomainTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
			try (DomainTest domainTest = new DomainTest(petstore, UNIT_TEST_USER)) {
				for (EntityDefinition entityDefinition : petstore.entities().definitions()) {
					domainTest.test(entityDefinition.type());
				}
			}
		}
	}

	@Test
	void turkishLocale() throws Exception {
		// A table or column name is an identifier, not prose. Under Turkish "ITEM" lower cases to "ıtem" and
		// "INSERT_USER" to "ınsert_user", so the entity type came out misspelled and no longer matched the configured
		// audit column names - leaving the audit columns visible and writable on a Turkish developer's machine.
		Locale locale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr"));
			try (Connection connection = DATABASE.createConnection(UNIT_TEST_USER)) {
				SchemaDomain petstore = SchemaDomain.schemaDomain(connection.getMetaData(), "PETSTORE", SchemaSettings.builder()
								.primaryKeyColumnSuffix("_id")
								.auditColumnNames("insert_user", "insert_time", "update_user", "update_time")
								.hideAuditColumns(true)
								.build());
				EntityDefinition item = petstore.entities().definitions().stream()
								.filter(definition -> definition.type().name().equals("petstore.item"))
								.findFirst()
								.orElseThrow(() -> new AssertionError("No petstore.item entity, type names: " +
												petstore.entities().definitions().stream()
																.map(definition -> definition.type().name())
																.collect(toList())));
				List<AttributeDefinition<?>> attributeDefinitions = new ArrayList<>(item.attributes().definitions());
				List<AttributeDefinition<?>> auditColumns = attributeDefinitions.subList(attributeDefinitions.size() - 4, attributeDefinitions.size());
				for (AttributeDefinition<?> auditColumn : auditColumns) {
					assertTrue(auditColumn.hidden(), auditColumn.attribute().name() + " should be hidden");
				}
			}
		}
		finally {
			Locale.setDefault(locale);
		}
	}

	@Test
	void sharedForeignKeyColumns() throws Exception {
		try (Connection connection = DATABASE.createConnection(UNIT_TEST_USER)) {
			SchemaDomain samples = SchemaDomain.schemaDomain(connection.getMetaData(), "SAMPLES");

			// species_no set by the species, the foreign key with the fewest columns, the maturity reading it
			EntityDefinition otolith = samples.entities().definition("samples.otolith");
			Column<?> speciesNo = column(otolith, "species_no");
			ForeignKeyDefinition species = foreignKey(otolith, "samples.species");
			ForeignKeyDefinition maturity = foreignKey(otolith, "samples.maturity");
			assertFalse(species.readOnly(speciesNo));
			assertTrue(maturity.readOnly(speciesNo));
			assertFalse(maturity.readOnly(column(otolith, "stage")));

			// both foreign keys on the measure's own key, ending on the same column, one of them setting it
			EntityDefinition measure = samples.entities().definition("samples.measure");
			Column<?> id = column(measure, "id");
			ForeignKeyDefinition label = foreignKey(measure, "samples.label");
			ForeignKeyDefinition remark = foreignKey(measure, "samples.remark");
			assertNotEquals(label.readOnly(id), remark.readOnly(id));
		}
	}

	@Test
	void caseSensitiveNames() throws Exception {
		try (Connection connection = DATABASE.createConnection(UNIT_TEST_USER)) {
			SchemaDomain gbif = SchemaDomain.schemaDomain(connection.getMetaData(), "GBIF");

			// created quoted, keeping their case, quoted in SQL
			EntityDefinition occurrence = gbif.entities().definition("gbif.Occurrence");
			assertEquals("gbif.\"Occurrence\"", occurrence.table());
			assertEquals("Occurrence", occurrence.caption());
			ColumnDefinition<?> verbatimLocality = occurrence.columns().definition(column(occurrence, "verbatimLocality"));
			assertEquals("\"verbatimLocality\"", verbatimLocality.name());
			assertEquals("Verbatim locality", verbatimLocality.caption());
			assertEquals("datasetID_fk", foreignKey(occurrence, "gbif.dataset").attribute().name());
			// folded to upper case, in lower case and unquoted
			assertEquals("remarks", occurrence.columns().definition(column(occurrence, "remarks")).name());
			EntityDefinition dataset = gbif.entities().definition("gbif.dataset");
			assertEquals("gbif.dataset", dataset.table());
			EntityDefinition export = gbif.entities().definition("gbif.occurrence_export");
			assertEquals("\"datasetName\"", export.columns().definition(column(export, "datasetName")).name());

			try (DomainTest domainTest = new DomainTest(gbif, UNIT_TEST_USER)) {
				for (EntityDefinition entityDefinition : gbif.entities().definitions()) {
					domainTest.test(entityDefinition.type());
				}
			}

			// without lower case identifiers only the folded names differ
			SchemaDomain upperCase = SchemaDomain.schemaDomain(connection.getMetaData(), "GBIF", SchemaSettings.builder()
							.lowerCaseIdentifiers(false)
							.build());
			EntityDefinition upperCaseOccurrence = upperCase.entities().definition("GBIF.Occurrence");
			assertEquals("GBIF.\"Occurrence\"", upperCaseOccurrence.table());
			column(upperCaseOccurrence, "REMARKS");
			column(upperCaseOccurrence, "verbatimLocality");
			assertEquals("datasetID_FK", foreignKey(upperCaseOccurrence, "GBIF.DATASET").attribute().name());
		}
	}

	@Test
	void sqlite(@TempDir Path directory) throws Exception {
		// SQLite has neither schemas nor catalogs, its driver reporting neither for the table a foreign key references
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("library.db"));
				 Statement statement = connection.createStatement()) {
			statement.execute("create table artist (id integer primary key autoincrement, name text not null)");
			statement.execute("create table album (id integer primary key autoincrement, " +
							"artist_id integer not null references artist (id), title text not null)");
			// the referenced column left out, the primary key
			statement.execute("create table track (id integer primary key autoincrement, " +
							"album_id integer not null references album (id), artist_id integer references artist, title text not null)");
			SchemaDomain library = SchemaDomain.schemaDomain(connection.getMetaData(), MetaDataSchema.NO_SCHEMA.name());
			// named after the database file
			assertEquals("library", library.type().name());

			EntityDefinition album = library.entities().definition("album");
			assertEquals("album", album.table());
			assertEquals("artist_id_fk", foreignKey(album, "artist").attribute().name());
			EntityDefinition track = library.entities().definition("track");
			assertEquals("album_id_fk", foreignKey(track, "album").attribute().name());
			assertEquals("artist_id_fk", foreignKey(track, "artist").attribute().name());
		}
	}

	@Test
	void databaseName() {
		assertEquals("library", SchemaDomain.databaseName("jdbc:sqlite:/data/library.db"));
		assertEquals("library", SchemaDomain.databaseName("jdbc:sqlite:library.db"));
		assertEquals("library", SchemaDomain.databaseName("jdbc:sqlite:C:\\data\\library.db"));
		assertEquals("library", SchemaDomain.databaseName("jdbc:sqlite:/data/library"));
		assertEquals("MusicLibrary", SchemaDomain.databaseName("jdbc:sqlite:/data/MusicLibrary.sqlite3?journal_mode=WAL"));
		// not valid in a java name
		assertEquals("fieldbook_sync", SchemaDomain.databaseName("jdbc:sqlite:file:/data/fieldbook-sync.db?mode=ro"));
		// in memory or temporary
		assertNull(SchemaDomain.databaseName("jdbc:sqlite::memory:"));
		assertNull(SchemaDomain.databaseName("jdbc:sqlite:file::memory:?cache=shared"));
		assertNull(SchemaDomain.databaseName("jdbc:sqlite:"));
		// not starting as a java name
		assertNull(SchemaDomain.databaseName("jdbc:sqlite:/data/2024.db"));
	}

	private static ForeignKeyDefinition foreignKey(EntityDefinition definition, String referencedType) {
		List<ForeignKeyDefinition> foreignKeys = definition.foreignKeys().definitions().stream()
						.filter(foreignKey -> foreignKey.attribute().referencedType().name().equals(referencedType))
						.collect(toList());
		assertEquals(1, foreignKeys.size());

		return foreignKeys.get(0);
	}

	private static Column<?> column(EntityDefinition definition, String name) {
		List<Column<?>> columns = definition.columns().get().stream()
						.filter(column -> column.name().equals(name))
						.collect(toList());
		assertEquals(1, columns.size());

		return columns.get(0);
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
			try (DomainTest domainTest = new DomainTest(chinook, ChinookEntityFactory::new, UNIT_TEST_USER)) {
				for (EntityDefinition entityDefinition : chinook.entities().definitions()) {
					domainTest.test(entityDefinition.type());
				}
			}
		}
	}

	private static final class ChinookEntityFactory extends AbstractEntityFactory {

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
			try (DomainTest domainTest = new DomainTest(world, WorldEntityFactory::new, UNIT_TEST_USER)) {
				for (EntityDefinition entityDefinition : world.entities().definitions()) {
					domainTest.test(entityDefinition.type());
				}
			}
		}
	}

	private static final class WorldEntityFactory extends AbstractEntityFactory {

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
