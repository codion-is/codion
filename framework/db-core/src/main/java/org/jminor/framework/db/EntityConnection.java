/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A connection to a database, for querying and manipulating {@link Entity}s and running database
 * operations specified by a single {@link Domain} model.
 * {@link #executeFunction(String, Object...)} and {@link #executeProcedure(String, Object...)}
 * do not perform any transaction control but {@link #insert(List)}, {@link #update(List)} and
 * {@link #delete(List)} perform a commit unless they are run within a transaction.
 * @see #beginTransaction()
 * @see #rollbackTransaction()
 * @see #commitTransaction()
 */
public interface EntityConnection {

  /**
   * @return the underlying domain model
   */
  Domain getDomain();

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
   * @param functionId the function ID
   * @param arguments the arguments, if any
   * @return the function return arguments
   * @throws DatabaseException in case anything goes wrong during the execution
   */
  List executeFunction(final String functionId, final Object... arguments) throws DatabaseException;

  /**
   * Executes the procedure with the given id
   * @param procedureId the procedure ID
   * @param arguments the arguments, if any
   * @throws DatabaseException in case anything goes wrong during the execution
   */
  void executeProcedure(final String procedureId, final Object... arguments) throws DatabaseException;

  /**
   * Inserts the given entities, returning a list containing the primary keys of the inserted entities
   * in the same order as they were received.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to insert
   * @return the primary key values of the inserted entities
   * @throws DatabaseException in case of a database exception
   */
  List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException;

  /**
   * Updates the given entities according to their properties. Returns the updated entities, in no particular order.
   * Throws an exception if any of the given entities is unmodified..
   * Performs a commit unless a transaction is open.
   * @param entities the entities to update
   * @return the updated entities, in no particular order
   * @throws DatabaseException in case of a database exception
   * @throws org.jminor.common.db.exception.UpdateException in case there is a mismatch between expected and actual number of updated rows
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity has been modified or deleted by another user
   */
  List<Entity> update(final List<Entity> entities) throws DatabaseException;

  /**
   * Deletes the entities according to the given primary keys.
   * Performs a commit unless a transaction is open.
   * @param entityKeys the primary keys of the entities to delete
   * @throws DatabaseException in case of a database exception
   */
  void delete(final List<Entity.Key> entityKeys) throws DatabaseException;

  /**
   * Deletes the entities specified by the given condition.
   * Performs a commit unless a transaction is open.
   * @param condition the condition specifying the entities to delete
   * @throws DatabaseException in case of a database exception
   */
  void delete(final EntityCondition condition) throws DatabaseException;

  /**
   * Selects ordered and distinct non-null values of the given property, note that the given property
   * must be of type {@link org.jminor.framework.domain.Property.ColumnProperty}.
   * @param propertyId the ID of the property
   * @param condition the condition
   * @return the values in the given column (Property)
   * @throws DatabaseException in case of a database exception
   * @throws IllegalArgumentException in case the given property is not a column based property
   * @throws UnsupportedOperationException in case the entity is based on a select query
   */
  List<Object> selectValues(final String propertyId, final EntityCondition condition) throws DatabaseException;

  /**
   * Selects a single entity
   * @param entityId the entity type
   * @param propertyId the ID of the property to use as a condition
   * @param value the value to use in the condition
   * @return an entity of the type {@code entityId}, having the
   * value of {@code propertyId} as {@code value}
   * @throws DatabaseException in case of a database exception
   * @throws org.jminor.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   */
  Entity selectSingle(final String entityId, final String propertyId, final Object value) throws DatabaseException;

  /**
   * Selects a single entity by key
   * @param key the key of the entity to select
   * @return an entity having the key {@code key}
   * @throws DatabaseException in case of a database exception
   * @throws org.jminor.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   */
  Entity selectSingle(final Entity.Key key) throws DatabaseException;

  /**
   * Selects a single entity according to the specified condition, throws a DatabaseException
   * if the condition results in more than one entity
   * @param condition the condition specifying the entity to select
   * @return the entities according to the given condition
   * @throws DatabaseException in case of a database exception
   * @throws org.jminor.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws org.jminor.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   */
  Entity selectSingle(final EntitySelectCondition condition) throws DatabaseException;

  /**
   * Returns entities according to {@code keys}
   * @param keys the keys used in the condition
   * @return entities according to {@code keys}
   * @throws DatabaseException in case of a database exception
   */
  List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException;

  /**
   * Selects entities according to the specified condition
   * @param condition the condition specifying which entities to select
   * @return entities according to the given condition
   * @throws DatabaseException in case of a database exception
   */
  List<Entity> selectMany(final EntitySelectCondition condition) throws DatabaseException;

  /**
   * Selects entities according to one property ({@code propertyId}), using {@code values} as a condition
   * @param entityId the entity type
   * @param propertyId the ID of the condition property
   * @param values the property values to use as condition
   * @return entities of the type {@code entityId} according to {@code propertyId} and {@code values}
   * @throws DatabaseException in case of a database exception
   */
  List<Entity> selectMany(final String entityId, final String propertyId, final Object... values) throws DatabaseException;

  /**
   * Returns the entities that depend on the given entities via (non-soft) foreign keys, mapped to corresponding entityIds
   * @param entities the entities for which to retrieve dependencies, must be of same type
   * @return the entities that depend on {@code entities}
   * @throws DatabaseException in case of a database exception
   * @see Property.ForeignKeyProperty#isSoftReference()
   */
  Map<String, Collection<Entity>> selectDependencies(final Collection<Entity> entities) throws DatabaseException;

  /**
   * Selects the number of rows returned according to the given condition
   * @param condition the search condition
   * @return the number of rows fitting the given condition
   * @throws DatabaseException in case of a database exception
   */
  int selectRowCount(final EntityCondition condition) throws DatabaseException;

  /**
   * Takes a ReportWrapper object using a JDBC datasource and returns an initialized ReportResult object
   * @param reportWrapper the wrapper containing the report to fill
   * @return an initialized ReportResult object
   * @throws DatabaseException in case of a database exception
   * @throws org.jminor.common.db.reports.ReportException in case of a report exception
   * @see org.jminor.common.db.reports.ReportWrapper#fillReport(java.sql.Connection)
   */
  ReportResult fillReport(final ReportWrapper reportWrapper) throws DatabaseException, ReportException;

  /**
   * Writes {@code blobData} in the blob field specified by the property identified by {@code propertyId}
   * for the given entity
   * @param primaryKey the primary key of the entity for which to write the blob field
   * @param blobPropertyId the ID of the blob property
   * @param blobData the blob data
   * @throws DatabaseException in case of a database exception
   */
  void writeBlob(final Entity.Key primaryKey, final String blobPropertyId, final byte[] blobData) throws DatabaseException;

  /**
   * Reads the blob specified by the property identified by {@code propertyId} from the given entity
   * @param primaryKey the primary key of the entity
   * @param blobPropertyId the ID of the blob property
   * @return a byte array containing the blob data
   * @throws DatabaseException in case of a database exception
   */
  byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyId) throws DatabaseException;
}
