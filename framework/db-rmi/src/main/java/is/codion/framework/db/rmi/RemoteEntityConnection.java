/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.rmi;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Condition;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A remote EntityConnection.
 */
public interface RemoteEntityConnection extends Remote, AutoCloseable {

  /**
   * @return the underlying domain entities
   * @throws RemoteException in case of an exception
   */
  Entities entities() throws RemoteException;

  /**
   * @return the user being used by this connection
   * @throws RemoteException in case of an exception
   */
  User user() throws RemoteException;

  /**
   * @return true if this connection has been established and is valid
   * @throws RemoteException in case of an exception
   */
  boolean connected() throws RemoteException;

  /**
   * Closes this connection.
   * @throws RemoteException in case of an exception
   */
  @Override
  void close() throws RemoteException;

  /**
   * @return true if a transaction is open, false otherwise
   * @throws RemoteException in case of exception
   */
  boolean transactionOpen() throws RemoteException;

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
   * Controls the enabled state of the query result cache.
   * The cache is cleared when disabled.
   * @param queryCacheEnabled the result cache state
   * @throws RemoteException in case of a remote exception
   */
  void setQueryCacheEnabled(boolean queryCacheEnabled) throws RemoteException;

  /**
   * @return true if the query cache is enabled
   * @throws RemoteException in case of a remote exception
   * @see #setQueryCacheEnabled(boolean)
   */
  boolean isQueryCacheEnabled() throws RemoteException;

  /**
   * Executes the function with the given type with no arguments
   * @param functionType the function type
   * @param <C> the connection type
   * @param <T> the argument type
   * @param <R> the return value type
   * @return the function return value
   * @throws DatabaseException in case anything goes wrong during the execution
   * @throws RemoteException in case of a remote exception
   */
  <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType) throws RemoteException, DatabaseException;

  /**
   * Executes the function with the given type
   * @param functionType the function type
   * @param argument the function argument
   * @param <C> the connection type
   * @param <T> the argument type
   * @param <R> the return value type
   * @return the function return value
   * @throws DatabaseException in case anything goes wrong during the execution
   * @throws RemoteException in case of a remote exception
   */
  <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType, T argument) throws RemoteException, DatabaseException;

  /**
   * Executes the procedure with the given type with no arguments
   * @param procedureType the procedure type
   * @param <C> the connection type
   * @param <T> the procedure argument type
   * @throws DatabaseException in case anything goes wrong during the execution
   * @throws RemoteException in case of a remote exception
   */
  <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType) throws RemoteException, DatabaseException;

  /**
   * Executes the procedure with the given type
   * @param procedureType the procedure type
   * @param argument the procedure argument
   * @param <C> the connection type
   * @param <T> the procedure argument type
   * @throws DatabaseException in case anything goes wrong during the execution
   * @throws RemoteException in case of a remote exception
   */
  <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType, T argument) throws RemoteException, DatabaseException;

  /**
   * Inserts the given entity, returning the primary key.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to insert
   * @return the primary key of the inserted entity
   * @throws DatabaseException in case of a database exception
   * @throws RemoteException in case of a remote exception
   */
  Entity.Key insert(Entity entity) throws RemoteException, DatabaseException;

  /**
   * Inserts the given entity, returning the inserted etity.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to insert
   * @return the inserted entity
   * @throws DatabaseException in case of a database exception
   * @throws RemoteException in case of a remote exception
   */
  Entity insertSelect(Entity entity) throws RemoteException, DatabaseException;

  /**
   * Inserts the given entities, returning the primary keys in the same order as they were received.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to insert
   * @return the primary keys of the inserted entities
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  Collection<Entity.Key> insert(Collection<? extends Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Inserts the given entities, returning the inserted entities.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to insert
   * @return the inserted entities
   * @throws DatabaseException in case of a database exception
   * @throws RemoteException in case of a remote exception
   */
  Collection<Entity> insertSelect(Collection<? extends Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Updates the given entity based on its attribute values. Returns the updated entity.
   * Throws an exception if the given entity is unmodified.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to update
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.UpdateException in case there is a mismatch between expected and actual number of updated rows
   * @throws is.codion.common.db.exception.RecordModifiedException in case the entity has been modified or deleted by another user
   * @throws RemoteException in case of a remote exception
   */
  void update(Entity entity) throws RemoteException, DatabaseException;

  /**
   * Updates the given entity based on its attribute values. Returns the updated entity.
   * Throws an exception if the given entity is unmodified.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to update
   * @return the updated entity
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.UpdateException in case there is a mismatch between expected and actual number of updated rows
   * @throws is.codion.common.db.exception.RecordModifiedException in case the entity has been modified or deleted by another user
   * @throws RemoteException in case of a remote exception
   */
  Entity updateSelect(Entity entity) throws RemoteException, DatabaseException;

  /**
   * Updates the given entities based on their attribute values.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to update
   * @throws DatabaseException in case of a db exception
   * @throws is.codion.common.db.exception.RecordModifiedException in case an entity has been modified or deleted by another user
   * @throws RemoteException in case of a remote exception
   */
  void update(Collection<? extends Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Updates the given entities based on their attribute values. Returns the updated entities, in no particular order.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to update
   * @return the updated entities
   * @throws DatabaseException in case of a db exception
   * @throws is.codion.common.db.exception.RecordModifiedException in case an entity has been modified or deleted by another user
   * @throws RemoteException in case of a remote exception
   */
  Collection<Entity> updateSelect(Collection<? extends Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Performs an update based on the given update, updating the columns found
   * in the {@link Update#columnValues()} map, with the associated values.
   * @param update the update to perform
   * @return the number of affected rows
   * @throws DatabaseException in case of a dabase exception
   * @throws RemoteException in case of a remote exception
   */
  int update(Update update) throws RemoteException, DatabaseException;

  /**
   * Deletes an entity according to the given primary key.
   * Performs a commit unless a transaction is open.
   * @param key the primary key of the entity to delete
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.DeleteException in case no row or multiple rows were deleted
   * @throws RemoteException in case of a remote exception
   */
  void delete(Entity.Key key) throws RemoteException, DatabaseException;

  /**
   * Deletes the entities with the given primary keys.
   * This method respects the iteration order of the given collection by first deleting all
   * entities of the first entityType encountered, then all entities of the next entityType encountered and so on.
   * This allows the deletion of multiple entities forming a master detail hierarchy, by having the detail
   * entities appear before their master entities in the collection.
   * Performs a commit unless a transaction is open.
   * @param keys the primary keys of the entities to delete
   * @throws DatabaseException in case of a db exception
   * @throws is.codion.common.db.exception.DeleteException in case the number of deleted rows does not match the number of keys
   * @throws RemoteException in case of a remote exception
   */
  void delete(Collection<Entity.Key> keys) throws RemoteException, DatabaseException;

  /**
   * Deletes the entities specified by the given condition
   * Performs a commit unless a transaction is open.
   * @param condition the condition specifying the entities to delete
   * @return the number of deleted rows
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException   in case of a remote exception
   */
  int delete(Condition condition) throws RemoteException, DatabaseException;

  /**
   * Selects ordered and distinct non-null values of the given column
   * @param column the column
   * @param <T> the value type
   * @return all the values of the given column
   * @throws DatabaseException in case of a db exception
   * @throws IllegalArgumentException in case the given column has not associated with a table column
   * @throws UnsupportedOperationException in case the entity uses a custom column clause or if the column represents an aggregate value
   * @throws RemoteException in case of a remote exception
   */
  <T> List<T> select(Column<T> column) throws RemoteException, DatabaseException;

  /**
   * Selects distinct non-null values of the given column. The result is ordered by the selected column.
   * @param column column
   * @param condition the condition
   * @param <T> the value type
   * @return the values of the given column
   * @throws DatabaseException in case of a database exception
   * @throws IllegalArgumentException in case the given column is not associated with a table column
   * @throws UnsupportedOperationException in case the entity uses a custom column clause or if the column represents an aggregate value
   * @throws RemoteException in case of a remote exception
   */
  <T> List<T> select(Column<T> column, Condition condition) throws RemoteException, DatabaseException;

  /**
   * Selects distinct non-null values of the given column. If the select provides no
   * order by clause the result is ordered by the selected column.
   * @param column the column
   * @param select the select to perform
   * @param <T> the value type
   * @return the values of the given column
   * @throws DatabaseException in case of a db exception
   * @throws IllegalArgumentException in case the given column is not associated with a table column
   * @throws UnsupportedOperationException in case the entity uses a custom column clause or if the column represents an aggregate value
   * @throws RemoteException in case of a remote exception
   */
  <T> List<T> select(Column<T> column, Select select) throws RemoteException, DatabaseException;

  /**
   * Selects an entity by key
   * @param key the key of the entity to select
   * @return an entity having the key {@code key}
   * @throws DatabaseException in case of a db exception
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   * @throws RemoteException in case of a remote exception
   */
  Entity select(Entity.Key key) throws RemoteException, DatabaseException;

  /**
   * Selects a single entity based on the specified condition
   * @param condition the condition specifying the entity to select
   * @return the entities based on the given condition
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   * @throws RemoteException in case of a remote exception
   */
  Entity selectSingle(Condition condition) throws RemoteException, DatabaseException;

  /**
   * Selects a single entity based on the specified select
   * @param select the select to perform
   * @return the entities according to the given select
   * @throws DatabaseException if an exception occurs
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   * @throws RemoteException in case of a remote exception
   */
  Entity selectSingle(Select select) throws RemoteException, DatabaseException;

  /**
   * Selects entities based on the given {@code keys}
   * @param keys the keys used in the condition
   * @return entities based on {@code keys}
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  Collection<Entity> select(Collection<Entity.Key> keys) throws RemoteException, DatabaseException;

  /**
   * Selects entities based on the given condition
   * @param condition the condition specifying which entities to select
   * @return entities based to the given condition
   * @throws DatabaseException in case of a database exception
   * @throws RemoteException in case of a remote exception
   */
  List<Entity> select(Condition condition) throws RemoteException,  DatabaseException;

  /**
   * Selects entities based on the given select
   * @param select the select to perform
   * @return entities based to the given select
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  List<Entity> select(Select select) throws RemoteException, DatabaseException;

  /**
   * Selects the entities that depend on the given entities via (non-soft) foreign keys, mapped to corresponding entityTypes
   * @param entities the entities for which to retrieve dependencies
   * @return the entities that depend on {@code entities}
   * @throws IllegalArgumentException in case the entities are not of the same type
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  Map<EntityType, Collection<Entity>> dependencies(Collection<? extends Entity> entities) throws RemoteException, DatabaseException;

  /**
   * Counts the number of rows returned based on the given count conditions
   * @param count the count conditions
   * @return the number of rows fitting the given count conditions
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException   in case of a remote exception
   */
  int count(Count count) throws RemoteException, DatabaseException;

  /**
   * Takes a ReportType object using a JDBC datasource and returns an initialized ReportResult object
   * @param reportType the report to fill
   * @param reportParameters the report parameters, if any
   * @param <T> the report type
   * @param <R> the report result type
   * @param <P> the report parameters type
   * @return the filled result object
   * @throws DatabaseException in case of a db exception
   * @throws is.codion.common.db.report.ReportException in case of a report exception
   * @throws RemoteException in case of a remote exception
   * @see Report#fillReport(java.sql.Connection, Object)
   */
  <T, R, P> R fillReport(ReportType<T, R, P> reportType, P reportParameters) throws RemoteException, DatabaseException, ReportException;

  /**
   * Writes {@code blobData} into the blob field specified by {@code attribute} for the given entity
   * @param primaryKey the primary key of the entity for which to write the blob field
   * @param blobColumn the blob column
   * @param blobData the blob data
   * @throws is.codion.common.db.exception.UpdateException in case zero or multiple rows were affected
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  void writeBlob(Entity.Key primaryKey, Column<byte[]> blobColumn, byte[] blobData) throws RemoteException, DatabaseException;

  /**
   * Reads the blob value associated with {@code attribute} from the given entity,
   * returns null if no blob data is found.
   * @param primaryKey the primary key of the entity
   * @param blobColumn the blob column
   * @return a byte array containing the blob data
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the row was not found
   * @throws DatabaseException in case of a db exception
   * @throws RemoteException in case of a remote exception
   */
  byte[] readBlob(Entity.Key primaryKey, Column<byte[]> blobColumn) throws RemoteException, DatabaseException;
}
