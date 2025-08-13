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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Generates primary key values for entities on insert.
 * <p>
 * KeyGenerators fall into two categories:
 * <ol>
 *   <li><strong>Pre-insert generators</strong> - Fetch or generate the primary key value before the row is inserted
 *   <li><strong>Post-insert generators</strong> - The database automatically sets the primary key value on insert (identity columns, triggers)
 * </ol>
 * <p>
 * Implementations should override either {@code beforeInsert()} or {@code afterInsert()}:
 * <ul>
 *   <li>If {@link #inserted()} returns true, the primary key value is included in the insert statement
 *       and {@link #beforeInsert(Entity, DatabaseConnection)} should be used
 *   <li>If {@link #inserted()} returns false, the database generates the primary key automatically
 *       and {@link #afterInsert(Entity, DatabaseConnection, Statement)} should be used
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
 *         Customer.TYPE.define(
 *                 Customer.ID.define()
 *                     .primaryKey()
 *                     .keyGenerator(KeyGenerator.identity()))
 *             .build();
 *
 *         // Sequence-based key generation (Oracle, PostgreSQL)
 *         Product.TYPE.define(
 *                 Product.ID.define()
 *                     .primaryKey()
 *                     .keyGenerator(KeyGenerator.sequence("product_seq")))
 *             .build();
 *
 *         // Custom query-based key generation
 *         Order.TYPE.define(
 *                 Order.ID.define()
 *                     .primaryKey()
 *                     .keyGenerator(KeyGenerator.queried("SELECT 'ORD-' || NEXT VALUE FOR order_seq")))
 *             .build();
 *     }
 * }
 *
 * // Custom key generator implementation
 * public class UUIDKeyGenerator implements KeyGenerator {
 *
 *     @Override
 *     public void beforeInsert(Entity entity, DatabaseConnection connection) {
 *         // Only generate if not already set
 *         if (entity.primaryKey().isNull()) {
 *             String uuid = UUID.randomUUID().toString();
 *             entity.set(MyEntity.ID, uuid);
 *         }
 *     }
 *
 *     @Override
 *     public boolean inserted() {
 *         return true; // Include generated key in INSERT
 *     }
 * }
 *}
 * @see #sequence(String)
 * @see #identity()
 * @see #queried(String)
 * @see #automatic(String)
 */
public interface KeyGenerator {

	/**
	 * The default implementation returns true.
	 * @return true if the primary key value should be included in the
	 * insert query when entities using this key generator is inserted
	 */
	default boolean inserted() {
		return true;
	}

	/**
	 * Prepares the given entity for insert, that is, generates and fetches any required primary key values
	 * and populates the entity's primary key.
	 * The default implementation does nothing, override to implement.
	 * @param entity the entity about to be inserted
	 * @param connection the connection to use
	 * @throws SQLException in case of an exception
	 */
	default void beforeInsert(Entity entity, DatabaseConnection connection) throws SQLException {/*for overriding*/}

	/**
	 * Prepares the given entity after insert, that is, fetches automatically generated primary
	 * key values and populates the entity's primary key.
	 * The default implementation does nothing, override to implement.
	 * @param entity the inserted entity
	 * @param connection the connection to use
	 * @param insertStatement the insert statement
	 * @throws SQLException in case of an exception
	 */
	default void afterInsert(Entity entity, DatabaseConnection connection, Statement insertStatement) throws SQLException {/*for overriding*/}

	/**
	 * Specifies whether the insert statement should return the primary key column values via the resulting
	 * {@link Statement#getGeneratedKeys()} resultSet, accessible in {@link #afterInsert(Entity, DatabaseConnection, Statement)}.
	 * The default implementation returns false.
	 * @return true if the primary key column values should be returned via the insert statement resultSet
	 * @see Statement#getGeneratedKeys()
	 * @see Statement#RETURN_GENERATED_KEYS
	 * @see Statement#NO_GENERATED_KEYS
	 * @see java.sql.Connection#prepareStatement(String, int)
	 */
	default boolean generatedKeys() {
		return false;
	}

	/**
	 * Instantiates a primary key generator which fetches primary key values from a sequence prior to insert.
	 * Note that if the primary key value of the entity being inserted is already populated this key
	 * generator does nothing, that is, it does not overwrite a manually set primary key value.
	 * {@snippet :
	 * // Oracle or PostgreSQL sequence
	 * Product.TYPE.define(
	 *         Product.ID.define()
	 *             .primaryKey()
	 *             .keyGenerator(KeyGenerator.sequence("product_seq")))
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
	 *
	 * // Manual key override (sequence not used)
	 * Entity productWithManualId = entities.entity(Product.TYPE)
	 *     .with(Product.ID, 999L) // Explicitly set
	 *     .with(Product.NAME, "Special Product")
	 *     .build();
	 *
	 * connection.insert(productWithManualId); // Uses ID 999, not sequence
	 *}
	 * @param sequenceName the sequence name
	 * @return a sequence based primary key generator
	 */
	static KeyGenerator sequence(String sequenceName) {
		return new SequenceKeyGenerator(sequenceName);
	}

	/**
	 * Instantiates a primary key generator which fetches primary key values using the given query prior to insert.
	 * Note that if the primary key value of the entity being inserted is already populated this key
	 * generator does nothing, that is, it does not overwrite a manually set primary key value.
	 * {@snippet :
	 * // Custom query-based key generation
	 * Order.TYPE.define(
	 *         Order.ID.define()
	 *             .primaryKey()
	 *             .keyGenerator(KeyGenerator.queried("SELECT 'ORD-' || NEXT VALUE FOR order_seq")))
	 *     .build();
	 *
	 * // Another example with date-based keys
	 * Invoice.TYPE.define(
	 *         Invoice.ID.define()
	 *             .primaryKey()
	 *             .keyGenerator(KeyGenerator.queried(
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
	 * @param query a query for retrieving the primary key value
	 * @return a query based primary key generator
	 */
	static KeyGenerator queried(String query) {
		return new QueryKeyGenerator(query);
	}

	/**
	 * Instantiates a primary key generator which fetches automatically incremented primary key values after insert.
	 * @param valueSource the value source, whether a sequence or a table name
	 * @return an auto-increment based primary key generator
	 */
	static KeyGenerator automatic(String valueSource) {
		return new AutomaticKeyGenerator(valueSource);
	}

	/**
	 * Returns a primary key generator based on an IDENTITY type column.
	 * {@snippet :
	 * // SQL Server, MySQL auto-increment, or similar
	 * Customer.TYPE.define(
	 *         Customer.ID.define()
	 *             .primaryKey()
	 *             .keyGenerator(KeyGenerator.identity()))
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
	 * @return a generated primary key generator
	 * @see Statement#getGeneratedKeys()
	 */
	static Identity identity() {
		return IdentityKeyGenerator.INSTANCE;
	}

	/**
	 * Marker interface indicating that a key generator is based on an identity column.
	 */
	interface Identity extends KeyGenerator {}
}
