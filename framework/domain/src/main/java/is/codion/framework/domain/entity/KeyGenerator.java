/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Generates primary key values for entities on insert.
 * KeyGenerators fall into two categories, one which fetches or generates the primary key value
 * before the row is inserted and one where the underlying database automatically sets the primary
 * key value on insert, i.e. with a table trigger or identity columns.
 * Implementations should override either {@code beforeInsert()} or {@code afterInsert()}.
 * If {@link #inserted()} returns true the primary key value should be included in the
 * insert statement, meaning that {@link #beforeInsert(Entity, DatabaseConnection)} should be used
 * to populate the entity's primary key values.
 * If {@link #inserted()} returns false then it is assumed that the database generates the primary key
 * values automatically, meaning that {@code afterInsert()} should be used to fetch the generated primary
 * key value and populate the entity instance accordingly.
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
   * @see java.sql.Connection#prepareStatement(String, int)
   */
  default boolean returnGeneratedKeys() {
    return false;
  }

  /**
   * Instantiates a primary key generator which fetches primary key values from a sequence prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
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
   * @return a generated primary key generator
   * @see Statement#getGeneratedKeys()
   */
  static KeyGenerator identity() {
    return IdentityKeyGenerator.INSTANCE;
  }
}
