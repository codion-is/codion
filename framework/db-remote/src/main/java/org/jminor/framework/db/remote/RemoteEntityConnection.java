/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.remote;

import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Entity;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A remote EntityConnection.
 */
public interface RemoteEntityConnection extends Remote {

  /**
   * @param methodLogger the method logger
   * @throws UnsupportedOperationException always
   * @throws RemoteException in case of an exception
   */
  void setMethodLogger(final MethodLogger methodLogger) throws RemoteException;

  /**
   * @return the connection type
   * @throws RemoteException in case of an exception
   */
  EntityConnection.Type getType() throws RemoteException;

  /**
   * @return the user being used by this connection
   * @throws RemoteException in case of an exception
   */
  User getUser() throws RemoteException;

  /**
   * @return true if this connection has been established and is valid
   * @throws RemoteException in case of an exception
   */
  boolean isConnected() throws RemoteException;

  /**
   * Disconnects this connection
   * @throws RemoteException in case of an exception
   */
  void disconnect() throws RemoteException;

  /**
   * @throws RemoteException in case of exception
   * @return true if a transaction is open, false otherwise
   */
  boolean isTransactionOpen() throws RemoteException;

  /**
   * Begins a transaction on this connection
   * @throws IllegalStateException if a transaction is already open
   * @throws RemoteException in case of a remote exception
   */
  void beginTransaction() throws RemoteException;

  /**
   * Performs a rollback and ends the current transaction
   * @throws IllegalStateException in case a transaction is not open
   * @throws RemoteException in case of a remote exception
   */
  void rollbackTransaction() throws RemoteException;

  /**
   * Performs a commit and ends the current transaction
   * @throws IllegalStateException in case a transaction is not open
   * @throws RemoteException in case of a remote exception
   */
  void commitTransaction() throws RemoteException;

  /**
   * Executes the function with the given id
   * @param functionId the function ID
   * @param arguments the arguments, if any
   * @return the function return arguments
   * @throws DatabaseException in case anything goes wrong during the execution
   * @throws RemoteException in case of a remote exception
   */
  List executeFunction(final String functionId, final Object... arguments) throws RemoteException, DatabaseException;

  /**
   * Executes the procedure with the given id
   * @param procedureId the procedure ID
   * @param arguments the arguments, if any
   * @throws DatabaseException in case anything goes wrong during the execution
   * @throws RemoteException in case of a remote exception
   */
  void executeProcedure(final String procedureId, final Object... arguments) throws RemoteException, DatabaseException;

  /**
   * Inserts the given entities, returning a list containing the primary keys of the inserted entities
   * in the same order as they were received.
   * If the primary key value of a entity is specified the id generation is disregarded.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to insert
   * @return the primary key values of the inserted entities
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  List<Entity.Key> insert(final List<Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Updates the given entities according to their properties.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to update
   * @return the updated entities
   * @throws DatabaseException in case of a db exception
   * @throws org.jminor.common.db.exception.RecordModifiedException in case an entity has been modified or deleted by another user
   * @throws RemoteException in case of a remote exception
   */
  List<Entity> update(final List<Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Deletes the entities according to the given primary keys.
   * Performs a commit unless a transaction is open.
   * @param entityKeys the primary keys of the entities to delete
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  void delete(final List<Entity.Key> entityKeys) throws RemoteException, DatabaseException;

  /**
   * Deletes the entities specified by the given condition
   * Performs a commit unless a transaction is open.
   * @param condition the condition specifying the entities to delete
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  void delete(final EntityCondition condition) throws RemoteException, DatabaseException;

  /**
   * Selects ordered and distinct non-null values of the given property
   * @param propertyId the ID of the property
   * @param condition the condition
   * @return the values in the given column (Property)
   * @throws DatabaseException in case of a db exception
   * @throws IllegalArgumentException in case the given property is not a column based property
   * @throws UnsupportedOperationException in case the entity is based on a select query
   * @throws RemoteException in case of a remote exception
   */
  List<Object> selectValues(final String propertyId, final EntityCondition condition)
          throws RemoteException, DatabaseException;

  /**
   * Selects a single entity
   * @param entityId the entity type
   * @param propertyId the ID of the property to use as a condition
   * @param value the value to use in the condition
   * @return an entity of the type {@code entityId}, having the
   * value of {@code propertyId} as {@code value}
   * @throws DatabaseException in case of a db exception
   * @throws org.jminor.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws RemoteException in case of a remote exception
   */
  Entity selectSingle(final String entityId, final String propertyId, final Object value) throws RemoteException, DatabaseException;

  /**
   * Selects a single entity by key
   * @param key the key of the entity to select
   * @return an entity having the key {@code key}
   * @throws DatabaseException in case of a db exception
   * @throws org.jminor.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws RemoteException in case of a remote exception
   */
  Entity selectSingle(final Entity.Key key) throws RemoteException, DatabaseException;

  /**
   * Selects a single entity according to the specified condition, throws a DatabaseException
   * if the condition results in more than one entity
   * @param condition the condition specifying the entity to select
   * @return the entities according to the given condition
   * @throws DatabaseException if an exception occurs
   * @throws org.jminor.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws RemoteException in case of a remote exception
   */
  Entity selectSingle(final EntitySelectCondition condition) throws RemoteException, DatabaseException;

  /**
   * Returns entities according to {@code keys}
   * @param keys the keys used in the condition
   * @return entities according to {@code keys}
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  List<Entity> selectMany(final List<Entity.Key> keys) throws RemoteException, DatabaseException;

  /**
   * Selects entities according to the specified condition
   * @param condition the condition specifying which entities to select
   * @return entities according to the given condition
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  List<Entity> selectMany(final EntitySelectCondition condition) throws RemoteException, DatabaseException;

  /**
   * Selects entities according to one property ({@code propertyId}), using {@code values} as a condition
   * @param entityId the entity type
   * @param propertyId the ID of the condition property
   * @param values the property values to use as condition
   * @return entities of the type {@code entityId} according to {@code propertyId} and {@code values}
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  List<Entity> selectMany(final String entityId, final String propertyId, final Object... values) throws RemoteException, DatabaseException;

  /**
   * Returns the entities that depend on the given entities via foreign keys, mapped to corresponding entityIds
   * @param entities the entities for which to retrieve dependencies
   * @return the entities that depend on {@code entities}
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Selects the number of rows returned according to the given condition
   * @param condition the search condition
   * @return the number of rows fitting the given condition
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  int selectRowCount(final EntityCondition condition) throws RemoteException, DatabaseException;

  /**
   * Takes a ReportWrapper object using a JDBC datasource and returns an initialized ReportResult object
   * @param reportWrapper the wrapper containing the report to fill
   * @return an initialized ReportResult object
   * @throws DatabaseException in case of a db exception
   * @throws org.jminor.common.db.reports.ReportException in case of a report exception
   * @throws RemoteException in case of a remote exception
   * @see org.jminor.common.db.reports.ReportWrapper#fillReport(java.sql.Connection)
   */
  ReportResult fillReport(final ReportWrapper reportWrapper) throws RemoteException, DatabaseException, ReportException;

  /**
   * Writes {@code blobData} in the blob field specified by the property identified by {@code propertyId}
   * for the given entity
   * @param primaryKey the primary key of the entity for which to write the blob field
   * @param blobPropertyId the ID of the blob property
   * @param blobData the blob data
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  void writeBlob(final Entity.Key primaryKey, final String blobPropertyId, final byte[] blobData) throws RemoteException, DatabaseException;

  /**
   * Reads the blob specified by the property identified by {@code propertyId} from the given entity
   * @param primaryKey the primary key of the entity
   * @param blobPropertyId the ID of the blob property
   * @return a byte array containing the blob data
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyId) throws RemoteException, DatabaseException;

  /**
   * Unsupported method
   * @return never
   * @throws UnsupportedOperationException always
   * @throws RemoteException in case of a remote exception
   */
  DatabaseConnection getDatabaseConnection() throws RemoteException;
}
