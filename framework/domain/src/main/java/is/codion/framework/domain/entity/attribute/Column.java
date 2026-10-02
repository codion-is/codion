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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.TypeReference;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.DefaultColumn.DefaultColumnDefiner;
import is.codion.framework.domain.entity.condition.ColumnConditions;

import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static java.util.Objects.requireNonNull;

/**
 * An {@link Attribute} representing a table column.
 * <p>
 * Columns are attributes that map directly to database table columns, providing type-safe
 * access to column values and enabling query condition creation. They extend the base
 * {@link Attribute} interface with column-specific functionality for database operations.
 * <p>
 * Columns are declared in the domain API, and defined in the domain model:
 * {@snippet class = is.codion.demos.chinook.javadoc.ColumnSnippets region = usage :
 * interface Track {
 * 	EntityType TYPE = DOMAIN.entityType("chinook.track");
 *
 * 	// Column definitions
 * 	Column<Long> ID = TYPE.longColumn("id");
 * 	Column<String> NAME = TYPE.stringColumn("name");
 * 	Column<String> COMPOSER = TYPE.stringColumn("composer");
 * 	Column<Integer> MILLISECONDS = TYPE.integerColumn("milliseconds");
 * 	Column<Integer> RATING = TYPE.integerColumn("rating");
 * 	Column<BigDecimal> UNITPRICE = TYPE.bigDecimalColumn("unitprice");
 * }
 *
 * EntityDefinition track() {
 * 	return Track.TYPE.as()
 * 					.attributes(
 * 									Track.ID.as()
 * 													.primaryKey()
 * 													.generator(Generator.identity()),
 * 									Track.NAME.as()
 * 													.column()
 * 													.caption("Name")
 * 													.nullable(false)
 * 													.maximumLength(200),
 * 									Track.COMPOSER.as()
 * 													.column()
 * 													.caption("Composer")
 * 													.maximumLength(220),
 * 									Track.MILLISECONDS.as()
 * 													.column()
 * 													.caption("Duration")
 * 													.nullable(false),
 * 									Track.RATING.as()
 * 													.column()
 * 													.caption("Rating")
 * 													.nullable(false)
 * 													.defaultValue(5)
 * 													.range(1, 10),
 * 									Track.UNITPRICE.as()
 * 													.column()
 * 													.caption("Price")
 * 													.nullable(false)
 * 													.minimum(0)
 * 													.fractionDigits(2))
 * 					.build();
 * }}
 * <p>
 * Columns inherit from {@link ColumnConditions} to provide condition creation methods:
 * {@snippet class = is.codion.demos.chinook.javadoc.ColumnSnippets region = conditions :
 * // Query condition usage (inherited from ColumnConditions)
 * List<Entity> bachTracks = connection.select(
 * 				Track.COMPOSER.equalTo("Johann Sebastian Bach"));
 *
 * List<Entity> loveSongs = connection.select(
 * 				Track.NAME.likeIgnoreCase("%love%"));
 *
 * List<Entity> longTracks = connection.select(
 * 				Track.MILLISECONDS.greaterThanOrEqualTo(600_000));
 *
 * List<Entity> expensiveTracks = connection.select(
 * 				Track.UNITPRICE.greaterThan(BigDecimal.valueOf(0.99)));
 *
 * // Complex conditions
 * List<Entity> liveAlbums = connection.select(and(
 * 				Album.ARTIST_FK.in(artists),
 * 				Album.TITLE.likeIgnoreCase("%live%")));}
 * @param <T> the column value type
 * @see ColumnConditions
 * @see #as()
 */
public sealed interface Column<T> extends Attribute<T>, ColumnConditions<T> permits DefaultColumn {

	/**
	 * @return a {@link ColumnDefiner} for this column
	 */
	ColumnDefiner<T> as();

	/**
	 * Returns a {@link ColumnDefinition.Builder} for this column, configured by the given template.
	 * {@snippet class = is.codion.demos.chinook.javadoc.ColumnSnippets region = template :
	 * Customer.LASTNAME.as(REQUIRED_SEARCHABLE)
	 * 				.maximumLength(20);}
	 * <p>The template is applied first, any subsequent configuration overriding it.
	 * <p>Note that a column has {@link Attribute#as(AttributeTemplate)} as well, so the template
	 * must be typed, an inline lambda being ambiguous between the two.
	 * @param template the column template
	 * @return a {@link ColumnDefinition.Builder} configured by the given template
	 */
	ColumnDefinition.Builder<T, ?> as(ColumnTemplate<T> template);

	/**
	 * Creates a new {@link Column}, associated with the given entityType.
	 * @param entityType the entityType owning this column
	 * @param name the column name
	 * @param typeReference the {@link TypeReference} representing the column value type
	 * @param <T> the column type
	 * @return a new {@link Column}
	 */
	static <T> Column<T> column(EntityType entityType, String name, TypeReference<T> typeReference) {
		return new DefaultColumn<>(name, requireNonNull(typeReference).rawType(), entityType);
	}

	/**
	 * Creates a new {@link Column}, associated with the given entityType.
	 * @param entityType the entityType owning this column
	 * @param name the column name
	 * @param type the class representing the column value type
	 * @param <T> the column type
	 * @return a new {@link Column}
	 */
	static <T> Column<T> column(EntityType entityType, String name, Class<T> type) {
		return new DefaultColumn<>(name, type, entityType);
	}

	/**
	 * Provides {@link ColumnDefinition.Builder} instances.
	 * @param <T> the column type
	 */
	sealed interface ColumnDefiner<T> extends AttributeDefiner<T> permits DefaultColumnDefiner {

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance.
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> column();

		/**
		 * Returns a new {@link ColumnDefinition.Builder} instance, with the primary key index 0.
		 * Note that this renders this column non-null and non-updatable by default, this can be
		 * reverted by setting it as updatable and/or nullable after defining a primary key column.
		 * {@snippet class = is.codion.demos.world.javadoc.ColumnSnippets region = primaryKey :
		 * Country.CODE.as()
		 * 				.primaryKey()
		 * 				.caption("Code")
		 * 				.updatable(true);}
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder} with primary key index 0
		 * @see ColumnDefinition.Builder#nullable(boolean)
		 * @see ColumnDefinition.Builder#updatable(boolean)
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey();

		/**
		 * Returns a new {@link ColumnDefinition.Builder} instance, with the given primary key index.
		 * Note that this renders this column non-null and non-updatable by default, this can be
		 * reverted by setting it as updatable and/or nullable after defining a primary key column.
		 * {@snippet class = is.codion.demos.world.javadoc.ColumnSnippets region = primaryKeyIndex :
		 * CountryLanguage.TYPE.as()
		 * 				.attributes(
		 * 								CountryLanguage.COUNTRY_CODE.as()
		 * 												.primaryKey(0)
		 * 												.updatable(true),
		 * 								CountryLanguage.LANGUAGE.as()
		 * 												.primaryKey(1)
		 * 												.caption("Language")
		 * 												.updatable(true))
		 * 				.build();}
		 * @param index the zero-based index of this column in the primary key
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder} with the given primary key index
		 * @throws IllegalArgumentException in case index is a negative number
		 * @see ColumnDefinition.Builder#nullable(boolean)
		 * @see ColumnDefinition.Builder#updatable(boolean)
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> primaryKey(int index);

		/**
		 * Creates a new {@link ColumnDefinition.Builder} instance, based on a subquery.
		 * @param subquery the sql query
		 * @param <B> the builder type
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> subquery(String subquery);
	}

	/**
	 * Converts to and from SQL values, such as integers being used to represent booleans in a database.
	 * <p> By default, a {@link Converter} is not expected to handle null values, with null values automatically converted to/from null column values.
	 * <p> If a {@link Converter} needs to handle null values as well as non-null values {@link #handlesNull()} must be overridden to return true.
	 * @param <T> the type of the value
	 * @param <C> the type of the underlying column
	 */
	interface Converter<T, C> {

		/**
		 * Unless a Converter handles null, null values are automatically converted to null column values.
		 * @return true if this Converter handles the null value, default false
		 */
		default boolean handlesNull() {
			return false;
		}

		/**
		 * Translates the given value into a sql value, usually this is not required
		 * but for certain types this may be necessary, such as boolean values where
		 * the values are represented by a non-boolean data type in the underlying database
		 * @param value the value to translate, not null unless {@link #handlesNull()} is overridden
		 * @param statement the statement using the value
		 * @return the sql value used to represent the given value
		 * @throws SQLException in case of an exception
		 */
		@Nullable C toColumn(@Nullable T value, Statement statement) throws SQLException;

		/**
		 * Translates the given sql column value into a column value.
		 * @param columnValue the sql value to translate from, not null unless {@link #handlesNull()} is overridden
		 * @return the value of sql {@code columnValue}
		 * @throws SQLException in case of an exception
		 */
		@Nullable T fromColumn(@Nullable C columnValue) throws SQLException;
	}

	/**
	 * Generates column values for entities on insert.
	 * <p>
	 * Generators fall into two categories:
	 * <ol>
	 *   <li><strong>Pre-insert generators</strong> - Fetch or generate the column value before the row is inserted
	 *   <li><strong>Post-insert generators</strong> - The database automatically sets the column value on insert (identity columns, triggers)
	 * </ol>
	 * <p>
	 * Implementations should override either {@code beforeInsert()} or {@code afterInsert()}:
	 * <ul>
	 *   <li>If {@link #inserted()} returns true, the generated value is included in the insert statement
	 *       and {@link #beforeInsert(Entity, Column, Database, Connection)} should be used
	 *   <li>If {@link #inserted()} returns false, the database generates the value automatically
	 *       and {@link #afterInsert(Entity, Column, Database, Statement)} should be used
	 * </ul>
	 * <p>
	 * The common generators are provided by {@link #identity()}, {@link #sequence(String)} and {@link #queried(String)}.
	 * A custom generator, setting a random UUID as the key:
	 * {@snippet class = is.codion.demos.chinook.javadoc.ColumnSnippets region = generator :
	 * class UUIDGenerator implements Generator<String> {
	 *
	 * 	@Override
	 * 	public void beforeInsert(Entity entity, Column<String> column, Database database, Connection connection) {
	 * 		// Only generate if not already set
	 * 		if (!entity.present(column)) {
	 * 			entity.set(column, UUID.randomUUID().toString());
	 * 		}
	 * 	}
	 * }}
	 * @param <T> the generated column type
	 * @see #sequence(String)
	 * @see #identity()
	 * @see #queried(String)
	 * @see #automatic(String)
	 */
	interface Generator<T> {

		/**
		 * The default implementation returns true.
		 * @return true if the generated value should be included in the
		 * insert query when entities using this generator are inserted
		 */
		default boolean inserted() {
			return true;
		}

		/**
		 * Prepares the given entity for insert, that is, generates and fetches any required values
		 * and populates the column value in the entity.
		 * The default implementation does nothing, override to implement.
		 * @param entity the entity about to be inserted
		 * @param column the column which value is being generated
		 * @param database the database
		 * @param connection the connection to use
		 * @throws SQLException in case of an exception
		 */
		default void beforeInsert(Entity entity, Column<T> column, Database database, Connection connection) throws SQLException {/*for overriding*/}

		/**
		 * Prepares the given entity after insert, that is, fetches automatically generated values
		 * and populates the column value in the entity.
		 * The default implementation does nothing, override to implement.
		 * @param entity the inserted entity
		 * @param column the column which value is being generated
		 * @param database the database
		 * @param statement the insert statement
		 * @throws SQLException in case of an exception
		 */
		default void afterInsert(Entity entity, Column<T> column, Database database, Statement statement) throws SQLException {/*for overriding*/}

		/**
		 * Specifies whether this {@link Generator} relies on the insert statement to return the generated column values via the resulting
		 * {@link Statement#getGeneratedKeys()} resultSet, accessible in {@link #afterInsert(Entity, Column, Database, Statement)}.
		 * The default implementation returns false.
		 * @return true if the generated column values should be returned via the insert statement resultSet
		 * @see Statement#getGeneratedKeys()
		 * @see Statement#RETURN_GENERATED_KEYS
		 * @see Statement#NO_GENERATED_KEYS
		 * @see java.sql.Connection#prepareStatement(String, int)
		 */
		default boolean generatedKeys() {
			return false;
		}

		/**
		 * Indicates an identity column based generator.
		 * @param <T> the column type
		 */
		sealed interface Identity<T> extends Generator<T> permits IdentityGenerator {}

		/**
		 * Instantiates a generator which fetches column values from a sequence prior to insert.
		 * {@snippet class = is.codion.demos.world.javadoc.ColumnSnippets region = sequence :
		 * // Oracle or PostgreSQL sequence
		 * City.TYPE.as()
		 * 				.attributes(
		 * 								City.ID.as()
		 * 												.primaryKey()
		 * 												.generator(Generator.sequence("world.city_seq")))
		 * 				.build();
		 *
		 * // Usage - the key is fetched from the sequence before insert
		 * Entity city = entities.entity(City.TYPE)
		 * 				.with(City.NAME, "Akureyri")
		 * 				.with(City.COUNTRY_FK, iceland)
		 * 				.with(City.POPULATION, 20_000)
		 * 				.build();
		 *
		 * Entity.Key key = connection.insert(city);
		 * Integer generatedId = key.get(City.ID);}
		 * @param <T> the generated column type
		 * @param sequenceName the sequence name
		 * @return a sequence based generator
		 */
		static <T> Generator<T> sequence(String sequenceName) {
			return new SequenceGenerator<>(sequenceName);
		}

		/**
		 * Instantiates a generator which fetches column values using the given query prior to insert.
		 * {@snippet class = is.codion.demos.chinook.javadoc.ColumnSnippets region = queried :
		 * // Custom query-based key generation, such as a function call
		 * Invoice.TYPE.as()
		 * 				.attributes(
		 * 								Invoice.ID.as()
		 * 												.primaryKey()
		 * 												.generator(Generator.queried("SELECT chinook.next_invoice_id()")))
		 * 				.build();
		 *
		 * // Usage - the key is fetched before insert
		 * Entity invoice = entities.entity(Invoice.TYPE)
		 * 				.with(Invoice.CUSTOMER_FK, customer)
		 * 				.with(Invoice.DATE, LocalDate.now())
		 * 				.build();
		 *
		 * Entity.Key key = connection.insert(invoice);
		 * Long generatedId = key.get(Invoice.ID);}
		 * @param <T> the generated column type
		 * @param query a query for retrieving the column value
		 * @return a query based column generator
		 */
		static <T> Generator<T> queried(String query) {
			return new QueryGenerator<>(query);
		}

		/**
		 * Instantiates a generator which fetches automatically incremented column values after insert.
		 * Note that H2 and SQL Server do not support this, use {@link #identity()} for those.
		 * @param <T> the generated column type
		 * @param valueSource the value source, whether a sequence or a table name
		 * @return an auto-increment based column value generator
		 */
		static <T> Generator<T> automatic(String valueSource) {
			return new AutomaticGenerator<>(valueSource);
		}

		/**
		 * Returns a column value generator based on an IDENTITY type column.
		 * {@snippet class = is.codion.demos.chinook.javadoc.ColumnSnippets region = identity :
		 * // SQL Server, MySQL auto-increment, or similar
		 * Customer.TYPE.as()
		 * 				.attributes(
		 * 								Customer.ID.as()
		 * 												.primaryKey()
		 * 												.generator(Generator.identity()))
		 * 				.build();
		 *
		 * // Usage - the database generates the key on insert
		 * Entity customer = entities.entity(Customer.TYPE)
		 * 				.with(Customer.FIRSTNAME, "John")
		 * 				.with(Customer.LASTNAME, "Doe")
		 * 				.with(Customer.EMAIL, "john@example.com")
		 * 				.build();
		 *
		 * Entity.Key key = connection.insert(customer);
		 * Long generatedId = key.get(Customer.ID);}
		 * @param <T> the generated column type
		 * @return an identity based generated column value generator
		 * @see Statement#getGeneratedKeys()
		 */
		static <T> Identity<T> identity() {
			return new IdentityGenerator<>();
		}
	}
}
