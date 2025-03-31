/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportType;
import is.codion.common.user.User;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A connection to a database, for querying and manipulating {@link Entity}s and running database
 * operations specified by a single {@link Domain} model.
 * {@link #execute(FunctionType)} and {@link #execute(ProcedureType)}
 * do not perform any transaction control whereas the select, insert, update and delete methods
 * perform a commit unless they are run within a transaction.
 * A static helper class for mass data manipulation.
 * @see #transaction(EntityConnection, Transactional)
 * @see #transaction(EntityConnection, TransactionalResult)
 */
public interface EntityConnection extends AutoCloseable {

	/**
	 * The default query timeout in seconds
	 */
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
	boolean connected();

	/**
	 * Performs a rollback and disconnects this connection
	 */
	void close();

	/**
	 * @return true if a transaction is open, false otherwise
	 */
	boolean transactionOpen();

	/**
	 * Starts a transaction on this connection.
	 * <p>
	 * NOTE: A transaction should ALWAYS be used in conjunction with a try/catch block,<br>
	 * in order for the transaction to be properly ended in case of an exception.<br>
	 * A transaction should always be started OUTSIDE the try/catch block.
	 * {@snippet :
	 * EntityConnection connection = connectionProvider().connection();
	 *
	 * connection.startTransaction(); // Very important, should NOT be inside the try block
	 * try {
	 *     connection.insert(entity);
	 *
	 *     connection.commitTransaction();
	 * }
	 * catch (DatabaseException e) {
	 *     connection.rollbackTransaction();
	 *     throw e;
	 * }
	 * catch (Exception e) {          // Very important to catch Exception
	 *     connection.rollbackTransaction();
	 *     throw new RuntimeException(e);
	 * }
	 * }
	 * @throws IllegalStateException if a transaction is already open
	 * @see #transaction(EntityConnection, Transactional)
	 * @see #transaction(EntityConnection, TransactionalResult)
	 */
	void startTransaction();

	/**
	 * Performs a rollback and ends the current transaction
	 * @throws DatabaseException in case the rollback failed
	 * @throws IllegalStateException in case a transaction is not open
	 * @see #transaction(EntityConnection, Transactional)
	 * @see #transaction(EntityConnection, TransactionalResult)
	 */
	void rollbackTransaction();

	/**
	 * Performs a commit and ends the current transaction
	 * @throws DatabaseException in case the commit failed
	 * @throws IllegalStateException in case a transaction is not open
	 * @see #transaction(EntityConnection, Transactional)
	 * @see #transaction(EntityConnection, TransactionalResult)
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
	<C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType);

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
	<C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType, T argument);

	/**
	 * Executes the procedure with the given type with no arguments
	 * @param procedureType the procedure type
	 * @param <C> the connection type
	 * @param <T> the procedure argument type
	 * @throws DatabaseException in case anything goes wrong during the execution
	 */
	<C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType);

	/**
	 * Executes the procedure with the given type
	 * @param procedureType the procedure type
	 * @param argument the procedure argument
	 * @param <C> the connection type
	 * @param <T> the argument type
	 * @throws DatabaseException in case anything goes wrong during the execution
	 */
	<C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType, T argument);

	/**
	 * Inserts the given entity, returning the primary key.
	 * Performs a commit unless a transaction is open.
	 * @param entity the entity to insert
	 * @return the primary key of the inserted entity
	 * @throws DatabaseException in case of a database exception
	 */
	Entity.Key insert(Entity entity);

	/**
	 * Inserts the given entity, returning the inserted entity.
	 * Performs a commit unless a transaction is open.
	 * @param entity the entity to insert
	 * @return the inserted entity
	 * @throws DatabaseException in case of a database exception
	 */
	Entity insertSelect(Entity entity);

	/**
	 * Inserts the given entities, returning the primary keys.
	 * Performs a commit unless a transaction is open.
	 * @param entities the entities to insert
	 * @return the primary keys of the inserted entities
	 * @throws DatabaseException in case of a database exception
	 */
	Collection<Entity.Key> insert(Collection<Entity> entities);

	/**
	 * Inserts the given entities, returning the inserted entities.
	 * Performs a commit unless a transaction is open.
	 * @param entities the entities to insert
	 * @return the inserted entities
	 * @throws DatabaseException in case of a database exception
	 */
	Collection<Entity> insertSelect(Collection<Entity> entities);

	/**
	 * Updates the given entity based on its attribute values.
	 * Throws an exception if the given entity is unmodified.
	 * Performs a commit unless a transaction is open.
	 * @param entity the entity to update
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.UpdateException in case of an unmodified entity or if there is a mismatch between expected and actual number of updated rows
	 * @throws is.codion.common.db.exception.RecordModifiedException in case the entity has been modified or deleted by another user
	 */
	void update(Entity entity);

	/**
	 * Updates the given entity based on its attribute values. Returns the updated entity.
	 * Throws an exception if the given entity is unmodified.
	 * Performs a commit unless a transaction is open.
	 * @param entity the entity to update
	 * @return the updated entity
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.UpdateException in case of an unmodified entity or if there is a mismatch between expected and actual number of updated rows
	 * @throws is.codion.common.db.exception.RecordModifiedException in case the entity has been modified or deleted by another user
	 */
	Entity updateSelect(Entity entity);

	/**
	 * Updates the given entities based on their attribute values.
	 * Throws an exception if any of the given entities is unmodified.
	 * Performs a commit unless a transaction is open.
	 * @param entities the entities to update
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.UpdateException in case of an unmodified entity or if there is a mismatch between expected and actual number of updated rows
	 * @throws is.codion.common.db.exception.RecordModifiedException in case an entity has been modified or deleted by another user
	 */
	void update(Collection<Entity> entities);

	/**
	 * Updates the given entities based on their attribute values. Returns the updated entities, in no particular order.
	 * Throws an exception if any of the given entities is unmodified.
	 * Performs a commit unless a transaction is open.
	 * @param entities the entities to update
	 * @return the updated entities, in no particular order
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.UpdateException in case of an unmodified entity or if there is a mismatch between expected and actual number of updated rows
	 * @throws is.codion.common.db.exception.RecordModifiedException in case an entity has been modified or deleted by another user
	 */
	Collection<Entity> updateSelect(Collection<Entity> entities);

	/**
	 * Performs an update based on the given update, updating the columns found
	 * in the {@link Update#values()} map, using the associated value.
	 * @param update the update to perform
	 * @return the number of affected rows
	 * @throws DatabaseException in case of a database exception
	 */
	int update(Update update);

	/**
	 * Deletes the entity with the given primary key.
	 * Performs a commit unless a transaction is open.
	 * @param key the primary key of the entity to delete
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.DeleteException in case no row or multiple rows were deleted
	 */
	void delete(Entity.Key key);

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
	void delete(Collection<Entity.Key> keys);

	/**
	 * Deletes the entities specified by the given condition.
	 * Performs a commit unless a transaction is open.
	 * @param condition the condition specifying the entities to delete
	 * @return the number of deleted rows
	 * @throws DatabaseException in case of a database exception
	 */
	int delete(Condition condition);

	/**
	 * Selects ordered and distinct non-null values of the given column.
	 * @param column column
	 * @param <T> the value type
	 * @return the values of the given column
	 * @throws DatabaseException in case of a database exception
	 * @throws IllegalArgumentException in case the given column has not associated with a table column
	 * @throws UnsupportedOperationException in case the entity uses a custom column clause or if the column represents an aggregate value
	 */
	<T> List<T> select(Column<T> column);

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
	<T> List<T> select(Column<T> column, Condition condition);

	/**
	 * Selects distinct non-null values of the given column. If the select provides no
	 * order by clause the result is ordered by the selected column.
	 * @param column column
	 * @param select the select to perform
	 * @param <T> the value type
	 * @return the values of the given column
	 * @throws DatabaseException in case of a database exception
	 * @throws IllegalArgumentException in case the column and select condition entity types don't match
	 * @throws UnsupportedOperationException in case the entity uses a custom column clause or if the column represents an aggregate value
	 */
	<T> List<T> select(Column<T> column, Select select);

	/**
	 * Selects an entity by key
	 * @param key the key of the entity to select
	 * @return an entity having the key {@code key}
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
	 * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
	 */
	Entity select(Entity.Key key);

	/**
	 * Selects a single entity based on the specified condition
	 * @param condition the condition specifying the entity to select
	 * @return the entities based on the given condition
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
	 * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
	 */
	Entity selectSingle(Condition condition);

	/**
	 * Selects a single entity based on the specified select
	 * @param select the select to perform
	 * @return the entities based on the given select
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.RecordNotFoundException in case the entity was not found
	 * @throws is.codion.common.db.exception.MultipleRecordsFoundException in case multiple entities were found
	 */
	Entity selectSingle(Select select);

	/**
	 * Selects entities based on the given {@code keys}
	 * @param keys the keys used in the condition
	 * @return entities based on {@code keys}
	 * @throws DatabaseException in case of a database exception
	 */
	Collection<Entity> select(Collection<Entity.Key> keys);

	/**
	 * Selects entities based on the given condition
	 * @param condition the condition specifying which entities to select
	 * @return entities based to the given condition
	 * @throws DatabaseException in case of a database exception
	 */
	List<Entity> select(Condition condition);

	/**
	 * Selects entities based on the given select
	 * @param select the select to perform
	 * @return entities based to the given select
	 * @throws DatabaseException in case of a database exception
	 */
	List<Entity> select(Select select);

	/**
	 * Selects the entities that depend on the given entities via (non-soft) foreign keys, mapped to corresponding entityTypes
	 * @param entities the entities for which to retrieve dependencies, must be of same type
	 * @return the entities that depend on {@code entities}
	 * @throws IllegalArgumentException in case the entities are not of the same type
	 * @throws DatabaseException in case of a database exception
	 * @see ForeignKeyDefinition#soft()
	 */
	Map<EntityType, Collection<Entity>> dependencies(Collection<Entity> entities);

	/**
	 * Counts the number of rows returned based on the given count conditions
	 * @param count the count conditions
	 * @return the number of rows fitting the given count conditions
	 * @throws DatabaseException in case of a database exception
	 */
	int count(Count count);

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
	 * @see Report#fill(java.sql.Connection, Object)
	 */
	<T, R, P> R report(ReportType<T, R, P> reportType, P reportParameters);

	/**
	 * Executes the given {@link Transactional} instance within a transaction on the given connection, committing on success and rolling back on exception.
	 * Any {@link DatabaseException}s, {@link RuntimeException}s or {@link Error}s encountered are rethrown, other exceptions are rethrown wrapped in a {@link RuntimeException}.
	 * Note that nesting transactions will cause an {@link IllegalStateException} to be thrown, causing the outer transaction to be rolled back.
	 * {@snippet :
	 * EntityConnection connection = connection();
	 * transaction(connection, () -> {
	 * 	 // Delete the playlist tracks
	 *   connection.delete(PlaylistTrack.PLAYLIST_FK.in(playlists));
	 * 	 // Then delete the playlists
	 *   connection.delete(primaryKeys(playlists));
	 * });
	 * }
	 * @param connection the connection to use
	 * @param transactional the transactional to run
	 * @throws DatabaseException in case of a database exception
	 * @throws RuntimeException in case of exceptions other than {@link DatabaseException}
	 */
	static void transaction(EntityConnection connection, Transactional transactional) {
		requireNonNull(connection);
		requireNonNull(transactional);
		transaction(connection, () -> {
			transactional.execute();
			return null;
		});
	}

	/**
	 * Executes the given {@link TransactionalResult} instance within a transaction on the given connection, committing on success and rolling back on exception.
	 * Any {@link DatabaseException}s, {@link RuntimeException}s or {@link Error}s encountered are rethrown, other exceptions are rethrown wrapped in a {@link RuntimeException}.
	 * Note that nesting transactions will cause an {@link IllegalStateException} to be thrown, causing the outer transaction to be rolled back.
	 * {@snippet :
	 * EntityConnection connection = connection();
	 * Entity randomPlaylist = transaction(connection, () ->
	 *   connection.execute(Playlist.RANDOM_PLAYLIST, parameters));
	 * }
	 * @param <T> the result type
	 * @param connection the connection to use
	 * @param transactional the transactional to run
	 * @return the result
	 * @throws DatabaseException in case of a database exception
	 * @throws RuntimeException in case of exceptions other than {@link DatabaseException}
	 */
	static <T> T transaction(EntityConnection connection, TransactionalResult<T> transactional) {
		requireNonNull(connection);
		requireNonNull(transactional);
		connection.startTransaction();
		try {
			T result = transactional.execute();
			connection.commitTransaction();

			return result;
		}
		catch (Throwable e) {
			connection.rollbackTransaction();
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			if (e instanceof Error) {
				throw (Error) e;
			}

			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new {@link BatchCopy.Builder} instance for copying entities from source to destination, with a default batch size of 100.
	 * Performs a commit after each {code batchSize} number of inserts, unless the destination connection has an open transaction.
	 * Call {@link BatchCopy.Builder#execute()} to perform the copy operation.
	 * @param source the source connection
	 * @param destination the destination connection
	 * @return a new {@link BatchCopy.Builder} instance
	 */
	static BatchCopy.Builder batchCopy(EntityConnection source, EntityConnection destination) {
		return new DefaultBatchCopy.DefaultBuilder(source, destination);
	}

	/**
	 * Creates a new {@link BatchInsert} instance based on the given iterator, with a default batch size of 100.
	 * Performs a commit after each {@code batchSize} number of inserts, unless the destination connection has an open transaction.
	 * Call {@link BatchInsert#execute()} to perform the insert operation.
	 * @param connection the entity connection to use when inserting
	 * @param entities the entities to insert
	 * @return a new {@link BatchInsert.Builder} instance
	 */
	static BatchInsert.Builder batchInsert(EntityConnection connection, Iterator<Entity> entities) {
		return new DefaultBatchInsert.DefaultBuilder(connection, entities);
	}

	/**
	 * Specifies an action to be executed within a transaction.
	 */
	interface Transactional {

		/**
		 * Executes the given transactional.
		 * @throws Exception in case of an exception
		 */
		void execute() throws Exception;
	}

	/**
	 * Specifies an action to be executed within a transaction producing a result.
	 * @param <T> the result type
	 */
	interface TransactionalResult<T> {

		/**
		 * Executes the given transactional.
		 * @throws Exception in case of an exception
		 * @return the result
		 */
		T execute() throws Exception;
	}

	/**
	 * Copies a set of entities between a source and destination connection, performing a commit after each {@code batchSize} number of inserts,
	 * unless the destination connection has an open transaction.
	 * @see #execute()
	 */
	interface BatchCopy {

		/**
		 * Executes this copy operation
		 * @throws DatabaseException in case of an exception
		 */
		void execute();

		/**
		 * A builder for a {@link BatchCopy} operation.
		 */
		interface Builder {

			/**
			 * @param entityTypes the entity types to copy
			 * @return this builder instance
			 */
			Builder entityTypes(EntityType... entityTypes);

			/**
			 * @param conditions the conditions to use when determining which entities of the given type to copy
			 * @return this builder instance
			 */
			Builder conditions(Condition... conditions);

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
			 * Builds and executes this copy operation
			 * @throws DatabaseException in case of an exception
			 */
			void execute();

			/**
			 * @return a new {@link BatchCopy} instance
			 */
			BatchCopy build();
		}
	}

	/**
	 * Inserts entities in batches, performing a commit after each {@code batchSize} number of inserts,
	 * unless the destination connection has an open transaction.
	 * @see #execute()
	 */
	interface BatchInsert {

		/**
		 * Executes this batch insert
		 * @throws DatabaseException in case of an exception
		 */
		void execute();

		/**
		 * A builder for {@link BatchInsert} operation.
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
			void execute();

			/**
			 * @return a new {@link BatchInsert} instance
			 */
			BatchInsert build();
		}
	}

	/**
	 * A class encapsulating select query parameters.
	 * A factory for {@link Builder} instances via
	 * {@link Select#all(EntityType)}, {@link Select#where(Condition)}.
	 */
	interface Select {

		/**
		 * @return the WHERE condition
		 */
		Condition where();

		/**
		 * @return the HAVING condition
		 */
		Condition having();

		/**
		 * @return the {@link OrderBy} for this condition, an empty Optional if none is specified
		 */
		Optional<OrderBy> orderBy();

		/**
		 * @return the LIMIT to use for the given condition, an empty Optional for no limit
		 */
		OptionalInt limit();

		/**
		 * @return the OFFSET to use for the given condition, an empty Optional for no offset
		 */
		OptionalInt offset();

		/**
		 * @return true if this select should lock the result FOR UPDATE
		 */
		boolean forUpdate();

		/**
		 * @return the query timeout
		 */
		int queryTimeout();

		/**
		 * @return the global fetch depth limit for this condition, an empty Optional if none has been specified
		 */
		OptionalInt fetchDepth();

		/**
		 * Returns the number of levels of foreign key values to fetch, with 0 meaning the referenced entity
		 * should not be fetched, -1 no limit and an empty Optional if the global limit should be used ({@link #fetchDepth()}).
		 * @param foreignKey the foreign key
		 * @return the number of levels of foreign key values to fetch
		 */
		OptionalInt fetchDepth(ForeignKey foreignKey);

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
			 * Sets the {@link OrderBy} for this condition
			 * @param orderBy the {@link OrderBy} to use when applying this condition
			 * @return this builder instance
			 */
			Builder orderBy(OrderBy orderBy);

			/**
			 * @param limit the LIMIT to use for this condition, null for no limit
			 * @return this builder instance
			 */
			Builder limit(Integer limit);

			/**
			 * @param offset the OFFSET to use for this condition, null for no offset
			 * @return this builder instance
			 */
			Builder offset(Integer offset);

			/**
			 * Marks the Select instance as a FOR UPDATE query, this means the resulting rows
			 * will be locked by the given connection until unlocked by running another (non-select for update)
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
			 * The HAVING condition. Note that this condition
			 * must be based on aggregate function columns
			 * @param having the HAVING condition
			 * @return this builder instance
			 * @see ColumnDefinition#aggregate()
			 */
			Builder having(Condition having);

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
		 * @param condition the WHERE condition
		 * @return a new {@link Builder} instance
		 */
		static Builder where(Condition condition) {
			return new DefaultSelect.DefaultBuilder(condition);
		}
	}

	/**
	 * A class encapsulating a where clause along with columns and their associated values for update.
	 * A factory for {@link Builder} instances via
	 * {@link Update#all(EntityType)}, {@link Update#where(Condition)}.
	 */
	interface Update {

		/**
		 * @return the WHERE condition
		 */
		Condition where();

		/**
		 * @return an unmodifiable view of the new values mapped to their respective columns
		 */
		Map<Column<?>, Object> values();

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
			 * @throws IllegalStateException in case no values have been specified
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
		 * @param condition the WHERE condition
		 * @return a {@link Builder} instance
		 */
		static Builder where(Condition condition) {
			return new DefaultUpdate.DefaultBuilder(condition);
		}
	}

	/**
	 * A class encapsulating count query parameters.
	 * A factory for {@link Count} instances via {@link Count#all(EntityType)},
	 * {@link Count#where(Condition)} and {@link Count#having(Condition)}.
	 * A factory for {@link Count.Builder} instances via {@link Count#builder(Condition)}.
	 */
	interface Count {

		/**
		 * @return the WHERE condition
		 */
		Condition where();

		/**
		 * @return the HAVING condition
		 */
		Condition having();

		/**
		 * Builds a {@link Count} instance.
		 */
		interface Builder {

			/**
			 * @param having the HAVING condition
			 * @return this builder instance
			 */
			Builder having(Condition having);

			/**
			 * @return a new {@link Count} instance based on this builder
			 */
			Count build();
		}

		/**
		 * @param entityType the entity type
		 * @return a {@link Count} instance
		 */
		static Count all(EntityType entityType) {
			return where(Condition.all(entityType));
		}

		/**
		 * @param condition the WHERE condition
		 * @return a {@link Count} instance
		 */
		static Count where(Condition condition) {
			requireNonNull(condition);

			return new DefaultCount.DefaultBuilder(condition).build();
		}

		/**
		 * @param condition the HAVING condition
		 * @return a {@link Count} instance
		 */
		static Count having(Condition condition) {
			requireNonNull(condition);

			return new DefaultCount.DefaultBuilder(Condition.all(condition.entityType()))
							.having(condition)
							.build();
		}

		/**
		 * @param where the WHERE condition
		 * @return a {@link Count.Builder} instance
		 */
		static Builder builder(Condition where) {
			return new DefaultCount.DefaultBuilder(where);
		}
	}
}
