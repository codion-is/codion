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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.TypeReference;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.DefaultColumn.DefaultColumnDefiner;
import is.codion.framework.domain.entity.condition.ColumnConditionFactory;

import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
 * Columns inherit from {@link ColumnConditionFactory} to provide condition creation methods:
 * {@snippet :
 * public class Store extends DefaultDomain {
 *
 *     interface Customer {
 *         EntityType TYPE = DOMAIN.entityType("store.customer");
 *
 *         // Column definitions
 *         Column<Integer> ID = TYPE.integerColumn("id");
 *         Column<String> NAME = TYPE.stringColumn("name");
 *         Column<String> EMAIL = TYPE.stringColumn("email");
 *         Column<LocalDate> BIRTH_DATE = TYPE.localDateColumn("birth_date");
 *         Column<Boolean> ACTIVE = TYPE.booleanColumn("active");
 *         Column<BigDecimal> CREDIT_LIMIT = TYPE.bigDecimalColumn("credit_limit");
 *     }
 *
 *     void defineCustomer() {
 *         Customer.TYPE.as(
 *                 Customer.ID.as()
 *                     .primaryKey()
 *                     .generator(Generator.identity()),
 *                 Customer.NAME.as()
 *                     .column()
 *                     .nullable(false)
 *                     .maximumLength(100)
 *                     .caption("Customer Name"),
 *                 Customer.EMAIL.as()
 *                     .column()
 *                     .nullable(false)
 *                     .maximumLength(255)
 *                     .caption("Email Address"),
 *                 Customer.BIRTH_DATE.as()
 *                     .column()
 *                     .nullable(true)
 *                     .caption("Date of Birth"),
 *                 Customer.ACTIVE.as()
 *                     .column()
 *                     .nullable(false)
 *                     .defaultValue(true)
 *                     .caption("Active"),
 *                 Customer.CREDIT_LIMIT.as()
 *                     .column()
 *                     .nullable(true)
 *                     .minimum(BigDecimal.ZERO)
 *                     .maximum(new BigDecimal("50000"))
 *                     .caption("Credit Limit"))
 *             .build();
 *     }
 * }
 *
 * // Query condition usage (inherited from ColumnCondition.Factory)
 * List<Entity> activeCustomers = connection.select(
 *     Customer.ACTIVE.equalTo(true));
 *
 * List<Entity> customersByName = connection.select(
 *     Customer.NAME.like("John%"));
 *
 * List<Entity> recentCustomers = connection.select(
 *     Customer.BIRTH_DATE.greaterThanOrEqualTo(LocalDate.now().minusYears(25)));
 *
 * List<Entity> highValueCustomers = connection.select(
 *     Customer.CREDIT_LIMIT.greaterThan(new BigDecimal("10000")));
 *
 * // Complex conditions
 * List<Entity> nonLiveAlbums = connection.select(and(
 * 						Album.ARTIST_FK.in(artists),
 * 						Album.TITLE.likeIgnoreCase("%live%")));
 *}
 * @param <T> the column value type
 * @see ColumnConditionFactory
 * @see #as()
 */
public sealed interface Column<T> extends Attribute<T>, ColumnConditionFactory<T> permits DefaultColumn {

	/**
	 * @return a {@link ColumnDefiner} for this column
	 */
	ColumnDefiner<T> as();

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
	 * @param valueClass the class representing the column value type
	 * @param <T> the column type
	 * @return a new {@link Column}
	 */
	static <T> Column<T> column(EntityType entityType, String name, Class<T> valueClass) {
		return new DefaultColumn<>(name, valueClass, entityType);
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
		 * Creates a new {@link ColumnDefinition.Builder} instance.
		 * @param <B> the builder type
		 * @param template the column template
		 * @return a new {@link ColumnDefinition.Builder}
		 */
		<B extends ColumnDefinition.Builder<T, B>> ColumnDefinition.Builder<T, B> column(ColumnTemplate<T> template);

		/**
		 * Returns a new {@link ColumnDefinition.Builder} instance, with the primary key index 0.
		 * Note that this renders this column non-null and non-updatable by default, this can be
		 * reverted by setting it as updatable and/or nullable after defining a primary key column.
		 * {@snippet :
		 * Employee.ID.as()
		 *   .primaryKey()
		 *   .nullable(true)
		 *   .updatable(true)
		 *}
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
		 * {@snippet :
		 * CountryLanguage.COUNTRY_CODE.as()
		 *   .primaryKey(0)
		 *   .updatable(true)
		 * CountryLanguage.LANGUAGE.as()
		 *   .primaryKey(1)
		 *   .updatable(true)
		 *}
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
	 * Gets a single value from a {@link ResultSet}.
	 * @param <C> the column value type
	 */
	interface GetValue<C> {

		/**
		 * Fetches a single value from a ResultSet
		 * @param resultSet the ResultSet
		 * @param index the index of the column to fetch
		 * @return a single value fetched from the given ResultSet
		 * @throws java.sql.SQLException in case of an exception
		 */
		@Nullable C get(ResultSet resultSet, int index) throws SQLException;
	}

	/**
	 * Sets a parameter value in a {@link PreparedStatement}
	 * @param <C> the column value type
	 */
	interface SetParameter<C> {

		/**
		 * Sets a parameter value in a {@link PreparedStatement}
		 * @param statement the statement
		 * @param index the parameter index
		 * @param value the value to set, may be null
		 * @throws SQLException in case of an exception
		 */
		void set(PreparedStatement statement, int index, @Nullable C value) throws SQLException;
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
	 *   <li><strong>Pre-insert generators</strong> - Fetch or generate the primary key value before the row is inserted
	 *   <li><strong>Post-insert generators</strong> - The database automatically sets the primary key value on insert (identity columns, triggers)
	 * </ol>
	 * <p>
	 * Implementations should override either {@code beforeInsert()} or {@code afterInsert()}:
	 * <ul>
	 *   <li>If {@link #inserted()} returns true, the primary key value is included in the insert statement
	 *       and {@link #beforeInsert(Entity, Column, Database, Connection)} should be used
	 *   <li>If {@link #inserted()} returns false, the database generates the primary key automatically
	 *       and {@link #afterInsert(Entity, Column, Database, Statement)} should be used
	 * </ul>
	 * <p>
	 * Common key generator types and usage patterns:
	 * {@snippet :
	 * public class Store extends DefaultDomain {
	 *
	 *     interface Customer {
	 *         EntityType TYPE = DOMAIN.entityType("store.customer");
	 *         Column<Integer> ID = TYPE.integerColumn("id");
	 *     }
	 *
	 *     interface Product {
	 *         EntityType TYPE = DOMAIN.entityType("store.product");
	 *         Column<Long> ID = TYPE.longColumn("id");
	 *     }
	 *
	 *     interface Order {
	 *         EntityType TYPE = DOMAIN.entityType("store.order");
	 *         Column<String> ID = TYPE.stringColumn("id");
	 *     }
	 *
	 *     void defineEntities() {
	 *         // Identity column (database auto-increment)
	 *         Customer.TYPE.as(
	 *                 Customer.ID.as()
	 *                     .primaryKey()
	 *                     .generator(Generator.identity()))
	 *             .build();
	 *
	 *         // Sequence-based key generation (Oracle, PostgreSQL)
	 *         Product.TYPE.as(
	 *                 Product.ID.as()
	 *                     .primaryKey()
	 *                     .generator(Generator.sequence("product_seq")))
	 *             .build();
	 *
	 *         // Custom query-based key generation
	 *         Order.TYPE.as(
	 *                 Order.ID.as()
	 *                     .primaryKey()
	 *                     .generator(Generator.queried("SELECT 'ORD-' || NEXT VALUE FOR order_seq")))
	 *             .build();
	 *     }
	 * }
	 *
	 * // Custom key generator implementation
	 * public class UUIDGenerator implements Generator<String> {
	 *
	 *     @Override
	 *     public void beforeInsert(Entity entity, Column<String> column, Database database, Connection connection) {
	 *         // Only generate if not already set
	 *         if (entity.primaryKey().isNull()) {
	 *             String uuid = UUID.randomUUID().toString();
	 *             entity.set(column, uuid);
	 *         }
	 *     }
	 *
	 *     @Override
	 *     public boolean inserted() {
	 *         return true; // Include generated key in INSERT
	 *     }
	 * }
	 *}
	 * @param <T> the generated column type
	 * @see #sequence(String)
	 * @see #identity()
	 * @see #queried(String)
	 * @see #automatic(String)
	 */
	interface Generator<T> {

		/**
		 * The default implementation returns true.
		 * @return true if the key value should be included in the
		 * insert query when entities using this key generator is inserted
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
		 * Prepares the given entity after insert, that is, fetches automatically values
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
		 * Indicates an identity column based key generator.
		 * @param <T> the column type
		 */
		sealed interface Identity<T> extends Generator<T> permits IdentityGenerator {}

		/**
		 * Instantiates a primary key generator which fetches primary key values from a sequence prior to insert.
		 * {@snippet :
		 * // Oracle or PostgreSQL sequence
		 * Product.TYPE.as(
		 *         Product.ID.as()
		 *             .primaryKey()
		 *             .generator(Generator.sequence("product_seq")))
		 *     .build();
		 *
		 * // Usage - key is generated automatically
		 * Entity product = entities.entity(Product.TYPE)
		 *     .with(Product.NAME, "Laptop")
		 *     .with(Product.PRICE, new BigDecimal("999.99"))
		 *     .build();
		 *
		 * // Primary key will be fetched from sequence before insert
		 * connection.insert(product);
		 * Long generatedId = product.get(Product.ID); // e.g., 123
		 *}
		 * @param <T> the generated column type
		 * @param sequenceName the sequence name
		 * @return a sequence based primary key generator
		 */
		static <T> Generator<T> sequence(String sequenceName) {
			return new SequenceGenerator<>(sequenceName);
		}

		/**
		 * Instantiates a primary key generator which fetches primary key values using the given query prior to insert.
		 * {@snippet :
		 * // Custom query-based key generation
		 * Order.TYPE.as(
		 *         Order.ID.as()
		 *             .primaryKey()
		 *             .generator(Generator.queried("SELECT 'ORD-' || NEXT VALUE FOR order_seq")))
		 *     .build();
		 *
		 * // Another example with date-based keys
		 * Invoice.TYPE.as(
		 *         Invoice.ID.as()
		 *             .primaryKey()
		 *             .generator(Generator.queried(
		 *                 "SELECT FORMAT(GETDATE(), 'yyyyMMdd') + '-' + RIGHT('0000' + CAST(NEXT VALUE FOR invoice_seq AS VARCHAR), 4)")))
		 *     .build();
		 *
		 * // Usage
		 * Entity order = entities.entity(Order.TYPE)
		 *     .with(Order.CUSTOMER_FK, customer)
		 *     .with(Order.TOTAL, new BigDecimal("150.00"))
		 *     .build();
		 *
		 * connection.insert(order);
		 * String generatedId = order.get(Order.ID); // e.g., "ORD-12345"
		 *}
		 * @param <T> the generated column type
		 * @param query a query for retrieving the column value
		 * @return a query based column generator
		 */
		static <T> Generator<T> queried(String query) {
			return new QueryGenerator<>(query);
		}

		/**
		 * Instantiates a primary key generator which fetches automatically incremented column values after insert.
		 * @param <T> the generated column type
		 * @param valueSource the value source, whether a sequence or a table name
		 * @return an auto-increment based column value generator
		 */
		static <T> Generator<T> automatic(String valueSource) {
			return new AutomaticGenerator<>(valueSource);
		}

		/**
		 * Returns a column value generator based on an IDENTITY type column.
		 * {@snippet :
		 * // SQL Server, MySQL auto-increment, or similar
		 * Customer.TYPE.as(
		 *         Customer.ID.as()
		 *             .primaryKey()
		 *             .generator(Generator.identity()))
		 *     .build();
		 *
		 * // Usage - database generates the key
		 * Entity customer = entities.entity(Customer.TYPE)
		 *     .with(Customer.NAME, "John Doe")
		 *     .with(Customer.EMAIL, "john@example.com")
		 *     .build();
		 *
		 * // Primary key is null before insert
		 * Integer idBeforeInsert = customer.get(Customer.ID); // null
		 *
		 * // Database auto-generates the key during insert
		 * connection.insert(customer);
		 *
		 * // Primary key is now populated from database
		 * Integer generatedId = customer.get(Customer.ID); // e.g., 42
		 *}
		 * @param <T> the generated column type
		 * @return an identity based generated column value generator
		 * @see Statement#getGeneratedKeys()
		 */
		static <T> Identity<T> identity() {
			return new IdentityGenerator<>();
		}
	}
}
