/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.common.db.connection.DatabaseConnection;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Generates primary key values for entities on insert.
 * KeyGenerators fall into two categories, one which fetches or generates the primary key value
 * before the record is inserted and one where the underlying database automatically sets the primary
 * key value on insert, i.e. with a table trigger or identity columns.
 * Implementations should override either {@code beforeInsert()} or {@code afterInsert()}.
 * If {@link #isInserted()} returns true the primary key value should be included in the
 * insert statement, meaning that {@link #beforeInsert(Entity, EntityDefinition, DatabaseConnection)} should be used
 * to populate the entity's primary key values.
 * If {@link #isInserted()} returns false then it is assumed that the database generates the primary key
 * values automatically, meaning that {@code afterInsert()} should be used to fetch the generated primary
 * key value and populate the entity instance accordingly.
 */
public interface KeyGenerator {

  /**
   * The default implementation returns true.
   * @return true if the primary key value should be included in the
   * insert query when entities of this type are inserted
   */
  default boolean isInserted() {
    return true;
  }

  /**
   * Prepares the given entity for insert, that is, generates and fetches any required primary key values
   * and populates the entity's primary key.
   * The default implementation does nothing, override to implement.
   * @param entity the entity about to be inserted
   * @param definition the definition of the entity about to be inserted
   * @param connection the connection to use
   * @throws SQLException in case of an exception
   */
  default void beforeInsert(final Entity entity, final EntityDefinition definition,
                            final DatabaseConnection connection) throws SQLException {/*for overriding*/}

  /**
   * Prepares the given entity after insert, that is, fetches automatically generated primary
   * key values and populates the entity's primary key.
   * The default implementation does nothing, override to implement.
   * @param entity the inserted entity
   * @param definition the definition of the inserted entity
   * @param connection the connection to use
   * @param insertStatement the insert statement
   * @throws SQLException in case of an exception
   */
  default void afterInsert(final Entity entity, final EntityDefinition definition, final DatabaseConnection connection,
                           final Statement insertStatement) throws SQLException {/*for overriding*/}

  /**
   * Specifies whether the insert statement should return the primary key column values via the resulting
   * {@link Statement#getGeneratedKeys()} resultSet, accessible in {@link #afterInsert(Entity, EntityDefinition, DatabaseConnection, Statement)}.
   * The default implementation returns false.
   * @return true if the primary key column values should be returned via the insert statement resultSet
   * @see Statement#getGeneratedKeys()
   * @see java.sql.Connection#prepareStatement(String, String[])
   */
  default boolean returnGeneratedKeys() {
    return false;
  }
}
