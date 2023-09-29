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
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Condition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A connection to a database, for querying and manipulating {@link Entity}s and running database
 * operations specified by a single {@link Domain} model.
 * {@link #execute(FunctionType)}  and {@link #execute(ProcedureType)}
 * do not perform any transaction control whereas the select, insert, update and delete methods
 * perform a commit unless they are run within a transaction.
 * A static helper class for mass data manipulation.
 * @see #beginTransaction()
 * @see #rollbackTransaction()
 * @see #commitTransaction()
 */
public interface EntityConnection extends AutoCloseable {

  int DEFAULT_QUERY_TIMEOUT_SECONDS = 120;

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
   * Queries are cached on a {@link Select}
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
  <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType) throws DatabaseException;

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
  <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType, T argument) throws DatabaseException;

  /**
   * Executes the procedure with the given type with no arguments
   * @param procedureType the procedure type
   * @param <C> the connection type
   * @param <T> the procedure argument type
   * @throws DatabaseException in case anything goes wrong during the execution
   */
  <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType) throws DatabaseException;

  /**
   * Executes the procedure with the given type
   * @param procedureType the procedure type
   * @param argument the procedure argument
   * @param <C> the connection type
   * @param <T> the argument type
   * @throws DatabaseException in case anything goes wrong during the execution
   */
  <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType, T argument) throws DatabaseException;

  /**
   * Inserts the given entity, returning the primary key.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to insert
   * @return the primary key of the inserted entity
   * @throws DatabaseException in case of a database exception
   */
  Entity.Key insert(Entity entity) throws DatabaseException;

  /**
   * Inserts the given entity, returning the inserted entity.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to insert
   * @return the inserted entity
   * @throws DatabaseException in case of a database exception
   */
  Entity insertSelect(Entity entity) throws DatabaseException;

  /**
   * Inserts the given entities, returning the primary keys.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to insert
   * @return the primary keys of the inserted entities
   * @throws DatabaseException in case of a database exception
   */
  Collection<Entity.Key> insert(Collection<? extends Entity> entities) throws DatabaseException;

  /**
   * Inserts the given entities, returning the inserted entities.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to insert
   * @return the inserted entities
   * @throws DatabaseException in case of a database exception
   */
  Collection<Entity> insertSelect(Collection<? extends Entity> entities) throws DatabaseException;

  /**
   * Updates the given entity based on its attribute values.
   * Throws an exception if the given entity is unmodified.
   * Performs a commit unless a transaction is open.
   * @param entity the entity to update
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.UpdateException in case there is a mismatch between expected and actual number of updated rows
   * @throws is.codion.common.db.exception.RecordModifiedException in case the entity has been modified or deleted by another user
   */
  void update(Entity entity) throws DatabaseException;

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
  Entity updateSelect(Entity entity) throws DatabaseException;

  /**
   * Updates the given entities based on their attribute values.
   * Throws an exception if any of the given entities is unmodified.
   * Performs a commit unless a transaction is open.
   * @param entities the entities to update
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.UpdateException in case there is a mismatch between expected and actual number of updated rows
   * @throws is.codion.common.db.exception.RecordModifiedException in case an entity has been modified or deleted by another user
   */
  void update(Collection<? extends Entity> entities) throws DatabaseException;

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
  Collection<Entity> updateSelect(Collection<? extends Entity> entities) throws DatabaseException;

  /**
   * Performs an update based on the given update, updating the columns found
   * in the {@link Update#columnValues()} map, with the associated values.
   * @param update the update to perform
   * @return the number of affected rows
   * @throws DatabaseException in case of a database exception
   */
  int update(Update update) throws DatabaseException;

  /**
   * Deletes the entity with the given primary key.
   * Performs a commit unless a transaction is open.
   * @param key the primary key of the entity to delete
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.DeleteException in case no row or multiple rows were deleted
   */
  void delete(Entity.Key key) throws DatabaseException;

  /**
   * Deletes the entities with the given primary keys.
   * This method respects the iteration order of the given collection by first deleting all
   * entities of the first entityType encountered, then all entities of the next entityType encountered and so on.
   * This allows the deletion of multiple entities forming a master detail hierarchy, by having the detail
   * entities appear before their master entities in the collection.
   * Performs a commit unless a transaction is open.
   * @param keys the primary keys of the entities to delete
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.DeleteException in case the number of deleted rows does not match the number of keys
   */
  void delete(Collection<Entity.Key> keys) throws DatabaseException;

  /**
   * Deletes the entities specified by the given condition.
   * Performs a commit unless a transaction is open.
   * @param condition the condition specifying the entities to delete
   * @return the number of deleted rows
   * @throws DatabaseException in case of a database exception
   */
  int delete(Condition condition) throws DatabaseException;

  /**
   * Selects ordered and distinct non-null values of the given column.
   * @param column column
   * @param <T> the value type
   * @return the values of the given column
   * @throws DatabaseException in case of a database exception
   * @throws IllegalArgumentException in case the given column has not associated with a table column
   * @throws UnsupportedOperationException in case the entity uses a custom column clause or if the column represents an aggregate value
   */
  <T> List<T> select(Column<T> column) throws DatabaseException;

  /**
   * Selects distinct non-null values of the given column. The result is ordered by the selected column.
   * @param column column
   * @param condition the condition
   * @param <T> the value type
   * @return the values of the given column
   * @throws DatabaseException in case of a database exception
   * @throws IllegalArgumentException in case the given column is not associated with a table column
   * @throws UnsupportedOperationException in case the entity uses a custom column clause or if the column represents an aggregate value
   */
  <T> List<T> select(Column<T> column, Condition condition) throws DatabaseException;

  /**
   * Selects distinct non-null values of the given column. If the select provides no
   * order by clause the result is ordered by the selected column.
   * @param column column
   * @param select the select to perform
   * @param <T> the value type
   * @return the values of the given column
   * @throws DatabaseException in case of a database exception
   * @throws IllegalArgumentException in case the given column is not associated with a table column
   * @throws UnsupportedOperationException in case the entity uses a custom column clause or if the column represents an aggregate value
   */
  <T> List<T> select(Column<T> column, Select select) throws DatabaseException;

  /**
   * Selects an entity by key
   * @param key the key of the entity to select
   * @return an entity having the key {@code key}
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   */
  Entity select(Entity.Key key) throws DatabaseException;

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
   * Selects a single entity based on the specified select
   * @param select the select to perform
   * @return the entities based on the given select
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
   * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
   */
  Entity selectSingle(Select select) throws DatabaseException;

  /**
   * Selects entities based on the given {@code keys}
   * @param keys the keys used in the condition
   * @return entities based on {@code keys}
   * @throws DatabaseException in case of a database exception
   */
  Collection<Entity> select(Collection<Entity.Key> keys) throws DatabaseException;

  /**
   * Selects entities based on the given condition
   * @param condition the condition specifying which entities to select
   * @return entities based to the given condition
   * @throws DatabaseException in case of a database exception
   */
  List<Entity> select(Condition condition) throws DatabaseException;

  /**
   * Selects entities based on the given select
   * @param select the select to perform
   * @return entities based to the given select
   * @throws DatabaseException in case of a database exception
   */
  List<Entity> select(Select select) throws DatabaseException;

  /**
   * Selects the entities that depend on the given entities via (non-soft) foreign keys, mapped to corresponding entityTypes
   * @param entities the entities for which to retrieve dependencies, must be of same type
   * @return the entities that depend on {@code entities}
   * @throws IllegalArgumentException in case the entities are not of the same type
   * @throws DatabaseException in case of a database exception
   * @see ForeignKeyDefinition#isSoftReference()
   */
  Map<EntityType, Collection<Entity>> dependencies(Collection<? extends Entity> entities) throws DatabaseException;

  /**
   * Counts the number of rows returned based on the given condition
   * @param condition the search condition
   * @return the number of rows fitting the given condition
   * @throws DatabaseException in case of a database exception
   */
  int count(Condition condition) throws DatabaseException;

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
   * Writes {@code blobData} into the blob field specified by {@code blobColumn} for the given entity
   * @param primaryKey the primary key of the entity for which to write the blob field
   * @param blobColumn the blob column
   * @param blobData the blob data
   * @throws is.codion.common.db.exception.UpdateException in case zero or multiple rows were affected
   * @throws DatabaseException in case of a database exception
   */
  void writeBlob(Entity.Key primaryKey, Column<byte[]> blobColumn, byte[] blobData) throws DatabaseException;

  /**
   * Reads the blob value associated with {@code blobColumn} from the given entity,
   * returns null if no blob data is found.
   * @param primaryKey the primary key of the entity
   * @param blobColumn the blob attribute
   * @return a byte array containing the blob data
   * @throws is.codion.common.db.exception.RecordNotFoundException in case the row was not found
   * @throws DatabaseException in case of a database exception
   */
  byte[] readBlob(Entity.Key primaryKey, Column<byte[]> blobColumn) throws DatabaseException;

  /**
   * Creates a new {@link Copy.Builder} instance for copying entities from source to destination, with a default batch size of 100.
   * Performs a commit after each {code batchSize} number of inserts, unless the destination connection has an open transaction.
   * Call {@link Copy.Builder#execute()} to perform the copy operation.
   * @param source the source connection
   * @param destination the destination connection
   * @return a new {@link Copy.Builder} instance
   */
  static Copy.Builder copyEntities(EntityConnection source, EntityConnection destination) {
    return new DefaultCopyEntities.DefaultBuilder(source, destination);
  }

  /**
   * Creates a new {@link Insert} instance the given entities, with a default batch size of 100.
   * Performs a commit after each {@code batchSize} number of inserts, unless the destination connection has an open transaction.
   * Call {@link Insert#execute()} to perform the insert operation.
   * @param connection the entity connection to use when inserting
   * @param entities the entities to insert
   * @return a new {@link Insert.Builder} instance
   */
  static Insert.Builder insertEntities(EntityConnection connection, Iterator<Entity> entities) {
    return new DefaultInsertEntities.DefaultBuilder(connection, entities);
  }

  /**
   * Copies a set of entities between a source and destination connection, performing a commit after each {@code batchSize} number of inserts,
   * unless the destination connection has an open transaction.
   * @see #execute()
   */
  interface Copy {

    /**
     * Executes this copy operation
     * @throws DatabaseException in case of an exception
     */
    void execute() throws DatabaseException;

    /**
     * A builder for a {@link Copy} operation.
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
       * Specifies a condition to use when determining which entities of the given type to copy,
       * if none is specified all entities are copied.
       * @param condition the condition to use
       * @return this builder instance
       */
      Builder condition(Condition condition);

      /**
       * Builds and executes this copy operation
       * @throws DatabaseException in case of an exception
       */
      void execute() throws DatabaseException;

      /**
       * @return a new {@link Copy} instance
       */
      Copy build();
    }
  }

  /**
   * Inserts entities in batches, performing a commit after each {@code batchSize} number of inserts,
   * unless the destination connection has an open transaction.
   * @see #execute()
   */
  interface Insert {

    /**
     * Executes this batch insert
     * @throws DatabaseException in case of an exception
     */
    void execute() throws DatabaseException;

    /**
     * A builder for {@link Insert} operation.
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
      Builder onInsert(Consumer<Collection<Entity.Key>> onInsert);

      /**
       * Builds and executes this insert operation
       * @throws DatabaseException in case of an exception
       */
      void execute() throws DatabaseException;

      /**
       * @return a new {@link Insert} instance
       */
      Insert build();
    }
  }

  /**
   * A class encapsulating select query parameters.
   * A factory class for {@link Builder} instances via
   * {@link Select#all(EntityType)}, {@link Select#where(Condition)}.
   */
  interface Select {

    /**
     * @return the where condition
     */
    Condition where();

    /**
     * @return the OrderBy for this condition, an empty Optional if none is specified
     */
    Optional<OrderBy> orderBy();

    /**
     * @return the limit to use for the given condition, -1 for no limit
     */
    int limit();

    /**
     * @return the offset to use for the given condition, -1 for no offset
     */
    int offset();

    /**
     * @return true if this select should lock the result for update
     */
    boolean forUpdate();

    /**
     * @return the query timeout
     */
    int queryTimeout();

    /**
     * @return the global fetch depth limit for this condition, an empty Optional if none has been specified
     */
    Optional<Integer> fetchDepth();

    /**
     * Returns the number of levels of foreign key values to fetch, with 0 meaning no referenced entities
     * should be fetched, -1 no limit and an empty Optional if unspecified (use default).
     * @param foreignKey the foreign key
     * @return the number of levels of foreign key values to fetch
     */
    Optional<Integer> fetchDepth(ForeignKey foreignKey);

    /**
     * Returns a map containing the number of levels of foreign key values to fetch per foreign key,
     * with 0 meaning no referenced entities should be fetched, -1 no limit.
     * @return a map containing the number of levels of foreign key values to fetch for each foreign key
     */
    Map<ForeignKey, Integer> foreignKeyFetchDepths();

    /**
     * @return the attributes to include in the query result,
     * an empty Collection if all should be included
     */
    Collection<Attribute<?>> attributes();

    /**
     * Builds a {@link Select}.
     */
    interface Builder {

      /**
       * Sets the OrderBy for this condition
       * @param orderBy the OrderBy to use when applying this condition
       * @return this builder instance
       */
      Builder orderBy(OrderBy orderBy);

      /**
       * @param limit the limit to use for this condition
       * @return this builder instance
       */
      Builder limit(int limit);

      /**
       * @param offset the offset to use for this condition
       * @return this builder instance
       */
      Builder offset(int offset);

      /**
       * Marks Select instance as a for update query, this means the resulting rows
       * will be locked by the given connection until unlocked by running another (non select for update)
       * query on the same connection or performing an update.
       * Note that marking this Select instance as for update, sets the {@link #fetchDepth()} to zero, which can
       * then be modified by setting it after setting forUpdate.
       * @return this builder instance
       */
      Builder forUpdate();

      /**
       * Limit the levels of foreign keys to fetch
       * @param fetchDepth the foreign key fetch depth limit
       * @return this builder instance
       */
      Builder fetchDepth(int fetchDepth);

      /**
       * Limit the levels of foreign keys to fetch via the given foreign key
       * @param foreignKey the foreign key
       * @param fetchDepth the foreign key fetch depth limit
       * @return this builder instance
       */
      Builder fetchDepth(ForeignKey foreignKey, int fetchDepth);

      /**
       * Sets the attributes to include in the query result. An empty array means all attributes should be included.
       * Note that primary key attribute values are always included.
       * @param attributes the attributes to include
       * @param <T> the attribute type
       * @return this builder instance
       */
      <T extends Attribute<?>> Builder attributes(T... attributes);

      /**
       * Sets the attributes to include in the query result. An empty Collection means all attributes should be included.
       * Note that primary key attribute values are always included.
       * @param attributes the attributes to include
       * @return this builder instance
       */
      Builder attributes(Collection<? extends Attribute<?>> attributes);

      /**
       * @param queryTimeout the query timeout, 0 for no timeout
       * @return this builder instance
       */
      Builder queryTimeout(int queryTimeout);

      /**
       * @return a new {@link Select} instance based on this builder
       */
      Select build();
    }

    /**
     * @param entityType the entity type
     * @return a new {@link Builder} instance
     */
    static Builder all(EntityType entityType) {
      return new DefaultSelect.DefaultBuilder(Condition.all(entityType));
    }

    /**
     * @param condition the where condition
     * @return a new {@link Builder} instance
     */
    static Builder where(Condition condition) {
      return new DefaultSelect.DefaultBuilder(condition);
    }
  }

  /**
   * A class encapsulating a where clause along with columns and their associated values for update.
   * A factory class for {@link Builder} instances via
   * {@link Update#all(EntityType)}, {@link Update#where(Condition)}.
   */
  interface Update {

    /**
     * @return the where condition
     */
    Condition where();

    /**
     * @return an unmodifiable view of the new values mapped to their respective columns
     */
    Map<Column<?>, Object> columnValues();

    /**
     * Builds an {@link Update}.
     */
    interface Builder {

      /**
       * Adds a column value to update
       * @param column the column
       * @param value the new value
       * @param <T> the value type
       * @return this builder
       * @throws IllegalStateException in case a value has already been added for the given column
       */
      <T> Builder set(Column<?> column, T value);

      /**
       * @return a new {@link Update} instance based on this builder
       */
      Update build();
    }

    /**
     * @param entityType the entity type
     * @return a {@link Builder} instance
     */
    static Builder all(EntityType entityType) {
      return new DefaultUpdate.DefaultBuilder(Condition.all(entityType));
    }

    /**
     * @param condition the where condition
     * @return a {@link Builder} instance
     */
    static Builder where(Condition condition) {
      return new DefaultUpdate.DefaultBuilder(condition);
    }
  }
}
