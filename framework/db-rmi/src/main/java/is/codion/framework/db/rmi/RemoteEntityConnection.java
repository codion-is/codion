/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.rmi;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportWrapper;
import is.codion.common.user.User;
import is.codion.framework.db.condition.EntityCondition;
import is.codion.framework.db.condition.EntitySelectCondition;
import is.codion.framework.db.condition.EntityUpdateCondition;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

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
   * @return the underlying domain entities
   * @throws RemoteException in case of an exception
   */
  Entities getEntities() throws RemoteException;

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
   * @param functionId the function id
   * @param arguments the arguments, if any
   * @param <T> the result type
   * @return the function return argument
   * @throws DatabaseException in case anything goes wrong during the execution
   * @throws RemoteException in case of a remote exception
   */
  <T> T executeFunction(String functionId, Object... arguments) throws RemoteException, DatabaseException;

  /**
   * Executes the procedure with the given id
   * @param procedureId the procedure id
   * @param arguments the arguments, if any
   * @throws DatabaseException in case anything goes wrong during the execution
   * @throws RemoteException in case of a remote exception
   */
  void executeProcedure(String procedureId, Object... arguments) throws RemoteException, DatabaseException;

  /**
   * Inserts the given entity, returning the primary key of the inserted entity.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to insert
   * @return the primary key of the inserted entity
   * @throws DatabaseException in case of a database exception
   * @throws RemoteException in case of a remote exception
   */
  Entity.Key insert(Entity entity) throws RemoteException, DatabaseException;

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
  List<Entity.Key> insert(List<Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Updates the given entity according to its properties. Returns the updated entity.
   * Throws an exception if the given entity is unmodified.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to update
   * @return the updated entity
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.UpdateException in case there is a mismatch between expected and actual number of updated rows
   * @throws is.codion.common.db.exception.RecordModifiedException in case the entity has been modified or deleted by another user
   * @throws RemoteException in case of a remote exception
   */
  Entity update(Entity entity) throws RemoteException, DatabaseException;

  /**
   * Updates the given entities according to their properties.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to update
   * @return the updated entities
   * @throws DatabaseException in case of a db exception
   * @throws is.codion.common.db.exception.RecordModifiedException in case an entity has been modified or deleted by another user
   * @throws RemoteException in case of a remote exception
   */
  List<Entity> update(List<Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Performs an update according to the given condition, updating the properties found
   * in the {@link EntityUpdateCondition#getAttributeValues()} map, with the associated values.
   * @param condition the condition
   * @return the number of affected rows
   * @throws DatabaseException in case of a dabase exception
   * @throws RemoteException in case of a remote exception
   */
  int update(EntityUpdateCondition condition) throws RemoteException, DatabaseException;

  /**
   * Deletes an entity according to the given primary key.
   * Performs a commit unless a transaction is open.
   * @param entityKey the primary key of the entity to delete
   * @return true if a record was deleted, false otherwise
   * @throws DatabaseException in case of a database exception
   * @throws RemoteException in case of a remote exception
   */
  boolean delete(Entity.Key entityKey) throws RemoteException, DatabaseException;

  /**
   * Deletes the entities according to the given primary keys.
   * Performs a commit unless a transaction is open.
   * @param entityKeys the primary keys of the entities to delete
   * @return the number of deleted rows
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  int delete(List<Entity.Key> entityKeys) throws RemoteException, DatabaseException;

  /**
   * Deletes the entities specified by the given condition
   * Performs a commit unless a transaction is open.
   * @param condition the condition specifying the entities to delete
   * @return the number of deleted rows
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  int delete(EntityCondition condition) throws RemoteException, DatabaseException;

  /**
   * Selects ordered and distinct non-null values of the given property
   * @param attribute the attribute
   * @param condition the condition
   * @param <T> the value type
   * @return the values for the given attribute
   * @throws DatabaseException in case of a db exception
   * @throws IllegalArgumentException in case the given property is not a column based attribute
   * @throws UnsupportedOperationException in case the entity is based on a select query
   * @throws RemoteException in case of a remote exception
   */
  <T> List<T> selectValues(Attribute<T> attribute, EntityCondition condition)
          throws RemoteException, DatabaseException;

  /**
   * Selects a single entity
   * @param entityType the entity type
   * @param attribute the attribute to use as a condition
   * @param value the value to use in the condition
   * @param <T> the value type
   * @return an entity of the type {@code entityType}, having the
   * value of {@code attribute} as {@code value}
   * @throws DatabaseException in case of a db exception
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   * @throws RemoteException in case of a remote exception
   */
  <T> Entity selectSingle(EntityType entityType, Attribute<T> attribute, T value) throws RemoteException, DatabaseException;

  /**
   * Selects a single entity by key
   * @param key the key of the entity to select
   * @return an entity having the key {@code key}
   * @throws DatabaseException in case of a db exception
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   * @throws RemoteException in case of a remote exception
   */
  Entity selectSingle(Entity.Key key) throws RemoteException, DatabaseException;

  /**
   * Selects a single entity according to the specified condition, throws a DatabaseException
   * if the condition results in more than one entity
   * @param condition the condition specifying the entity to select
   * @return the entities according to the given condition
   * @throws DatabaseException if an exception occurs
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   * @throws RemoteException in case of a remote exception
   */
  Entity selectSingle(EntitySelectCondition condition) throws RemoteException, DatabaseException;

  /**
   * Returns entities according to {@code keys}
   * @param keys the keys used in the condition
   * @return entities according to {@code keys}
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  List<Entity> select(List<Entity.Key> keys) throws RemoteException, DatabaseException;

  /**
   * Selects entities according to the specified condition
   * @param condition the condition specifying which entities to select
   * @return entities according to the given condition
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  List<Entity> select(EntitySelectCondition condition) throws RemoteException, DatabaseException;

  /**
   * Selects entities according to one property ({@code attribute}), using {@code values} as a condition
   * @param entityType the entity type
   * @param attribute the condition attribute
   * @param value the value to use as condition
   * @param <T> the value type
   * @return entities of the type {@code entityType} according to {@code attribute} and {@code values}
   * @throws DatabaseException in case of a database exception
   * @throws RemoteException in case of a remote exception
   */
  <T> List<Entity> select(EntityType entityType, Attribute<T> attribute, T value) throws RemoteException, DatabaseException;

  /**
   * Selects entities according to one property ({@code attribute}), using {@code values} as a condition
   * @param entityType the entity type
   * @param attribute the condition attribute
   * @param values the values to use as condition
   * @param <T> the value type
   * @return entities of the type {@code entityType} according to {@code attribute} and {@code values}
   * @throws DatabaseException in case of a database exception
   * @throws RemoteException in case of a remote exception
   */
  <T> List<Entity> select(EntityType entityType, Attribute<T> attribute, Collection<T> values) throws RemoteException, DatabaseException;

  /**
   * Returns the entities that depend on the given entities via foreign keys, mapped to corresponding entityTypes
   * @param entities the entities for which to retrieve dependencies
   * @return the entities that depend on {@code entities}
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  Map<EntityType, Collection<Entity>> selectDependencies(Collection<Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Selects the number of rows returned according to the given condition
   * @param condition the search condition
   * @return the number of rows fitting the given condition
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  int selectRowCount(EntityCondition condition) throws RemoteException, DatabaseException;

  /**
   * Takes a ReportWrapper object using a JDBC datasource and returns an initialized ReportResult object
   * @param reportWrapper the wrapper containing the report to fill
   * @param reportParameters the report parameters, if any
   * @param <T> the report type
   * @param <R> the report result type
   * @param <P> the report parameters type
   * @return the filled result object
   * @throws DatabaseException in case of a db exception
   * @throws is.codion.common.db.reports.ReportException in case of a report exception
   * @throws RemoteException in case of a remote exception
   * @see is.codion.common.db.reports.ReportWrapper#fillReport(java.sql.Connection, Object)
   */
  <T, R, P> R fillReport(ReportWrapper<T, R, P> reportWrapper, P reportParameters) throws RemoteException, DatabaseException, ReportException;

  /**
   * Writes {@code blobData} in the blob field specified by the property identified by {@code attribute}
   * for the given entity
   * @param primaryKey the primary key of the entity for which to write the blob field
   * @param blobAttribute the blob attribute
   * @param blobData the blob data
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  void writeBlob(Entity.Key primaryKey, Attribute<byte[]> blobAttribute, byte[] blobData) throws RemoteException, DatabaseException;

  /**
   * Reads the blob specified by the property identified by {@code attribute} from the given entity
   * @param primaryKey the primary key of the entity
   * @param blobAttribute the blob attribute
   * @return a byte array containing the blob data
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  byte[] readBlob(Entity.Key primaryKey, Attribute<byte[]> blobAttribute) throws RemoteException, DatabaseException;
}
