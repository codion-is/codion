/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.DatabaseConnection;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Generates primary key values for entities on insert.
 * KeyGenerators fall into two categories, one in which the primary key value is
 * fetched or generated before the record is inserted and one where the underlying database
 * automatically sets the primary key value on insert, f.ex. with a table trigger or identity columns.
 * Implementations should override either {@code beforeInsert()} or {@code afterInsert()}.
 * {@code isAutoIncrement()} returns true if the database generates primary key values automatically,
 * this implies that {@code afterInsert()} should be used, fetching the generated primary key value
 * and updating the entity instance accordingly.
 */
public interface KeyGenerator {

  /**
   * The default implementation returns true.
   * @return true if the primary key should be included when entities of this type are inserted
   */
  default boolean isInserted() {
    return true;
  }

  /**
   * Prepares the given entity for insert, that is, generates and fetches any required primary key values
   * and populates the entitys primary key.
   * The default version does nothing, override to implement.
   * @param entity the entity to prepare
   * @param connection the connection to use
   * @throws SQLException in case of an exception
   */
  default void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {/*for overriding*/}

  /**
   * Prepares the given entity after insert, that is, fetches automatically generated primary
   * key values and populates the entitys primary key.
   * The default version does nothing, override to implement.
   * @param entity the entity to prepare
   * @param connection the connection to use
   * @param insertStatement the insert statement
   * @throws SQLException in case of an exception
   */
  default void afterInsert(final Entity entity, final DatabaseConnection connection,
                           final Statement insertStatement) throws SQLException {/*for overriding*/}

  /**
   * Specifies whether the insert statement should return the primary key column values via the resulting
   * {@link Statement#getGeneratedKeys()} resultSet, accessible in {@link #afterInsert(Entity, DatabaseConnection, Statement)}.
   * The default implementation returns false.
   * @return true if the primary key column values should be returned via the insert statement resultSet
   * @see java.sql.Connection#prepareStatement(String, String[])
   */
  default boolean returnPrimaryKeyValues() {
    return false;
  }
}
