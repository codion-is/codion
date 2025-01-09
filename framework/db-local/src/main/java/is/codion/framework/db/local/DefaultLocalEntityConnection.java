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
package is.codion.framework.db.local;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.DeleteException;
import is.codion.common.db.exception.MultipleRecordsFoundException;
import is.codion.common.db.exception.RecordModifiedException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.db.exception.UpdateException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.db.result.ResultPacker;
import is.codion.common.logging.MethodLogger;
import is.codion.common.resource.MessageBundle;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKey.Reference;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.condition.Condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static is.codion.common.db.connection.DatabaseConnection.SQL_STATE_NO_DATA;
import static is.codion.common.db.database.Database.Operation.*;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.db.local.Queries.*;
import static is.codion.framework.domain.entity.Entity.Key;
import static is.codion.framework.domain.entity.Entity.Key.groupByType;
import static is.codion.framework.domain.entity.Entity.groupByType;
import static is.codion.framework.domain.entity.Entity.originalPrimaryKeys;
import static is.codion.framework.domain.entity.Entity.primaryKeyMap;
import static is.codion.framework.domain.entity.Entity.primaryKeys;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.condition.Condition.*;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.*;

final class DefaultLocalEntityConnection implements LocalEntityConnection {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultLocalEntityConnection.class);

	private static final MessageBundle MESSAGES =
					messageBundle(LocalEntityConnection.class, getBundle(LocalEntityConnection.class.getName()));
	private static final String EXECUTE_UPDATE = "executeUpdate";
	private static final String EXECUTE_QUERY = "executeQuery";
	private static final String RECORD_MODIFIED = "record_modified";
	private static final String ENTITIES = "entities may not be null";
	private static final String ENTITY = "entity may not be null";
	private static final String SELECT_MAY_NOT_BE_NULL = "select may not be null";
	private static final String PACK_RESULT = "packResult";
	private static final String EXECUTE = "execute";
	private static final String REPORT = "report";
	private static final Function<Entity, Entity> IMMUTABLE = Entity::immutable;

	private final Domain domain;
	private final DatabaseConnection connection;
	private final Database database;
	private final SelectQueries selectQueries;
	private final Map<EntityType, List<ColumnDefinition<?>>> insertableColumnsCache = new HashMap<>();
	private final Map<EntityType, List<ColumnDefinition<?>>> updatableColumnsCache = new HashMap<>();
	private final Map<EntityType, List<ForeignKeyDefinition>> hardForeignKeyReferenceCache = new HashMap<>();
	private final Map<EntityType, List<Attribute<?>>> primaryKeyAndWritableColumnsCache = new HashMap<>();
	private final Map<Select, List<Entity>> queryCache = new HashMap<>();

	private boolean optimisticLocking = LocalEntityConnection.OPTIMISTIC_LOCKING.getOrThrow();
	private boolean limitForeignKeyFetchDepth = LocalEntityConnection.LIMIT_FOREIGN_KEY_FETCH_DEPTH.getOrThrow();
	private int defaultQueryTimeout = LocalEntityConnection.QUERY_TIMEOUT_SECONDS.getOrThrow();
	private boolean queryCacheEnabled = false;

	/**
	 * Constructs a new LocalEntityConnection instance
	 * @param database the Database instance
	 * @param domain the domain model
	 * @param user the user used for connecting to the database
	 * @throws DatabaseException in case there is a problem connecting to the database
	 * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
	 */
	DefaultLocalEntityConnection(Database database, Domain domain, User user) {
		this(domain, DatabaseConnection.databaseConnection(configureDatabase(database, domain), user));
	}

	/**
	 * Constructs a new LocalEntityConnection instance
	 * @param database the Database instance
	 * @param domain the domain model
	 * @param connection the Connection object to base this EntityConnection on, it is assumed to be in a valid state
	 */
	DefaultLocalEntityConnection(Database database, Domain domain, Connection connection) {
		this(domain, DatabaseConnection.databaseConnection(configureDatabase(database, domain), connection));
	}

	private DefaultLocalEntityConnection(Domain domain, DatabaseConnection connection) {
		this.domain = domain;
		this.connection = connection;
		this.database = connection.database();
		this.domain.configure(connection);
		this.selectQueries = new SelectQueries(connection.database());
	}

	@Override
	public Entities entities() {
		return domain.entities();
	}

	@Override
	public User user() {
		return connection.user();
	}

	@Override
	public boolean connected() {
		synchronized (connection) {
			return connection.valid();
		}
	}

	@Override
	public void close() {
		synchronized (connection) {
			connection.close();
		}
	}

	@Override
	public void startTransaction() {
		synchronized (connection) {
			connection.startTransaction();
		}
	}

	@Override
	public boolean transactionOpen() {
		synchronized (connection) {
			return connection.transactionOpen();
		}
	}

	@Override
	public void rollbackTransaction() {
		synchronized (connection) {
			try {
				connection.rollbackTransaction();
			}
			catch (SQLException e) {
				LOG.error("Exception during transaction rollback", e);
				throw new DatabaseException(e);
			}
		}
	}

	@Override
	public void commitTransaction() {
		synchronized (connection) {
			try {
				connection.commitTransaction();
			}
			catch (SQLException e) {
				LOG.error("Exception during transaction commit", e);
				throw new DatabaseException(e);
			}
		}
	}

	@Override
	public void setQueryCacheEnabled(boolean queryCacheEnabled) {
		synchronized (connection) {
			this.queryCacheEnabled = queryCacheEnabled;
			if (!queryCacheEnabled) {
				queryCache.clear();
			}
		}
	}

	@Override
	public boolean isQueryCacheEnabled() {
		synchronized (connection) {
			return queryCacheEnabled;
		}
	}

	@Override
	public Key insert(Entity entity) {
		return insert(singletonList(requireNonNull(entity, ENTITY))).iterator().next();
	}

	@Override
	public Entity insertSelect(Entity entity) {
		return insertSelect(singletonList(requireNonNull(entity, ENTITY))).iterator().next();
	}

	@Override
	public Collection<Key> insert(Collection<Entity> entities) {
		if (requireNonNull(entities, ENTITIES).isEmpty()) {
			return emptyList();
		}

		return insert(entities, null);
	}

	@Override
	public Collection<Entity> insertSelect(Collection<Entity> entities) {
		if (requireNonNull(entities, ENTITIES).isEmpty()) {
			return emptyList();
		}
		Collection<Entity> insertedEntities = new ArrayList<>(entities.size());
		insert(entities, insertedEntities);

		return insertedEntities;
	}

	@Override
	public void update(Entity entity) {
		update(singletonList(requireNonNull(entity, ENTITY)));
	}

	@Override
	public Entity updateSelect(Entity entity) {
		return updateSelect(singletonList(requireNonNull(entity, ENTITY))).iterator().next();
	}

	@Override
	public void update(Collection<Entity> entities) {
		update(entities, null);
	}

	@Override
	public Collection<Entity> updateSelect(Collection<Entity> entities) {
		if (requireNonNull(entities, ENTITIES).isEmpty()) {
			return emptyList();
		}
		Collection<Entity> updatedEntities = new ArrayList<>(entities.size());
		update(entities, updatedEntities);

		return updatedEntities;
	}

	@Override
	public int update(Update update) {
		if (requireNonNull(update, "update may not be null").values().isEmpty()) {
			throw new IllegalArgumentException("Update requires one or more values");
		}
		throwIfReadOnly(update.where().entityType());

		List<Object> statementValues = new ArrayList<>();
		List<ColumnDefinition<?>> statementColumns = new ArrayList<>();
		String updateQuery = createUpdateQuery(update, statementColumns, statementValues);
		synchronized (connection) {
			try (PreparedStatement statement = prepareStatement(updateQuery)) {
				int updatedRows = executeUpdate(statement, updateQuery, statementColumns, statementValues, UPDATE);
				commitIfTransactionIsNotOpen();

				return updatedRows;
			}
			catch (SQLException e) {
				rollbackQuietlyIfTransactionIsNotOpen();
				LOG.error(createLogMessage(updateQuery, statementValues, statementColumns, e), e);
				throw database.exception(e, UPDATE);
			}
		}
	}

	@Override
	public int delete(Condition condition) {
		throwIfReadOnly(requireNonNull(condition, "Delete condition may not be null").entityType());

		EntityDefinition entityDefinition = definition(condition.entityType());
		List<?> statementValues = condition.values();
		List<ColumnDefinition<?>> statementColumns = definitions(condition.columns());
		String deleteQuery = deleteQuery(entityDefinition.tableName(), condition.toString(entityDefinition));
		synchronized (connection) {
			try (PreparedStatement statement = prepareStatement(deleteQuery)) {
				int deleteCount = executeUpdate(statement, deleteQuery, statementColumns, statementValues, DELETE);
				commitIfTransactionIsNotOpen();

				return deleteCount;
			}
			catch (SQLException e) {
				rollbackQuietlyIfTransactionIsNotOpen();
				LOG.error(createLogMessage(deleteQuery, statementValues, statementColumns, e), e);
				throw database.exception(e, DELETE);
			}
		}
	}

	@Override
	public void delete(Key key) {
		delete(singletonList(requireNonNull(key, "key may not be null")));
	}

	@Override
	public void delete(Collection<Key> keys) {
		if (requireNonNull(keys, "keys may not be null").isEmpty()) {
			return;
		}
		Map<EntityType, List<Key>> keysByEntityType = groupByType(keys);
		throwIfReadOnly(keysByEntityType.keySet());

		List<?> statementValues = emptyList();
		List<ColumnDefinition<?>> statementColumns = emptyList();
		PreparedStatement statement = null;
		Condition condition = null;
		String deleteQuery = null;
		synchronized (connection) {
			try {
				int deleteCount = 0;
				for (Map.Entry<EntityType, List<Key>> entityTypeKeys : keysByEntityType.entrySet()) {
					EntityDefinition entityDefinition = definition(entityTypeKeys.getKey());
					List<Key> keysToDelete = entityTypeKeys.getValue();
					int keysPerStatement = keysPerStatement(keysToDelete.get(0));
					for (int i = 0; i < keysToDelete.size(); i += keysPerStatement) {
						condition = keys(keysToDelete.subList(i, Math.min(i + keysPerStatement, keysToDelete.size())));
						statementValues = condition.values();
						statementColumns = definitions(condition.columns());
						deleteQuery = deleteQuery(entityDefinition.tableName(), condition.toString(entityDefinition));
						statement = prepareStatement(deleteQuery);
						deleteCount += executeUpdate(statement, deleteQuery, statementColumns, statementValues, DELETE);
						statement.close();
					}
				}
				if (keys.size() != deleteCount) {
					throw new DeleteException(deleteCount + " rows deleted, expected " + keys.size());
				}
				commitIfTransactionIsNotOpen();
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();
				LOG.error(createLogMessage(deleteQuery, statementValues, statementColumns, exception), exception);
				throwDatabaseException(exception, DELETE);
				throw runtimeException(exception);
			}
			finally {
				closeSilently(statement);
			}
		}
	}

	@Override
	public Entity select(Key key) {
		return selectSingle(key(key));
	}

	@Override
	public Entity selectSingle(Condition condition) {
		return selectSingle(where(condition).build());
	}

	@Override
	public Entity selectSingle(Select select) {
		List<Entity> entities = select(select);
		if (entities.isEmpty()) {
			throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
		}
		if (entities.size() > 1) {
			throw new MultipleRecordsFoundException(MESSAGES.getString("multiple_records_found"));
		}

		return entities.get(0);
	}

	@Override
	public Collection<Entity> select(Collection<Key> keys) {
		if (requireNonNull(keys, "keys may not be null").isEmpty()) {
			return emptyList();
		}

		synchronized (connection) {
			try {
				List<Entity> result = new ArrayList<>();
				for (List<Key> entityTypeKeys : groupByType(keys).values()) {
					result.addAll(query(where(keys(entityTypeKeys)).build()));
				}
				commitIfTransactionIsNotOpen();

				return result;
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();
				throwDatabaseException(exception, SELECT);
				throw runtimeException(exception);
			}
		}
	}

	@Override
	public List<Entity> select(Condition condition) {
		return select(where(condition).build());
	}

	@Override
	public List<Entity> select(Select select) {
		requireNonNull(select, SELECT_MAY_NOT_BE_NULL);
		synchronized (connection) {
			try {
				List<Entity> result = query(select);
				if (!select.forUpdate()) {
					commitIfTransactionIsNotOpen();
				}

				return result;
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();
				throwDatabaseException(exception, SELECT);
				throw runtimeException(exception);
			}
		}
	}

	@Override
	public <T> List<T> select(Column<T> column) {
		return select(requireNonNull(column, "column may not be null"), Select.all(column.entityType())
						.orderBy(ascending(column))
						.build());
	}

	@Override
	public <T> List<T> select(Column<T> column, Condition condition) {
		return select(column, where(condition)
						.orderBy(ascending(column))
						.build());
	}

	@Override
	public <T> List<T> select(Column<T> column, Select select) {
		EntityDefinition entityDefinition = definition(requireNonNull(column, "column may not be null").entityType());
		if (!requireNonNull(select, SELECT_MAY_NOT_BE_NULL).where().entityType().equals(column.entityType())) {
			throw new IllegalArgumentException("Condition entity type " + select.where().entityType() +
							" does not match Column entity type " + column.entityType());
		}
		ColumnDefinition<T> columnDefinition = entityDefinition.columns().definition(column);
		verifyColumnIsSelectable(columnDefinition, entityDefinition);
		Condition combinedCondition = and(select.where(), column.isNotNull());
		String selectQuery = selectQueries.builder(entityDefinition)
						.select(select, false)
						.columns(columnDefinition.expression())
						.where(combinedCondition)
						.groupBy(columnDefinition.expression())
						.build();
		List<Object> statementValues = statementValues(combinedCondition, select.having());
		List<ColumnDefinition<?>> statementColumns = statementColumns(combinedCondition, select.having());
		synchronized (connection) {
			try (PreparedStatement statement = prepareStatement(selectQuery);
					 ResultSet resultSet = executeQuery(statement, selectQuery, statementColumns, statementValues)) {
				List<T> result = packResult(columnDefinition, resultSet);
				commitIfTransactionIsNotOpen();

				return result;
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();
				LOG.error(createLogMessage(selectQuery, statementValues, statementColumns, exception), exception);
				throwDatabaseException(exception, SELECT);
				throw runtimeException(exception);
			}
		}
	}

	@Override
	public int count(Count count) {
		EntityDefinition entityDefinition = definition(requireNonNull(count, "count may not be null").where().entityType());
		String selectQuery = selectQueries.builder(entityDefinition)
						.columns("COUNT(*)")
						.subquery(selectQueries.builder(entityDefinition)
										.select(where(count.where())
														.having(count.having())
														.attributes(entityDefinition.primaryKey().columns())
														.build())
										.build())
						.build();
		List<Object> statementValues = statementValues(count.where(), count.having());
		List<ColumnDefinition<?>> statementColumns = statementColumns(count.where(), count.having());
		synchronized (connection) {
			try (PreparedStatement statement = prepareStatement(selectQuery);
					 ResultSet resultSet = executeQuery(statement, selectQuery, statementColumns, statementValues)) {
				if (!resultSet.next()) {
					throw new SQLException("Row count query returned no value", SQL_STATE_NO_DATA);
				}
				int result = resultSet.getInt(1);
				commitIfTransactionIsNotOpen();

				return result;
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();
				LOG.error(createLogMessage(selectQuery, statementValues, statementColumns, exception), exception);
				throwDatabaseException(exception, SELECT);
				throw runtimeException(exception);
			}
		}
	}

	@Override
	public Map<EntityType, Collection<Entity>> dependencies(Collection<Entity> entities) {
		Set<EntityType> entityTypes = requireNonNull(entities, ENTITIES).stream()
						.map(Entity::entityType)
						.collect(toSet());
		if (entityTypes.isEmpty()) {
			return emptyMap();//no entities
		}
		if (entityTypes.size() > 1) {
			throw new IllegalArgumentException("All entities must be of the same type when selecting dependencies");
		}

		Map<EntityType, Collection<Entity>> dependencyMap = new HashMap<>();
		synchronized (connection) {
			try {
				for (ForeignKeyDefinition foreignKeyReference : hardForeignKeyReferences(entityTypes.iterator().next())) {
					List<Entity> dependencies = query(where(foreignKeyReference.attribute().in(entities)).build(), 0);//bypass caching
					if (!dependencies.isEmpty()) {
						dependencyMap.computeIfAbsent(foreignKeyReference.entityType(), k -> new HashSet<>()).addAll(dependencies);
					}
				}
				commitIfTransactionIsNotOpen();
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();
				throwDatabaseException(exception, SELECT);
				throw runtimeException(exception);
			}
		}

		return dependencyMap;
	}

	@Override
	public <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType) {
		return execute(functionType, null);
	}

	@Override
	public <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType, T argument) {
		requireNonNull(functionType, "functionType may not be null");
		Exception exception = null;
		logEntry(EXECUTE, functionType, argument);
		try {
			synchronized (connection) {
				return domain.function(functionType).execute((C) this, argument);
			}
		}
		catch (Exception e) {
			exception = e;
			LOG.error(createLogMessage(functionType.name(), argument instanceof List ? (List<?>) argument : singletonList(argument), emptyList(), e), e);
			throwDatabaseException(e, OTHER);
			throw runtimeException(e);
		}
		finally {
			logExit(EXECUTE, exception);
		}
	}

	@Override
	public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType) {
		execute(procedureType, null);
	}

	@Override
	public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType, T argument) {
		requireNonNull(procedureType, "procedureType may not be null");
		Exception exception = null;
		logEntry(EXECUTE, procedureType, argument);
		try {
			synchronized (connection) {
				domain.procedure(procedureType).execute((C) this, argument);
			}
		}
		catch (Exception e) {
			exception = e;
			rollbackQuietlyIfTransactionIsNotOpen();
			LOG.error(createLogMessage(procedureType.name(), argument instanceof List ? (List<?>) argument : singletonList(argument), emptyList(), e), e);
			throwDatabaseException(e, OTHER);
			throw runtimeException(e);
		}
		finally {
			logExit(EXECUTE, exception);
		}
	}

	@Override
	public <T, R, P> R report(ReportType<T, R, P> reportType, P reportParameters) {
		requireNonNull(reportType, "reportType may not be null");
		Exception exception = null;
		synchronized (connection) {
			logEntry(REPORT, reportType, reportParameters);
			try {
				R result = domain.report(reportType).fill(connection.getConnection(), reportParameters);
				commitIfTransactionIsNotOpen();

				return result;
			}
			catch (Exception e) {
				exception = e;
				rollbackQuietlyIfTransactionIsNotOpen();
				LOG.error(createLogMessage(null, singletonList(reportType), emptyList(), e), e);
				throwDatabaseException(e, SELECT);
				throw runtimeException(e);
			}
			finally {
				logExit(REPORT, exception);
			}
		}
	}

	@Override
	public DatabaseConnection databaseConnection() {
		return connection;
	}

	@Override
	public ResultIterator<Entity> iterator(Condition condition) {
		return iterator(where(condition).build());
	}

	@Override
	public ResultIterator<Entity> iterator(Select select) {
		synchronized (connection) {
			try {
				return resultIterator(select);
			}
			catch (SQLException e) {
				throw database.exception(e, SELECT);
			}
		}
	}

	@Override
	public boolean isOptimisticLocking() {
		return optimisticLocking;
	}

	@Override
	public void setOptimisticLocking(boolean optimisticLocking) {
		this.optimisticLocking = optimisticLocking;
	}

	@Override
	public boolean isLimitForeignKeyFetchDepth() {
		return limitForeignKeyFetchDepth;
	}

	@Override
	public void setLimitForeignKeyFetchDepth(boolean limitForeignKeyFetchDepth) {
		this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
	}

	@Override
	public int getDefaultQueryTimeout() {
		return defaultQueryTimeout;
	}

	@Override
	public void setDefaultQueryTimeout(int defaultQueryTimeout) {
		this.defaultQueryTimeout = defaultQueryTimeout;
	}

	private Collection<Key> insert(Collection<Entity> entities, Collection<Entity> insertedEntities) {
		throwIfReadOnly(entities);

		List<Key> insertedKeys = new ArrayList<>(entities.size());
		List<Object> statementValues = new ArrayList<>();
		List<ColumnDefinition<?>> statementColumns = new ArrayList<>();
		PreparedStatement statement = null;
		String insertQuery = null;
		synchronized (connection) {
			try {
				for (Entity entity : entities) {
					EntityDefinition entityDefinition = definition(entity.entityType());
					KeyGenerator keyGenerator = entityDefinition.primaryKey().generator();
					keyGenerator.beforeInsert(entity, connection);

					populateColumnsAndValues(entity, insertableColumns(entityDefinition, keyGenerator.inserted()),
									statementColumns, statementValues, columnDefinition -> entity.contains(columnDefinition.attribute()));
					if (keyGenerator.inserted() && statementColumns.isEmpty()) {
						throw new SQLException("Unable to insert entity " + entity.entityType() + ", no values to insert");
					}

					insertQuery = insertQuery(entityDefinition.tableName(), statementColumns);
					statement = prepareStatement(insertQuery, keyGenerator.returnGeneratedKeys());
					executeUpdate(statement, insertQuery, statementColumns, statementValues, INSERT);
					keyGenerator.afterInsert(entity, connection, statement);

					insertedKeys.add(entity.primaryKey());

					statement.close();
					statementColumns.clear();
					statementValues.clear();
				}
				if (insertedEntities != null) {
					for (List<Key> entityTypeKeys : groupByType(insertedKeys).values()) {
						insertedEntities.addAll(query(where(keys(entityTypeKeys)).build(), 0));//bypass caching
					}
				}
				commitIfTransactionIsNotOpen();

				return insertedKeys;
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();
				LOG.error(createLogMessage(insertQuery, statementValues, statementColumns, exception), exception);
				throwDatabaseException(exception, INSERT);
				throw runtimeException(exception);
			}
			finally {
				closeSilently(statement);
			}
		}
	}

	private void update(Collection<Entity> entities, Collection<Entity> updatedEntities) {
		Map<EntityType, List<Entity>> entitiesByEntityType = groupByType(entities);
		throwIfReadOnly(entitiesByEntityType.keySet());

		List<Object> statementValues = new ArrayList<>();
		List<ColumnDefinition<?>> statementColumns = new ArrayList<>();
		PreparedStatement statement = null;
		String updateQuery = null;
		synchronized (connection) {
			try {
				if (optimisticLocking) {
					performOptimisticLocking(entitiesByEntityType);
				}

				for (Map.Entry<EntityType, List<Entity>> entityTypeEntities : entitiesByEntityType.entrySet()) {
					EntityDefinition entityDefinition = definition(entityTypeEntities.getKey());
					List<ColumnDefinition<?>> updatableColumns = updatableColumns(entityDefinition);

					List<Entity> entitiesToUpdate = entityTypeEntities.getValue();
					for (Entity entity : entitiesToUpdate) {
						populateColumnsAndValues(entity, updatableColumns, statementColumns, statementValues,
										columnDefinition -> entity.modified(columnDefinition.attribute()));
						if (statementColumns.isEmpty()) {
							throw new UpdateException("Unable to update entity " + entity.entityType() + ", no modified values found");
						}

						Condition condition = key(entity.originalPrimaryKey());
						updateQuery = updateQuery(entityDefinition.tableName(), statementColumns, condition.toString(entityDefinition));
						statement = prepareStatement(updateQuery);
						statementColumns.addAll(definitions(condition.columns()));
						statementValues.addAll(condition.values());
						int updatedRows = executeUpdate(statement, updateQuery, statementColumns, statementValues, UPDATE);
						if (updatedRows == 0) {
							throw new UpdateException("Update did not affect any rows, entityType: " + entityTypeEntities.getKey());
						}

						statement.close();
						statementColumns.clear();
						statementValues.clear();
					}
					if (updatedEntities != null) {
						List<Entity> selected = query(where(keys(primaryKeys(entitiesToUpdate))).build(), 0);//bypass caching
						if (selected.size() != entitiesToUpdate.size()) {
							throw new UpdateException(entitiesToUpdate.size() + " updated rows expected, query returned " +
											selected.size() + ", entityType: " + entityTypeEntities.getKey());
						}
						updatedEntities.addAll(selected);
					}
				}
				commitIfTransactionIsNotOpen();
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();//releases the select for update lock
				if (exception instanceof RecordModifiedException) {
					LOG.debug(exception.getMessage(), exception);
					throw (RecordModifiedException) exception;
				}
				LOG.error(createLogMessage(updateQuery, statementValues, statementColumns, exception), exception);
				throwDatabaseException(exception, UPDATE);
				throw runtimeException(exception);
			}
			finally {
				closeSilently(statement);
			}
		}
	}

	/**
	 * Selects the given entities for update (if that is supported by the underlying dbms)
	 * and checks if they have been modified by comparing the attribute values to the current values in the database.
	 * Note that this does not include BLOB columns or columns that are readOnly.
	 * The calling method is responsible for releasing the select for update lock.
	 * @param entitiesByEntityType the entities to check, mapped to entityType
	 * @throws SQLException in case of exception
	 * @throws RecordModifiedException in case an entity has been modified, if an entity has been deleted,
	 * the {@code modifiedRow} provided by the exception is null
	 */
	private void performOptimisticLocking(Map<EntityType, List<Entity>> entitiesByEntityType) throws SQLException, RecordModifiedException {
		for (Map.Entry<EntityType, List<Entity>> entitiesByEntityTypeEntry : entitiesByEntityType.entrySet()) {
			EntityDefinition definition = definition(entitiesByEntityTypeEntry.getKey());
			if (definition.optimisticLocking()) {
				checkIfMissingOrModified(entitiesByEntityTypeEntry.getKey(), entitiesByEntityTypeEntry.getValue());
			}
		}
	}

	private void checkIfMissingOrModified(EntityType entityType, List<Entity> entities) throws SQLException, RecordModifiedException {
		Collection<Key> originalKeys = originalPrimaryKeys(entities);
		Select selectForUpdate = where(keys(originalKeys))
						.attributes(primaryKeyAndWritableColumns(entityType))
						.forUpdate()
						.build();
		Map<Key, Entity> currentEntitiesByKey = primaryKeyMap(query(selectForUpdate));
		for (Entity entity : entities) {
			Entity current = currentEntitiesByKey.get(entity.originalPrimaryKey());
			if (current == null) {
				Entity original = entity.copy().mutable();
				original.revert();

				throw new RecordModifiedException(entity, null, MESSAGES.getString(RECORD_MODIFIED)
								+ ", " + original + " " + MESSAGES.getString("has_been_deleted"));
			}
			Collection<Column<?>> modifiedColumns = modifiedColumns(entity, current);
			if (!modifiedColumns.isEmpty()) {
				throw new RecordModifiedException(entity, current, createModifiedExceptionMessage(entity, current, modifiedColumns));
			}
		}
	}

	private String createUpdateQuery(Update update, List<ColumnDefinition<?>> statementColumns,
																	 List<Object> statementValues) throws UpdateException {
		EntityDefinition entityDefinition = definition(update.where().entityType());
		for (Map.Entry<Column<?>, Object> columnValue : update.values().entrySet()) {
			ColumnDefinition<Object> columnDefinition = entityDefinition.columns().definition((Column<Object>) columnValue.getKey());
			if (!columnDefinition.updatable()) {
				throw new UpdateException("Column is not updatable: " + columnDefinition.attribute());
			}
			statementColumns.add(columnDefinition);
			statementValues.add(columnDefinition.attribute().type().validateType(columnValue.getValue()));
		}
		String updateQuery = updateQuery(entityDefinition.tableName(), statementColumns, update.where().toString(entityDefinition));
		statementColumns.addAll(definitions(update.where().columns()));
		statementValues.addAll(update.where().values());

		return updateQuery;
	}

	private List<Entity> query(Select select) throws SQLException {
		List<Entity> result = cachedResult(select);
		if (result != null) {
			LOG.debug("Returning cached result: {}", select.where().entityType());
			return result;
		}

		return cacheResult(select, query(select, 0));
	}

	private List<Entity> query(Select select, int currentForeignKeyFetchDepth) throws SQLException {
		List<Entity> result;
		try (ResultIterator<Entity> iterator = resultIterator(select)) {
			result = packResult(iterator);
		}
		if (!result.isEmpty()) {
			populateForeignKeys(result, select, currentForeignKeyFetchDepth);
		}

		return result;
	}

	/**
	 * Selects the entities referenced by the given entities via foreign keys and sets those
	 * as their respective foreign key values. This is done recursively for the entities referenced
	 * by the foreign keys as well, until we reach the select fetch depth limit.
	 * @param entities the entities for which to set the foreign key entity values
	 * @param select the select
	 * @param currentForeignKeyFetchDepth the current foreign key fetch depth
	 * @throws SQLException in case of a database exception
	 * @see #setLimitForeignKeyFetchDepth(boolean)
	 * @see Select.Builder#fetchDepth(int)
	 */
	private void populateForeignKeys(List<Entity> entities, Select select,
																	 int currentForeignKeyFetchDepth) throws SQLException {
		Collection<ForeignKeyDefinition> foreignKeysToSet =
						foreignKeysToPopulate(entities.get(0).entityType(), select.attributes());
		for (ForeignKeyDefinition foreignKeyDefinition : foreignKeysToSet) {
			ForeignKey foreignKey = foreignKeyDefinition.attribute();
			int conditionOrForeignKeyFetchDepthLimit = select.fetchDepth(foreignKey)
							.orElse(foreignKeyDefinition.fetchDepth());
			if (withinFetchDepthLimit(currentForeignKeyFetchDepth, conditionOrForeignKeyFetchDepthLimit)
							&& containsReferencedColumns(entities.get(0), foreignKey.references())) {
				try {
					logEntry("populateForeignKeys", foreignKeyDefinition);
					Collection<Key> referencedKeys = Entity.keys(foreignKey, entities);
					if (referencedKeys.isEmpty()) {
						entities.forEach(entity -> entity.put(foreignKey, null));
					}
					else {
						Map<Key, Entity> referencedEntitiesMappedByKey = queryReferencedEntities(foreignKeyDefinition,
										new ArrayList<>(referencedKeys), currentForeignKeyFetchDepth, conditionOrForeignKeyFetchDepthLimit);
						entities.forEach(entity -> entity.put(foreignKey,
										entity(entity.key(foreignKey), referencedEntitiesMappedByKey)));
					}
				}
				finally {
					logExit("populateForeignKeys");
				}
			}
		}
	}

	private Collection<ForeignKeyDefinition> foreignKeysToPopulate(EntityType entityType,
																																 Collection<Attribute<?>> conditionAttributes) {
		Collection<ForeignKeyDefinition> foreignKeyDefinitions = definition(entityType).foreignKeys().definitions();
		if (conditionAttributes.isEmpty()) {
			return foreignKeyDefinitions;
		}

		Set<Attribute<?>> selectAttributes = new HashSet<>(conditionAttributes);

		return foreignKeyDefinitions.stream()
						.filter(foreignKeyDefinition -> selectAttributes.contains(foreignKeyDefinition.attribute()))
						.collect(toList());
	}

	private boolean withinFetchDepthLimit(int currentForeignKeyFetchDepth, int conditionFetchDepthLimit) {
		return !limitForeignKeyFetchDepth || conditionFetchDepthLimit == -1 || currentForeignKeyFetchDepth < conditionFetchDepthLimit;
	}

	private Map<Key, Entity> queryReferencedEntities(ForeignKeyDefinition foreignKeyDefinition, List<Key> referencedKeys,
																									 int currentForeignKeyFetchDepth, int conditionFetchDepthLimit) throws SQLException {
		Key referencedKey = referencedKeys.get(0);
		Collection<Column<?>> keyColumns = referencedKey.columns();
		List<Entity> referencedEntities = new ArrayList<>(referencedKeys.size());
		int keysPerStatement = keysPerStatement(referencedKeys.get(0));
		for (int i = 0; i < referencedKeys.size(); i += keysPerStatement) {
			List<Key> keys = referencedKeys.subList(i, Math.min(i + keysPerStatement, referencedKeys.size()));
			Select referencedEntitiesCondition = where(keys(keys))
							.fetchDepth(conditionFetchDepthLimit)
							.attributes(attributesToSelect(foreignKeyDefinition, keyColumns))
							.build();
			referencedEntities.addAll(query(referencedEntitiesCondition, currentForeignKeyFetchDepth + 1).stream()
							.map(IMMUTABLE)
							.collect(toList()));
		}

		if (referencedKey.primary()) {
			return primaryKeyMap(referencedEntities);
		}

		return referencedEntities.stream()
						.collect(toMap(entity -> createKey(entity, keyColumns), Function.identity()));
	}

	private int keysPerStatement(Key key) {
		if (database.maximumNumberOfParameters() == Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}

		return database.maximumNumberOfParameters() / key.columns().size();
	}

	private Key createKey(Entity entity, Collection<Column<?>> keyColumns) {
		Key.Builder keyBuilder = entities().keyBuilder(entity.entityType());
		keyColumns.forEach(column -> keyBuilder.with((Column<Object>) column, entity.get(column)));

		return keyBuilder.build();
	}

	private ResultIterator<Entity> resultIterator(Select select) throws SQLException {
		requireNonNull(select, SELECT_MAY_NOT_BE_NULL);
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		EntityDefinition entityDefinition = definition(select.where().entityType());
		SelectQueries.Builder queryBuilder = selectQueries.builder(entityDefinition)
						.select(select);
		String selectQuery = queryBuilder.build();
		List<Object> statementValues = statementValues(select.where(), select.having());
		List<ColumnDefinition<?>> statementColumns = statementColumns(select.where(), select.having());
		try {
			statement = prepareStatement(selectQuery, false, select.queryTimeout());
			resultSet = executeQuery(statement, selectQuery, statementColumns, statementValues);

			return new EntityResultIterator(statement, resultSet,
							new EntityResultPacker(entityDefinition, queryBuilder.selectedColumns()));
		}
		catch (SQLException e) {
			closeSilently(resultSet);
			closeSilently(statement);
			LOG.error(createLogMessage(selectQuery, statementValues, statementColumns, e), e);
			throw e;
		}
	}

	private int executeUpdate(PreparedStatement statement, String query,
														List<ColumnDefinition<?>> statementColumns, List<?> statementValues,
														Operation operation) throws SQLException {
		SQLException exception = null;
		logEntry(EXECUTE_UPDATE, statementValues);
		try {
			return setParameterValues(statement, statementColumns, statementValues).executeUpdate();
		}
		catch (SQLException e) {
			exception = e;
			throw e;
		}
		finally {
			logExit(EXECUTE_UPDATE, exception);
			countQuery(operation);
			if (LOG.isDebugEnabled()) {
				LOG.debug(createLogMessage(query, statementValues, statementColumns, exception));
			}
		}
	}

	private ResultSet executeQuery(PreparedStatement statement, String query,
																 List<ColumnDefinition<?>> statementColumns, List<?> statementValues) throws SQLException {
		SQLException exception = null;
		logEntry(EXECUTE_QUERY, statementValues);
		try {
			return setParameterValues(statement, statementColumns, statementValues).executeQuery();
		}
		catch (SQLException e) {
			exception = e;
			throw e;
		}
		finally {
			logExit(EXECUTE_QUERY, exception);
			countQuery(SELECT);
			if (LOG.isDebugEnabled()) {
				LOG.debug(createLogMessage(query, statementValues, statementColumns, exception));
			}
		}
	}

	private List<ColumnDefinition<?>> statementColumns(Condition where, Condition having) {
		List<ColumnDefinition<?>> whereColumns = definitions(where.columns());
		if (having == null || having instanceof Condition.All) {
			return whereColumns;
		}

		List<ColumnDefinition<?>> havingColumns = definitions(having.columns());
		List<ColumnDefinition<?>> statementColumns = new ArrayList<>(whereColumns.size() + havingColumns.size());
		statementColumns.addAll(whereColumns);
		statementColumns.addAll(havingColumns);

		return statementColumns;
	}

	private List<ColumnDefinition<?>> definitions(List<Column<?>> columns) {
		return columns.stream()
						.map(column -> entities().definition(column.entityType()).columns().definition(column))
						.collect(toList());
	}

	private static List<Object> statementValues(Condition where, Condition having) {
		List<Object> whereValues = (List<Object>) where.values();
		if (having == null || having instanceof Condition.All) {
			return whereValues;
		}

		List<?> havingValues = having.values();
		List<Object> statementValues = new ArrayList<>(whereValues.size() + havingValues.size());
		statementValues.addAll(where.values());
		statementValues.addAll(havingValues);

		return statementValues;
	}

	private PreparedStatement prepareStatement(String query) throws SQLException {
		return prepareStatement(query, false);
	}

	private PreparedStatement prepareStatement(String query, boolean returnGeneratedKeys) throws SQLException {
		return prepareStatement(query, returnGeneratedKeys, defaultQueryTimeout);
	}

	private PreparedStatement prepareStatement(String query, boolean returnGeneratedKeys,
																						 int queryTimeout) throws SQLException {
		try {
			logEntry("prepareStatement", query);
			PreparedStatement statement = returnGeneratedKeys ?
							connection.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS) :
							connection.getConnection().prepareStatement(query);
			statement.setQueryTimeout(queryTimeout);

			return statement;
		}
		finally {
			logExit("prepareStatement");
		}
	}

	/**
	 * @param entityType the entityType
	 * @return all hard (non-soft) foreign keys in the domain referencing entities of type {@code entityType}
	 */
	private Collection<ForeignKeyDefinition> hardForeignKeyReferences(EntityType entityType) {
		return hardForeignKeyReferenceCache.computeIfAbsent(entityType, this::initializeHardForeignKeyReferences);
	}

	private List<ForeignKeyDefinition> initializeHardForeignKeyReferences(EntityType entityType) {
		return domain.entities().definitions().stream()
						.flatMap(entityDefinition -> entityDefinition.foreignKeys().definitions().stream())
						.filter(foreignKeyDefinition -> !foreignKeyDefinition.soft())
						.filter(foreignKeyDefinition -> foreignKeyDefinition.attribute().referencedType().equals(entityType))
						.collect(toList());
	}

	private List<Entity> packResult(ResultIterator<Entity> iterator) throws SQLException {
		SQLException packingException = null;
		List<Entity> result = new ArrayList<>();
		try {
			logEntry(PACK_RESULT);
			while (iterator.hasNext()) {
				result.add(iterator.next());
			}

			return result;
		}
		catch (SQLException e) {
			packingException = e;
			throw e;
		}
		finally {
			logExit(PACK_RESULT, packingException, "row count: " + result.size());
		}
	}

	private <T> List<T> packResult(ColumnDefinition<T> columnDefinition, ResultSet resultSet) throws SQLException {
		SQLException packingException = null;
		List<T> result = emptyList();
		try {
			logEntry(PACK_RESULT);
			result = resultPacker(columnDefinition).pack(resultSet);

			return result;
		}
		catch (SQLException e) {
			packingException = e;
			throw e;
		}
		finally {
			logExit(PACK_RESULT, packingException, "row count: " + result.size());
		}
	}

	private List<ColumnDefinition<?>> insertableColumns(EntityDefinition entityDefinition,
																											boolean includePrimaryKeyColumns) {
		return insertableColumnsCache.computeIfAbsent(entityDefinition.entityType(), k ->
						writableColumnDefinitions(entityDefinition, includePrimaryKeyColumns, true));
	}

	private List<ColumnDefinition<?>> updatableColumns(EntityDefinition entityDefinition) {
		return updatableColumnsCache.computeIfAbsent(entityDefinition.entityType(), k ->
						writableColumnDefinitions(entityDefinition, true, false));
	}

	private List<Attribute<?>> primaryKeyAndWritableColumns(EntityType entityType) {
		return primaryKeyAndWritableColumnsCache.computeIfAbsent(entityType, k ->
						collectPrimaryKeyAndWritableColumns(entityType));
	}

	private List<Attribute<?>> collectPrimaryKeyAndWritableColumns(EntityType entityType) {
		EntityDefinition entityDefinition = definition(entityType);
		List<ColumnDefinition<?>> writableAndPrimaryKeyColumns =
						new ArrayList<>(writableColumnDefinitions(entityDefinition, true, true));
		entityDefinition.primaryKey().definitions().forEach(primaryKeyColumn -> {
			if (!writableAndPrimaryKeyColumns.contains(primaryKeyColumn)) {
				writableAndPrimaryKeyColumns.add(primaryKeyColumn);
			}
		});

		return writableAndPrimaryKeyColumns.stream()
						.map(ColumnDefinition::attribute)
						.collect(toList());
	}

	private void rollbackQuietly() {
		try {
			connection.rollback();
		}
		catch (SQLException e) {
			LOG.error("Exception while performing a quiet rollback", e);
		}
	}

	private void commitIfTransactionIsNotOpen() throws SQLException {
		if (!transactionOpen()) {
			connection.commit();
		}
	}

	private void rollbackQuietlyIfTransactionIsNotOpen() {
		if (!transactionOpen()) {
			rollbackQuietly();
		}
	}

	private void logEntry(String method) {
		MethodLogger methodLogger = connection.getMethodLogger();
		if (methodLogger != null && methodLogger.isEnabled()) {
			methodLogger.enter(method);
		}
	}

	private void logEntry(String method, Object argument) {
		MethodLogger methodLogger = connection.getMethodLogger();
		if (methodLogger != null && methodLogger.isEnabled()) {
			methodLogger.enter(method, argument);
		}
	}

	private void logEntry(String method, Object... arguments) {
		MethodLogger methodLogger = connection.getMethodLogger();
		if (methodLogger != null && methodLogger.isEnabled()) {
			methodLogger.enter(method, arguments);
		}
	}

	private void logExit(String method) {
		logExit(method, null);
	}

	private void logExit(String method, Exception exception) {
		logExit(method, exception, null);
	}

	private void logExit(String method, Exception exception, String exitMessage) {
		MethodLogger methodLogger = connection.getMethodLogger();
		if (methodLogger != null && methodLogger.isEnabled()) {
			methodLogger.exit(method, exception, exitMessage);
		}
	}

	private String createLogMessage(String sqlStatement, List<?> values, List<ColumnDefinition<?>> columnDefinitions, Exception exception) {
		StringBuilder logMessage = new StringBuilder(user().toString()).append("\n");
		String valueString = "[" + createValueString(values, columnDefinitions) + "]";
		logMessage.append(sqlStatement == null ? "no sql statement" : sqlStatement).append(", ").append(valueString);
		if (exception != null) {
			logMessage.append("\n").append(" [Exception: ").append(exception.getMessage()).append("]");
		}

		return logMessage.toString();
	}

	private void countQuery(Operation operation) {
		switch (operation) {
			case SELECT:
				database.queryCounter().select();
				break;
			case INSERT:
				database.queryCounter().insert();
				break;
			case UPDATE:
				database.queryCounter().update();
				break;
			case DELETE:
				database.queryCounter().delete();
				break;
			default:
				break;
		}
	}

	private void throwIfReadOnly(Collection<Entity> entities) {
		for (Entity entity : entities) {
			throwIfReadOnly(entity.entityType());
		}
	}

	private void throwIfReadOnly(Set<EntityType> entityTypes) {
		for (EntityType entityType : entityTypes) {
			throwIfReadOnly(entityType);
		}
	}

	private void throwIfReadOnly(EntityType entityType) {
		if (definition(entityType).readOnly()) {
			throw new DatabaseException("Entities of type: " + entityType + " are read only");
		}
	}

	private void throwDatabaseException(Exception exception, Operation operation) {
		if (exception instanceof SQLException) {
			throw database.exception((SQLException) exception, operation);
		}
	}

	private RuntimeException runtimeException(Exception exception) {
		if (exception instanceof RuntimeException) {
			return (RuntimeException) exception;
		}

		return new RuntimeException(exception);
	}

	private EntityDefinition definition(EntityType entityType) {
		return domain.entities().definition(entityType);
	}

	private List<Entity> cachedResult(Select select) {
		if (queryCacheEnabled && !select.forUpdate()) {
			return queryCache.get(select);
		}

		return null;
	}

	private List<Entity> cacheResult(Select select, List<Entity> result) {
		if (queryCacheEnabled && !select.forUpdate()) {
			LOG.debug("Caching result: {}", select.where().entityType());
			queryCache.put(select, result);
		}

		return result;
	}

	private static Entity entity(Key key, Map<Key, Entity> entityKeyMap) {
		if (key == null) {
			return null;
		}
		Entity entity = entityKeyMap.get(key);
		if (entity == null) {
			//if the referenced entity is not found (it's been deleted or has been filtered out of an underlying view for example),
			//we create an empty entity wrapping the key since that's the best we can do under the circumstances
			entity = Entity.entity(key).immutable();
		}

		return entity;
	}

	private static List<ColumnDefinition<?>> writableColumnDefinitions(
					EntityDefinition entityDefinition, boolean includePrimaryKeyColumns, boolean includeNonUpdatable) {
		return entityDefinition.columns().definitions().stream()
						.filter(column -> isWritable(column, includePrimaryKeyColumns, includeNonUpdatable))
						.collect(toList());
	}

	private static boolean isWritable(ColumnDefinition<?> column, boolean includePrimaryKeyColumns,
																		boolean includeNonUpdatable) {
		return column.insertable() && (includeNonUpdatable || column.updatable())
						&& (includePrimaryKeyColumns || !column.primaryKey());
	}

	private static <T> ResultPacker<T> resultPacker(ColumnDefinition<T> columnDefinition) {
		return resultSet -> columnDefinition.get(resultSet, 1);
	}

	private static PreparedStatement setParameterValues(PreparedStatement statement, List<ColumnDefinition<?>> statementColumns,
																											List<?> statementValues) throws SQLException {
		if (statementColumns.isEmpty()) {
			return statement;
		}
		if (statementColumns.size() != statementValues.size()) {
			throw new SQLException("Parameter column value count mismatch: " +
							"expected: " + statementValues.size() + ", got: " + statementColumns.size());
		}

		for (int i = 0; i < statementColumns.size(); i++) {
			setParameterValue(statement, (ColumnDefinition<Object>) statementColumns.get(i), statementValues.get(i), i + 1);
		}

		return statement;
	}

	private static void setParameterValue(PreparedStatement statement, ColumnDefinition<Object> columnDefinition,
																				Object value, int parameterIndex) throws SQLException {
		Object columnValue = columnValue(value, statement, columnDefinition.converter());
		try {
			if (columnValue == null) {
				statement.setNull(parameterIndex, columnDefinition.type());
			}
			else {
				statement.setObject(parameterIndex, columnValue, columnDefinition.type());
			}
		}
		catch (SQLException e) {
			LOG.error("Unable to set parameter: {}, value: {}, value class: {}", columnDefinition, value, value == null ? "null" : value.getClass(), e);
			throw e;
		}
	}

	private static Object columnValue(Object value, PreparedStatement statement,
																		Column.Converter<Object, Object> converter) throws SQLException {
		if (value != null || converter.handlesNull()) {
			return converter.toColumnValue(value, statement);
		}

		return null;
	}

	/**
	 * Returns all updatable {@link Column}s which original value differs from the one in the comparison entity,
	 * returns an empty Collection if all of {@code entity}s original values match the values found in {@code comparison}.
	 * Note that only populated values are included in this comparison.
	 * @param entity the entity instance to check
	 * @param comparison the entity instance to compare with
	 * @return the updatable columns which values differ from the ones in the comparison entity
	 * @see ColumnDefinition#lazy()
	 */
	static Collection<Column<?>> modifiedColumns(Entity entity, Entity comparison) {
		return entity.entrySet().stream()
						.map(entry -> entity.definition().attributes().definition(entry.getKey()))
						.filter(ColumnDefinition.class::isInstance)
						.map(attributeDefinition -> (ColumnDefinition<?>) attributeDefinition)
						.filter(columnDefinition -> columnDefinition.updatable()
										&& !columnDefinition.lazy()
										&& valueMissingOrModified(entity, comparison, columnDefinition.attribute()))
						.map(ColumnDefinition::attribute)
						.collect(toList());
	}

	/**
	 * @param entity the entity instance to check
	 * @param comparison the entity instance to compare with
	 * @param attribute the attribute to check
	 * @param <T> the attribute type
	 * @return true if the value is missing or the original value differs from the one in the comparison entity
	 */
	static <T> boolean valueMissingOrModified(Entity entity, Entity comparison, Attribute<T> attribute) {
		if (!comparison.contains(attribute)) {
			return true;
		}

		T originalValue = entity.original(attribute);
		T comparisonValue = comparison.get(attribute);
		if (attribute.type().isByteArray()) {
			return !Arrays.equals((byte[]) originalValue, (byte[]) comparisonValue);
		}

		return !Objects.equals(originalValue, comparisonValue);
	}

	/**
	 * Populates the given lists with applicable columns and values.
	 * @param entity the Entity instance
	 * @param columnDefinitions the columns the entity type is based on
	 * @param statementColumns the list to populate with the columns to use in the statement
	 * @param statementValues the list to populate with the values to be used in the statement
	 * @param includeIf the Predicate to apply when checking to see if the column should be included
	 */
	private static void populateColumnsAndValues(Entity entity,
																							 List<ColumnDefinition<?>> columnDefinitions,
																							 List<ColumnDefinition<?>> statementColumns,
																							 List<Object> statementValues,
																							 Predicate<ColumnDefinition<?>> includeIf) {
		for (int i = 0; i < columnDefinitions.size(); i++) {
			ColumnDefinition<?> columnDefinition = columnDefinitions.get(i);
			if (includeIf.test(columnDefinition)) {
				statementColumns.add(columnDefinition);
				statementValues.add(entity.get(columnDefinition.attribute()));
			}
		}
	}

	private static String createModifiedExceptionMessage(Entity entity, Entity modified,
																											 Collection<Column<?>> modifiedColumns) {
		StringBuilder builder = new StringBuilder(MESSAGES.getString(RECORD_MODIFIED)).append(": ").append(entity.entityType());
		for (Column<?> column : modifiedColumns) {
			builder.append("\n").append(column)
							.append(": ").append(entity.original(column))
							.append(" -> ").append(modified.get(column));
		}

		return builder.toString();
	}

	private static boolean containsReferencedColumns(Entity entity, List<Reference<?>> references) {
		return references.stream()
						.map(Reference::column)
						.allMatch(entity::contains);
	}

	private static Collection<Attribute<?>> attributesToSelect(ForeignKeyDefinition foreignKeyDefinition,
																														 Collection<? extends Attribute<?>> referencedAttributes) {
		if (foreignKeyDefinition.attributes().isEmpty()) {
			return emptyList();
		}

		Set<Attribute<?>> selectAttributes = new HashSet<>(foreignKeyDefinition.attributes());
		selectAttributes.addAll(referencedAttributes);

		return selectAttributes;
	}

	private static String createValueString(List<?> values, List<ColumnDefinition<?>> columnDefinitions) {
		if (columnDefinitions.isEmpty() || columnDefinitions.size() != values.size()) {
			return values.stream()
							.map(Objects::toString)
							.collect(Collectors.joining(", "));
		}
		List<String> stringValues = new ArrayList<>(values.size());
		for (int i = 0; i < values.size(); i++) {
			ColumnDefinition<Object> columnDefinition = (ColumnDefinition<Object>) columnDefinitions.get(i);
			Object value = values.get(i);
			Object columnValue;
			String stringValue;
			try {
				Column.Converter<Object, Object> converter = columnDefinition.converter();
				if (value == null && !converter.handlesNull()) {
					columnValue = null;
				}
				else {
					columnValue = converter.toColumnValue(value, null);
				}
				stringValue = String.valueOf(value);
			}
			catch (Exception e) {
				//fallback to the original value
				columnValue = value;
				stringValue = String.valueOf(value);
			}
			stringValues.add(columnValue == null ? "null" : addSingleQuotes(columnDefinition.type(), stringValue));
		}

		return String.join(", ", stringValues);
	}

	private static String addSingleQuotes(int columnType, String string) {
		switch (columnType) {
			case Types.VARCHAR:
			case Types.CHAR:
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				return "'" + string + "'";
			default:
				return string;
		}
	}

	private static void verifyColumnIsSelectable(ColumnDefinition<?> columnDefinition, EntityDefinition entityDefinition) {
		if (columnDefinition.aggregate()) {
			throw new UnsupportedOperationException("Selecting column values is not implemented for aggregate function values");
		}
		entityDefinition.selectQuery().ifPresent(selectQuery -> {
			if (selectQuery.columns() != null) {
				throw new UnsupportedOperationException("Selecting column values is not implemented for entities with custom column clauses");
			}
		});
	}

	private static void closeSilently(AutoCloseable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		}
		catch (Exception ignored) {/*ignored*/}
	}

	static Database configureDatabase(Database database, Domain domain) {
		new DatabaseConfiguration(requireNonNull(domain, "domain may not be null"), requireNonNull(database, "database may not be null")).configure();

		return database;
	}

	private static final class DatabaseConfiguration {

		private static final Set<DatabaseConfiguration> CONFIGURED_DATABASES = new HashSet<>();

		private final Domain domain;
		private final Database database;
		private final int hashCode;

		private DatabaseConfiguration(Domain domain, Database database) {
			this.domain = domain;
			this.database = database;
			this.hashCode = Objects.hash(domain.type(), database);
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof DatabaseConfiguration)) {
				return false;
			}

			DatabaseConfiguration that = (DatabaseConfiguration) object;

			return Objects.equals(domain.type(), that.domain.type()) && database == that.database;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		private void configure() {
			synchronized (CONFIGURED_DATABASES) {
				if (!CONFIGURED_DATABASES.contains(this)) {
					domain.configure(database);
					CONFIGURED_DATABASES.add(this);
				}
			}
		}
	}
}