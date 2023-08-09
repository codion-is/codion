/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.common.user.User;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A connection to a database, for querying and manipulating {@link Entity}s and running database
 * operations specified by a single {@link Domain} model.
 * {@link #executeFunction(FunctionType)}  and {@link #executeProcedure(ProcedureType)}
 * do not perform any transaction control but {@link #insert(Entity)}, {@link #insert(Collection)},
 * {@link #update(Entity)}, {@link #update(Collection)}, {@link #delete(Key)} and {@link #delete(Collection)}
 * perform a commit unless they are run within a transaction.
 * A static helper class for mass data manipulation.
 * @see #beginTransaction()
 * @see #rollbackTransaction()
 * @see #commitTransaction()
 */
public interface EntityConnection extends AutoCloseable {

  /**
   * @return the underlying domain entities
   */
  Entities entities();

  /**
   * @return the user being used by this connection
   */
  User user();

  /**
   * @return true if the connection has been established and is valid
   */
  boolean isConnected();

  /**
   * Performs a rollback and disconnects this connection
   */
  void close();

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
   * Controls the enabled state of the query result cache.
   * Queries are cached on a {@link is.codion.framework.db.condition.SelectCondition}
   * basis, but never when selecting for update.
   * The cache is cleared when disabled.
   * @param queryCacheEnabled the result cache state
   */
  void setQueryCacheEnabled(boolean queryCacheEnabled);

  /**
   * @return true if the query cache is enabled
   * @see #setQueryCacheEnabled(boolean)
   */
  boolean isQueryCacheEnabled();

  /**
   * Executes the function with the given type with no arguments
   * @param functionType the function type
   * @param <C> the connection type
   * @param <T> the argument type
   * @param <R> the return value type
   * @return the function return value
   * @throws DatabaseException in case anything goes wrong during the execution
   */
  <C extends EntityConnection, T, R> R executeFunction(FunctionType<C, T, R> functionType) throws DatabaseException;

  /**
   * Executes the function with the given type
   * @param functionType the function type
   * @param argument the function argument
   * @param <C> the connection type
   * @param <T> the argument type
   * @param <R> the return value type
   * @return the function return value
   * @throws DatabaseException in case anything goes wrong during the execution
   */
  <C extends EntityConnection, T, R> R executeFunction(FunctionType<C, T, R> functionType, T argument) throws DatabaseException;

  /**
   * Executes the procedure with the given type with no arguments
   * @param procedureType the procedure type
   * @param <C> the connection type
   * @param <T> the procedure argument type
   * @throws DatabaseException in case anything goes wrong during the execution
   */
  <C extends EntityConnection, T> void executeProcedure(ProcedureType<C, T> procedureType) throws DatabaseException;

  /**
   * Executes the procedure with the given type
   * @param procedureType the procedure type
   * @param argument the procedure argument
   * @param <C> the connection type
   * @param <T> the argument type
   * @throws DatabaseException in case anything goes wrong during the execution
   */
  <C extends EntityConnection, T> void executeProcedure(ProcedureType<C, T> procedureType, T argument) throws DatabaseException;

  /**
   * Inserts the given entity, returning the primary key.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to insert
   * @return the primary key of the inserted entity
   * @throws DatabaseException in case of a database exception
   */
  Key insert(Entity entity) throws DatabaseException;

  /**
   * Inserts the given entities, returning the primary keys in the same order as they were received.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to insert
   * @return the primary keys of the inserted entities
   * @throws DatabaseException in case of a database exception
   */
  Collection<Key> insert(Collection<? extends Entity> entities) throws DatabaseException;

  /**
   * Updates the given entity based on its attribute values. Returns the updated entity.
   * Throws an exception if the given entity is unmodified.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to update
   * @return the updated entity
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.UpdateException in case there is a mismatch between expected and actual number of updated rows
   * @throws is.codion.common.db.exception.RecordModifiedException in case the entity has been modified or deleted by another user
   */
  Entity update(Entity entity) throws DatabaseException;

  /**
   * Updates the given entities based on their attribute values. Returns the updated entities, in no particular order.
   * Throws an exception if any of the given entities is unmodified.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to update
   * @return the updated entities, in no particular order
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.UpdateException in case there is a mismatch between expected and actual number of updated rows
   * @throws is.codion.common.db.exception.RecordModifiedException in case an entity has been modified or deleted by another user
   */
  Collection<Entity> update(Collection<? extends Entity> entities) throws DatabaseException;

  /**
   * Performs an update based on the given condition, updating the attributes found
   * in the {@link UpdateCondition#attributeValues()} map, with the associated values.
   * @param condition the condition
   * @return the number of affected rows
   * @throws DatabaseException in case of a database exception
   */
  int update(UpdateCondition condition) throws DatabaseException;

  /**
   * Deletes the entity with the given primary key.
   * Performs a commit unless a transaction is open.
   * @param entityKey the primary key of the entity to delete
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.DeleteException in case no row or multiple rows were deleted
   */
  void delete(Key entityKey) throws DatabaseException;

  /**
   * Deletes the entities with the given primary keys.
   * Performs a commit unless a transaction is open.
   * @param entityKeys the primary keys of the entities to delete
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.DeleteException in case the number of deleted rows does not match the number of keys
   */
  void delete(Collection<Key> entityKeys) throws DatabaseException;

  /**
   * Deletes the entities specified by the given condition.
   * Performs a commit unless a transaction is open.
   * @param condition the condition specifying the entities to delete
   * @return the number of deleted rows
   * @throws DatabaseException in case of a database exception
   */
  int delete(Condition condition) throws DatabaseException;

  /**
   * Selects ordered and distinct non-null values of the given attribute, note that the attribute
   * must be associated with a {@link ColumnProperty}.
   * @param attribute attribute
   * @param <T> the value type
   * @return the values of the given attribute
   * @throws DatabaseException in case of a database exception
   * @throws IllegalArgumentException in case the given property is not a column based attribute
   * @throws UnsupportedOperationException in case the entity is based on a select query
   */
  <T> List<T> select(Attribute<T> attribute) throws DatabaseException;

  /**
   * Selects distinct non-null values of the given attribute, note that the attribute
   * must be associated with a {@link ColumnProperty}. If the condition provides no
   * order by clause the result is ordered by the attribute column.
   * @param attribute attribute
   * @param condition the condition, null if all values should be selected
   * @param <T> the value type
   * @return the values of the given attribute
   * @throws DatabaseException in case of a database exception
   * @throws IllegalArgumentException in case the given property is not a column based attribute
   * @throws UnsupportedOperationException in case the entity is based on a select query
   */
  <T> List<T> select(Attribute<T> attribute, Condition condition) throws DatabaseException;

  /**
   * Selects a single entity
   * @param attribute attribute to use as a condition
   * @param value the value to use in the condition
   * @param <T> the value type
   * @return an entity of the type {@code entityType}, having the
   * value of {@code attribute} as {@code value}
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   */
  <T> Entity selectSingle(Attribute<T> attribute, T value) throws DatabaseException;

  /**
   * Selects an entity by key
   * @param key the key of the entity to select
   * @return an entity having the key {@code key}
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   */
  Entity select(Key key) throws DatabaseException;

  /**
   * Selects a single entity based on the specified condition
   * @param condition the condition specifying the entity to select
   * @return the entities based on the given condition
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   */
  Entity selectSingle(Condition condition) throws DatabaseException;

  /**
   * Selects entities based on the given {@code keys}
   * @param keys the keys used in the condition
   * @return entities based on {@code keys}
   * @throws DatabaseException in case of a database exception
   */
  Collection<Entity> select(Collection<Key> keys) throws DatabaseException;

  /**
   * Selects entities based on the given condition
   * @param condition the condition specifying which entities to select
   * @return entities based to the given condition
   * @throws DatabaseException in case of a database exception
   */
  List<Entity> select(Condition condition) throws DatabaseException;

  /**
   * Selects entities based on a single attribute condition
   * @param attribute the condition attribute
   * @param value the value to use as condition
   * @param <T> the value type
   * @return entities of the type {@code entityType} based on {@code attribute} and {@code values}
   * @throws DatabaseException in case of a database exception
   */
  <T> List<Entity> select(Attribute<T> attribute, T value) throws DatabaseException;

  /**
   * Selects entities based on a single attribute condition, using {@code values} OR'ed together
   * @param attribute the condition attribute
   * @param values the values to use as condition
   * @param <T> the value type
   * @return entities of the type {@code entityType} based on {@code attribute} and {@code values}
   * @throws DatabaseException in case of a database exception
   */
  <T> List<Entity> select(Attribute<T> attribute, Collection<T> values) throws DatabaseException;

  /**
   * Selects the entities that depend on the given entities via (non-soft) foreign keys, mapped to corresponding entityTypes
   * @param entities the entities for which to retrieve dependencies, must be of same type
   * @return the entities that depend on {@code entities}
   * @throws DatabaseException in case of a database exception
   * @see ForeignKeyProperty#isSoftReference()
   */
  Map<EntityType, Collection<Entity>> selectDependencies(Collection<? extends Entity> entities) throws DatabaseException;

  /**
   * Selects the number of rows returned based on the given condition
   * @param condition the search condition
   * @return the number of rows fitting the given condition
   * @throws DatabaseException in case of a database exception
   */
  int rowCount(Condition condition) throws DatabaseException;

  /**
   * Takes a ReportType object using a JDBC datasource and returns an initialized report result object
   * @param reportType the report to fill
   * @param reportParameters the report parameters, if any
   * @param <T> the report type
   * @param <R> the report result type
   * @param <P> the report parameters type
   * @return the filled result object
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.report.ReportException in case of a report exception
   * @see Report#fillReport(java.sql.Connection, Object)
   */
  <T, R, P> R fillReport(ReportType<T, R, P> reportType, P reportParameters) throws DatabaseException, ReportException;

  /**
   * Writes {@code blobData} into the blob field specified by {@code attribute} for the given entity
   * @param primaryKey the primary key of the entity for which to write the blob field
   * @param blobAttribute the blob attribute
   * @param blobData the blob data
   * @throws is.codion.common.db.exception.UpdateException in case multiple rows were affected
   * @throws DatabaseException in case of a database exception
   */
  void writeBlob(Key primaryKey, Attribute<byte[]> blobAttribute, byte[] blobData) throws DatabaseException;

  /**
   * Reads the blob value associated with {@code attribute} from the given entity,
   * returns null if no blob data is found.
   * @param primaryKey the primary key of the entity
   * @param blobAttribute the blob attribute
   * @return a byte array containing the blob data or null if no blob data is found
   * @throws DatabaseException in case of a database exception
   */
  byte[] readBlob(Key primaryKey, Attribute<byte[]> blobAttribute) throws DatabaseException;

  /**
   * Creates a new {@link CopyEntities.Builder} instance for copying entities from source to destination, with a default batch size of 100.
   * Performs a commit after each {code batchSize} number of inserts, unless the destination connection has an open transaction.
   * Call {@link CopyEntities.Builder#execute()} to perform the copy operation.
   * @param source the source connection
   * @param destination the destination connection
   * @return a new {@link CopyEntities.Builder} instance
   */
  static CopyEntities.Builder copyEntities(EntityConnection source, EntityConnection destination) {
    return new DefaultCopyEntities.DefaultBuilder(source, destination);
  }

  /**
   * Creates a new {@link InsertEntities} instance the given entities, with a default batch size of 100.
   * Performs a commit after each {@code batchSize} number of inserts, unless the destination connection has an open transaction.
   * Call {@link InsertEntities#execute()} to perform the insert operation.
   * @param connection the entity connection to use when inserting
   * @param entities the entities to insert
   * @return a new {@link InsertEntities.Builder} instance
   */
  static InsertEntities.Builder insertEntities(EntityConnection connection, Iterator<Entity> entities) {
    return new DefaultInsertEntities.DefaultBuilder(connection, entities);
  }

  /**
   * Copies a set of entities between a source and destination connection, performing a commit after each {@code batchSize} number of inserts,
   * unless the destination connection has an open transaction.
   * @see #execute()
   */
  interface CopyEntities {

    /**
     * Executes this copy operation
     * @throws DatabaseException in case of an exception
     */
    void execute() throws DatabaseException;

    /**
     * A builder for a {@link CopyEntities} operation.
     */
    interface Builder {

      /**
       * @param entityTypes the entity types to copy
       * @return this builder instance
       */
      Builder entityTypes(EntityType... entityTypes);

      /**
       * @param batchSize the commit batch size
       * @return this buildr instance
       * @throws IllegalArgumentException if {@code batchSize} is not a positive integer
       */
      Builder batchSize(int batchSize);

      /**
       * @param includePrimaryKeys true if the primary key values should be included when copying
       * @return this builder instance
       */
      Builder includePrimaryKeys(boolean includePrimaryKeys);

      /**
       * Specifies a criteria to use when determining which entities of the given type to copy,
       * if none is specified all entities are copied.
       * @param criteria the criteria to use
       * @return this builder instance
       */
      Builder criteria(Criteria criteria);

      /**
       * Builds and executes this copy operation
       * @throws DatabaseException in case of an exception
       */
      void execute() throws DatabaseException;

      /**
       * @return a new {@link CopyEntities} instance
       */
      CopyEntities build();
    }
  }

  /**
   * Inserts entities in batches, performing a commit after each {@code batchSize} number of inserts,
   * unless the destination connection has an open transaction.
   * @see #execute()
   */
  interface InsertEntities {

    /**
     * Executes this batch insert
     * @throws DatabaseException in case of an exception
     */
    void execute() throws DatabaseException;

    /**
     * A builder for {@link InsertEntities} operation.
     */
    interface Builder {

      /**
       * @param batchSize the commit batch size
       * @return this builder instance
       */
      Builder batchSize(int batchSize);

      /**
       * @param progressReporter if specified this will be used to report batch progress
       * @return this builder instance
       */
      Builder progressReporter(Consumer<Integer> progressReporter);

      /**
       * @param onInsert notified each time a batch is inserted, providing the inserted keys
       * @return this builder instance
       */
      Builder onInsert(Consumer<Collection<Key>> onInsert);

      /**
       * Builds and executes this insert operation
       * @throws DatabaseException in case of an exception
       */
      void execute() throws DatabaseException;

      /**
       * @return a new {@link InsertEntities} instance
       */
      InsertEntities build();
    }
  }
}
