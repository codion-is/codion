/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.User;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.common.model.tools.MethodLogger;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Entity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Defines the database methods the database layer offers.
 */
public interface EntityConnection {

  /**
   * The possible EntityConnection types
   */
  enum Type {
    LOCAL, REMOTE
  }

  /**
   * @param methodLogger the MethodLogger to use
   */
  void setMethodLogger(final MethodLogger methodLogger);

  /**
   * @return the connection type
   */
  Type getType();

  /**
   * @return the user being used by this connection
   */
  User getUser();

  /**
   * @return true if the connection has been established and is valid
   */
  boolean isConnected();

  /**
   * Performs a rollback and disconnects this connection
   */
  void disconnect();

  /**
   * @return true if a transaction is open, false otherwise
   */
  boolean isTransactionOpen();

  /**
   * Begins a transaction on this connection
   * @throws IllegalStateException if a transaction is already open
   */
  void beginTransaction();

  /**
   * Performs a rollback and ends the current transaction
   * @throws IllegalStateException in case a transaction is not open
   */
  void rollbackTransaction();

  /**
   * Performs a commit and ends the current transaction
   * @throws IllegalStateException in case a transaction is not open
   */
  void commitTransaction();

  /**
   * Executes the function with the given id
   * @param functionID the function ID
   * @param arguments the arguments, if any
   * @return the function return arguments
   * @throws DatabaseException in case anything goes wrong during the execution
   */
  List executeFunction(final String functionID, final Object... arguments) throws DatabaseException;

  /**
   * Executes the procedure with the given id
   * @param procedureID the procedure ID
   * @param arguments the arguments, if any
   * @throws DatabaseException in case anything goes wrong during the execution
   */
  void executeProcedure(final String procedureID, final Object... arguments) throws DatabaseException;

  /**
   * Inserts the given entities, returning a list containing the primary keys of the inserted entities
   * in the same order as they were received.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to insert
   * @return the primary key values of the inserted entities
   * @throws DatabaseException in case of a db exception
   */
  List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException;

  /**
   * Updates the given entities according to their properties.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to update
   * @return the updated entities
   * @throws DatabaseException in case of a db exception
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity has been modified or deleted by another user
   */
  List<Entity> update(final List<Entity> entities) throws DatabaseException;

  /**
   * Deletes the entities according to the given primary keys.
   * Performs a commit unless a transaction is open.
   * @param entityKeys the primary keys of the entities to delete
   * @throws DatabaseException in case of a db exception
   */
  void delete(final List<Entity.Key> entityKeys) throws DatabaseException;

  /**
   * Deletes the entities specified by the given condition
   * Performs a commit unless a transaction is open.
   * @param condition the condition specifying the entities to delete
   * @throws DatabaseException in case of a db exception
   */
  void delete(final EntityCondition condition) throws DatabaseException;

  /**
   * Selects ordered and distinct non-null values of the given property, note that the given property
   * must be of type {@link org.jminor.framework.domain.Property.ColumnProperty}.
   * @param propertyID the ID of the property
   * @param condition the condition
   * @return the values in the given column (Property)
   * @throws DatabaseException in case of a db exception
   * @throws IllegalArgumentException in case the given property is not a column based property
   * @throws UnsupportedOperationException in case the entity is based on a select query
   */
  List<Object> selectValues(final String propertyID, final EntityCondition condition) throws DatabaseException;

  /**
   * Selects a single entity
   * @param entityID the entity type
   * @param propertyID the ID of the property to use as a condition
   * @param value the value to use in the condition
   * @return an entity of the type {@code entityID}, having the
   * value of {@code propertyID} as {@code value}
   * @throws DatabaseException in case of a db exception or if many records were found
   * @throws org.jminor.common.db.exception.RecordNotFoundException in case the entity was not found
   */
  Entity selectSingle(final String entityID, final String propertyID, final Object value) throws DatabaseException;

  /**
   * Selects a single entity by key
   * @param key the key of the entity to select
   * @return an entity having the key {@code key}
   * @throws DatabaseException in case of a db exception or if many records were found
   * @throws org.jminor.common.db.exception.RecordNotFoundException in case the entity was not found
   */
  Entity selectSingle(final Entity.Key key) throws DatabaseException;

  /**
   * Selects a single entity according to the specified condition, throws a DatabaseException
   * if the condition results in more than one entity
   * @param condition the condition specifying the entity to select
   * @return the entities according to the given condition
   * @throws DatabaseException in case of a db exception or if many records were found
   * @throws org.jminor.common.db.exception.RecordNotFoundException in case the entity was not found
   */
  Entity selectSingle(final EntitySelectCondition condition) throws DatabaseException;

  /**
   * Returns entities according to {@code keys}
   * @param keys the keys used in the condition
   * @return entities according to {@code keys}
   * @throws DatabaseException in case of a db exception
   */
  List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException;

  /**
   * Selects entities according to the specified condition
   * @param condition the condition specifying which entities to select
   * @return entities according to the given condition
   * @throws DatabaseException in case of a db exception
   */
  List<Entity> selectMany(final EntitySelectCondition condition) throws DatabaseException;

  /**
   * Selects entities according to one property ({@code propertyID}), using {@code values} as a condition
   * @param entityID the entity type
   * @param propertyID the ID of the condition property
   * @param values the property values to use as condition
   * @return entities of the type {@code entityID} according to {@code propertyID} and {@code values}
   * @throws DatabaseException in case of a db exception
   */
  List<Entity> selectMany(final String entityID, final String propertyID, final Object... values) throws DatabaseException;

  /**
   * Returns the entities that depend on the given entities via foreign keys, mapped to corresponding entityIDs
   * @param entities the entities for which to retrieve dependencies
   * @return the entities that depend on {@code entities}
   * @throws DatabaseException in case of a db exception
   */
  Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException;

  /**
   * Selects the number of rows returned according to the given condition
   * @param condition the search condition
   * @return the number of rows fitting the given condition
   * @throws DatabaseException in case of a db exception
   */
  int selectRowCount(final EntityCondition condition) throws DatabaseException;

  /**
   * Takes a ReportWrapper object using a JDBC datasource and returns an initialized ReportResult object
   * @param reportWrapper the wrapper containing the report to fill
   * @return an initialized ReportResult object
   * @throws DatabaseException in case of a db exception
   * @throws org.jminor.common.model.reports.ReportException in case of a report exception
   * @see org.jminor.common.model.reports.ReportWrapper#fillReport(java.sql.Connection)
   */
  ReportResult fillReport(final ReportWrapper reportWrapper) throws DatabaseException, ReportException;

  /**
   * Writes {@code blobData} in the blob field specified by the property identified by {@code propertyID}
   * for the given entity
   * @param primaryKey the primary key of the entity for which to write the blob field
   * @param blobPropertyID the ID of the blob property
   * @param blobData the blob data
   * @throws DatabaseException in case of a db exception
   */
  void writeBlob(final Entity.Key primaryKey, final String blobPropertyID, final byte[] blobData) throws DatabaseException;

  /**
   * Reads the blob specified by the property identified by {@code propertyID} from the given entity
   * @param primaryKey the primary key of the entity
   * @param blobPropertyID the ID of the blob property
   * @return a byte array containing the blob data
   * @throws DatabaseException in case of a db exception
   */
  byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyID) throws DatabaseException;

  /**
   * @return the underlying connection
   */
  DatabaseConnection getDatabaseConnection();
}
