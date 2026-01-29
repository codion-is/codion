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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.db.exception.DeleteEntityException;
import is.codion.framework.db.exception.EntityModifiedException;
import is.codion.framework.db.exception.EntityNotFoundException;
import is.codion.framework.db.exception.MultipleEntitiesFoundException;
import is.codion.framework.db.exception.UpdateEntityException;
import is.codion.framework.db.local.tracer.MethodTracer;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKey.Reference;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static is.codion.common.db.database.Database.Operation.*;
import static is.codion.common.db.exception.DatabaseException.SQL_STATE_NO_DATA;
import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
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

final class DefaultLocalEntityConnection implements LocalEntityConnection, MethodTracer.Traceable {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultLocalEntityConnection.class);

	private static final MessageBundle MESSAGES =
					messageBundle(LocalEntityConnection.class, getBundle(LocalEntityConnection.class.getName()));
	private static final Map<String, User> META_DATA_USERS = new ConcurrentHashMap<>();
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
	private final User user;
	private final Database database;
	private final SelectQueries selectQueries;
	private final Map<EntityType, Boolean> generatedKeysCache = new HashMap<>();
	private final Map<EntityType, List<ColumnDefinition<?>>> generatedColumnsCache = new HashMap<>();
	private final Map<EntityType, List<ColumnDefinition<?>>> insertableColumnsCache = new HashMap<>();
	private final Map<EntityType, List<ColumnDefinition<?>>> updatableColumnsCache = new HashMap<>();
	private final Map<EntityType, List<Column<?>>> lazyColumnsCache = new HashMap<>();
	private final Map<EntityType, List<ForeignKeyDefinition>> hardForeignKeyReferenceCache = new HashMap<>();
	private final Map<EntityType, List<Attribute<?>>> primaryKeyAndWritableSelectedColumnsCache = new HashMap<>();
	private final Map<Select, List<Entity>> queryCache = new HashMap<>();

	private MethodTracer tracer = MethodTracer.NO_OP;

	private boolean optimisticLocking = OPTIMISTIC_LOCKING.getOrThrow();
	private boolean limitReferenceDepth = LIMIT_REFERENCE_DEPTH.getOrThrow();
	private int iteratorBufferSize = ITERATOR_BUFFER_SIZE.getOrThrow();
	private int queryTimeout = QUERY_TIMEOUT.getOrThrow();
	private boolean cacheQueries = false;

	private @Nullable Connection connection;
	private boolean transactionOpen = false;

	/**
	 * Constructs a new LocalEntityConnection instance
	 * @param database the Database instance
	 * @param domain the domain model
	 * @param user the user used for connecting to the database
	 * @throws DatabaseException in case there is a problem connecting to the database
	 * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
	 */
	DefaultLocalEntityConnection(Database database, Domain domain, User user) {
		this(requireNonNull(domain), configureDatabase(requireNonNull(database), domain), database.createConnection(requireNonNull(user)), user);
	}

	/**
	 * Constructs a new LocalEntityConnection instance
	 * @param database the Database instance
	 * @param domain the domain model
	 * @param connection the Connection object to base this EntityConnection on, it is assumed to be in a valid state
	 */
	DefaultLocalEntityConnection(Database database, Domain domain, Connection connection) {
		this(requireNonNull(domain), configureDatabase(requireNonNull(database), domain), requireNonNull(connection), user(connection));
	}

	private DefaultLocalEntityConnection(Domain domain, Database database, Connection connection, User user) {
		this.domain = domain;
		this.database = database;
		this.connection = disableAutoCommit(connection);
		this.user = user;
		this.domain.configure(connection);
		this.selectQueries = new SelectQueries(database);
	}

	@Override
	public Entities entities() {
		return domain.entities();
	}

	@Override
	public User user() {
		return user;
	}

	@Override
	public boolean connected() {
		synchronized (database) {
			return connectionValid();
		}
	}

	@Override
	public void close() {
		synchronized (database) {
			closeConnection();
		}
	}

	@Override
	public void startTransaction() {
		synchronized (database) {
			tracer.enter("startTransaction");
			SQLException exception = null;
			try {
				if (transactionOpen) {
					throw new IllegalStateException("Transaction already open");
				}
				connection = verifyOpenConnection();
				transactionOpen = true;
			}
			catch (SQLException e) {
				exception = e;
				LOG.error("Exception when starting transaction", e);
				throw new DatabaseException(e);
			}
			finally {
				tracer.exit("startTransaction", exception);
			}
		}
	}

	@Override
	public boolean transactionOpen() {
		synchronized (database) {
			return transactionOpen;
		}
	}

	@Override
	public void rollbackTransaction() {
		synchronized (database) {
			tracer.enter("rollbackTransaction");
			SQLException exception = null;
			try {
				if (!transactionOpen) {
					throw new IllegalStateException("Transaction is not open");
				}
				verifyOpenConnection().rollback();
				transactionOpen = false;
			}
			catch (SQLException e) {
				exception = e;
				LOG.error("Exception during transaction rollback", e);
				throw new DatabaseException(e);
			}
			finally {
				tracer.exit("rollbackTransaction", exception);
			}
		}
	}

	@Override
	public void commitTransaction() {
		synchronized (database) {
			tracer.enter("commitTransaction");
			SQLException exception = null;
			try {
				if (!transactionOpen) {
					throw new IllegalStateException("Transaction is not open");
				}
				verifyOpenConnection().commit();
				transactionOpen = false;
			}
			catch (SQLException e) {
				exception = e;
				LOG.error("Exception during transaction commit", e);
				throw new DatabaseException(e);
			}
			finally {
				tracer.exit("commitTransaction", exception);
			}
		}
	}

	@Override
	public void cacheQueries(boolean cacheQueries) {
		synchronized (database) {
			this.cacheQueries = cacheQueries;
			if (!cacheQueries) {
				queryCache.clear();
			}
		}
	}

	@Override
	public boolean cacheQueries() {
		synchronized (database) {
			return cacheQueries;
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
		synchronized (database) {
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
		String deleteQuery = deleteQuery(entityDefinition.table(), condition.string(entityDefinition));
		synchronized (database) {
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
		Condition condition = null;
		String deleteQuery = null;
		synchronized (database) {
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
						deleteQuery = deleteQuery(entityDefinition.table(), condition.string(entityDefinition));
						try (PreparedStatement statement = prepareStatement(deleteQuery)) {
							deleteCount += executeUpdate(statement, deleteQuery, statementColumns, statementValues, DELETE);
						}
					}
				}
				if (keys.size() != deleteCount) {
					throw new DeleteEntityException(deleteCount + " rows deleted, expected " + keys.size());
				}
				commitIfTransactionIsNotOpen();
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();
				LOG.error(createLogMessage(deleteQuery, statementValues, statementColumns, exception), exception);
				throwDatabaseException(exception, DELETE);
				throw Exceptions.runtime(exception);
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
			throw new EntityNotFoundException(MESSAGES.getString("record_not_found"));
		}
		if (entities.size() > 1) {
			throw new MultipleEntitiesFoundException(MESSAGES.getString("multiple_records_found"));
		}

		return entities.get(0);
	}

	@Override
	public Collection<Entity> select(Collection<Key> keys) {
		if (requireNonNull(keys, "keys may not be null").isEmpty()) {
			return emptyList();
		}

		synchronized (database) {
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
				throw Exceptions.runtime(exception);
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
		synchronized (database) {
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
				throw Exceptions.runtime(exception);
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
		if (columnDefinition.aggregate()) {
			throw new UnsupportedOperationException("Selecting column values is not implemented for aggregate function values");
		}
		Condition combinedCondition = and(select.where(), column.isNotNull());
		String selectQuery = selectQueries.builder(entityDefinition)
						.select(select, false)
						.columns(columnDefinition.expression())
						.where(combinedCondition)
						.groupBy(columnDefinition.expression())
						.build();
		List<Object> statementValues = statementValues(combinedCondition, select.having());
		List<ColumnDefinition<?>> statementColumns = statementColumns(combinedCondition, select.having());
		synchronized (database) {
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
				throw Exceptions.runtime(exception);
			}
		}
	}

	@Override
	public int count(Count count) {
		EntityDefinition entityDefinition = definition(requireNonNull(count, "count may not be null").where().entityType());
		String selectQuery = selectQueries.builder(entityDefinition)
						.count(count)
						.build();
		List<Object> statementValues = statementValues(count.where(), count.having());
		List<ColumnDefinition<?>> statementColumns = statementColumns(count.where(), count.having());
		synchronized (database) {
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
				throw Exceptions.runtime(exception);
			}
		}
	}

	@Override
	public Map<EntityType, Collection<Entity>> dependencies(Collection<Entity> entities) {
		Set<EntityType> entityTypes = requireNonNull(entities, ENTITIES).stream()
						.map(Entity::type)
						.collect(toSet());
		if (entityTypes.isEmpty()) {
			return emptyMap();//no entities
		}
		if (entityTypes.size() > 1) {
			throw new IllegalArgumentException("All entities must be of the same type when selecting dependencies");
		}

		Map<EntityType, Collection<Entity>> dependencyMap = new HashMap<>();
		synchronized (database) {
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
				throw Exceptions.runtime(exception);
			}
		}

		return dependencyMap;
	}

	@Override
	public <C extends EntityConnection, T, R> @Nullable R execute(FunctionType<C, T, R> functionType) {
		return execute(functionType, null);
	}

	@Override
	public <C extends EntityConnection, T, R> @Nullable R execute(FunctionType<C, T, R> functionType, @Nullable T parameter) {
		requireNonNull(functionType, "functionType may not be null");
		Exception exception = null;
		tracer.enter(EXECUTE, functionType, parameter);
		try {
			synchronized (database) {
				return domain.function(functionType).execute((C) this, parameter);
			}
		}
		catch (Exception e) {
			exception = e;
			LOG.error(createLogMessage(functionType.name(), parameter instanceof List ? (List<?>) parameter : singletonList(parameter), emptyList(), e), e);
			throwDatabaseException(e, OTHER);
			throw Exceptions.runtime(e);
		}
		finally {
			tracer.exit(EXECUTE, exception);
		}
	}

	@Override
	public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType) {
		execute(procedureType, null);
	}

	@Override
	public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType, @Nullable T parameter) {
		requireNonNull(procedureType, "procedureType may not be null");
		Exception exception = null;
		tracer.enter(EXECUTE, procedureType, parameter);
		try {
			synchronized (database) {
				domain.procedure(procedureType).execute((C) this, parameter);
			}
		}
		catch (Exception e) {
			exception = e;
			rollbackQuietlyIfTransactionIsNotOpen();
			LOG.error(createLogMessage(procedureType.name(), parameter instanceof List ? (List<?>) parameter : singletonList(parameter), emptyList(), e), e);
			throwDatabaseException(e, OTHER);
			throw Exceptions.runtime(e);
		}
		finally {
			tracer.exit(EXECUTE, exception);
		}
	}

	@Override
	public <T, R, P> R report(ReportType<T, R, P> reportType, P parameter) {
		requireNonNull(reportType, "reportType may not be null");
		Exception exception = null;
		tracer.enter(REPORT, reportType, parameter);
		synchronized (database) {
			try {
				R result = domain.report(reportType).fill(verifyOpenConnection(), parameter);
				commitIfTransactionIsNotOpen();

				return result;
			}
			catch (Exception e) {
				exception = e;
				rollbackQuietlyIfTransactionIsNotOpen();
				LOG.error(createLogMessage(null, singletonList(reportType), emptyList(), e), e);
				throwDatabaseException(e, SELECT);
				throw Exceptions.runtime(e);
			}
			finally {
				tracer.exit(REPORT, exception);
			}
		}
	}

	@Override
	public EntityResultIterator iterator(Condition condition) {
		return iterator(where(condition).build());
	}

	@Override
	public EntityResultIterator iterator(Select select) {
		synchronized (database) {
			try {
				EntityResultIterator iterator = resultIterator(select);
				if (noForeignKeysToPopulate(select)) {
					return iterator;
				}

				return new BufferedEntityResultIterator(this, iterator, select, iteratorBufferSize);
			}
			catch (SQLException e) {
				throw database.exception(e, SELECT);
			}
		}
	}

	@Override
	public Connection connection() {
		try {
			return verifyOpenConnection();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	@Override
	public Database database() {
		return database;
	}

	@Override
	public boolean optimisticLocking() {
		synchronized (database) {
			return optimisticLocking;
		}
	}

	@Override
	public void optimisticLocking(boolean optimisticLocking) {
		synchronized (database) {
			this.optimisticLocking = optimisticLocking;
		}
	}

	@Override
	public int iteratorBufferSize() {
		synchronized (database) {
			return iteratorBufferSize;
		}
	}

	@Override
	public void iteratorBufferSize(int iteratorBufferSize) {
		synchronized (database) {
			this.iteratorBufferSize = iteratorBufferSize;
		}
	}

	@Override
	public boolean limitReferenceDepth() {
		synchronized (database) {
			return limitReferenceDepth;
		}
	}

	@Override
	public void limitReferenceDepth(boolean limitReferenceDepth) {
		synchronized (database) {
			this.limitReferenceDepth = limitReferenceDepth;
		}
	}

	@Override
	public int queryTimeout() {
		synchronized (database) {
			return queryTimeout;
		}
	}

	@Override
	public void queryTimeout(int queryTimeout) {
		if (queryTimeout < 0) {
			throw new IllegalArgumentException("queryTimeout must be >= 0");
		}
		synchronized (database) {
			this.queryTimeout = queryTimeout;
		}
	}

	@Override
	public void tracer(MethodTracer tracer) {
		requireNonNull(tracer);
		synchronized (database) {
			this.tracer = tracer;
		}
	}

	@Override
	public void setConnection(@Nullable Connection connection) {
		synchronized (database) {
			this.connection = connection;
		}
	}

	@Override
	public @Nullable Connection getConnection() {
		synchronized (database) {
			return connection;
		}
	}

	private Collection<Key> insert(Collection<Entity> entities, @Nullable Collection<Entity> insertedEntities) {
		throwIfReadOnly(entities);

		List<Key> insertedKeys = new ArrayList<>(entities.size());
		List<Object> statementValues = new ArrayList<>();
		List<ColumnDefinition<?>> statementColumns = new ArrayList<>();
		Map<EntityType, Set<Column<?>>> lazyColumns = new HashMap<>();
		String insertQuery = null;
		synchronized (database) {
			try {
				for (Entity entity : entities) {
					EntityDefinition entityDefinition = definition(entity.type());
					List<ColumnDefinition<?>> generatedColumns = generatedColumns(entityDefinition);
					for (ColumnDefinition<?> column : generatedColumns) {
						generateBeforeInsert(entity, column);
					}
					populateColumnsAndValues(entity, insertableColumns(entityDefinition),
									statementColumns, statementValues, column -> entity.contains(column.attribute()));
					lazyColumns.computeIfAbsent(entity.type(), k -> new HashSet<>()).addAll(lazyColumns(entity));
					if (generatedColumns.isEmpty() && statementColumns.isEmpty()) {
						throw new SQLException("Unable to insert entity " + entity.type() + ", no values to insert");
					}
					insertQuery = insertQuery(entityDefinition.table(), statementColumns);
					try (PreparedStatement statement = prepareStatement(insertQuery, generatedKeys(entityDefinition, generatedColumns))) {
						executeUpdate(statement, insertQuery, statementColumns, statementValues, INSERT);
						for (ColumnDefinition<?> column : generatedColumns) {
							generateAfterInsert(entity, column, statement);
						}
					}

					insertedKeys.add(entity.primaryKey());
					statementColumns.clear();
					statementValues.clear();
				}
				if (insertedEntities != null) {
					for (Map.Entry<EntityType, List<Key>> entry : groupByType(insertedKeys).entrySet()) {
						insertedEntities.addAll(query(where(keys(entry.getValue()))
										.include(lazyColumns.get(entry.getKey()))
										.build(), 0));
					}
				}
				commitIfTransactionIsNotOpen();

				return insertedKeys;
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();
				LOG.error(createLogMessage(insertQuery, statementValues, statementColumns, exception), exception);
				throwDatabaseException(exception, INSERT);
				throw Exceptions.runtime(exception);
			}
		}
	}

	private <T> void generateBeforeInsert(Entity entity, ColumnDefinition<T> column) throws SQLException {
		column.generator().beforeInsert(entity, column.attribute(), database, connection);
	}

	private <T> void generateAfterInsert(Entity entity, ColumnDefinition<T> column, PreparedStatement statement) throws SQLException {
		column.generator().afterInsert(entity, column.attribute(), database, statement);
	}

	private boolean generatedKeys(EntityDefinition entityDefinition, List<ColumnDefinition<?>> generated) {
		return generatedKeysCache.computeIfAbsent(entityDefinition.type(), k ->
						generated.stream().anyMatch(column -> column.generator().generatedKeys()));
	}

	private void update(Collection<Entity> entities, @Nullable Collection<Entity> updatedEntities) {
		Map<EntityType, List<Entity>> entitiesByEntityType = groupByType(entities);
		throwIfReadOnly(entitiesByEntityType.keySet());

		List<Object> statementValues = new ArrayList<>();
		List<ColumnDefinition<?>> statementColumns = new ArrayList<>();
		String updateQuery = null;
		synchronized (database) {
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
							throw new UpdateEntityException("Unable to update entity " + entity.type() + ", no modified values found");
						}

						Condition condition = key(entity.originalPrimaryKey());
						updateQuery = updateQuery(entityDefinition.table(), statementColumns, condition.string(entityDefinition));
						try (PreparedStatement statement = prepareStatement(updateQuery)) {
							statementColumns.addAll(definitions(condition.columns()));
							statementValues.addAll(condition.values());
							int updatedRows = executeUpdate(statement, updateQuery, statementColumns, statementValues, UPDATE);
							if (updatedRows == 0) {
								throw new UpdateEntityException("Update did not affect any rows, entityType: " + entityTypeEntities.getKey());
							}
						}

						statementColumns.clear();
						statementValues.clear();
					}
					if (updatedEntities != null) {
						List<Entity> selected = query(where(keys(primaryKeys(entitiesToUpdate)))
										.include(lazyColumns(entitiesToUpdate, entityDefinition))
										.build(), 0);//bypass caching
						if (selected.size() != entitiesToUpdate.size()) {
							throw new UpdateEntityException(entitiesToUpdate.size() + " updated rows expected, query returned " +
											selected.size() + ", entityType: " + entityTypeEntities.getKey());
						}
						updatedEntities.addAll(selected);
					}
				}
				commitIfTransactionIsNotOpen();
			}
			catch (Exception exception) {
				rollbackQuietlyIfTransactionIsNotOpen();//releases the select for update lock
				if (exception instanceof EntityModifiedException) {
					LOG.debug(exception.getMessage(), exception);
					throw (EntityModifiedException) exception;
				}
				LOG.error(createLogMessage(updateQuery, statementValues, statementColumns, exception), exception);
				throwDatabaseException(exception, UPDATE);
				throw Exceptions.runtime(exception);
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
	 * @throws EntityModifiedException in case an entity has been modified or deleted,
	 * the {@code modifiedRow} provided by the exception is null
	 */
	private void performOptimisticLocking(Map<EntityType, List<Entity>> entitiesByEntityType) throws SQLException, EntityModifiedException {
		for (Map.Entry<EntityType, List<Entity>> entitiesByEntityTypeEntry : entitiesByEntityType.entrySet()) {
			EntityDefinition definition = definition(entitiesByEntityTypeEntry.getKey());
			if (definition.optimisticLocking()) {
				checkIfMissingOrModified(entitiesByEntityTypeEntry.getKey(), entitiesByEntityTypeEntry.getValue());
			}
		}
	}

	private void checkIfMissingOrModified(EntityType entityType, List<Entity> entities) throws SQLException, EntityModifiedException {
		Collection<Key> originalKeys = originalPrimaryKeys(entities);
		Select selectForUpdate = where(keys(originalKeys))
						.attributes(primaryKeyAndWritableSelectedColumns(entityType))
						.include(lazyColumns(entities, entities().definition(entityType)))
						.forUpdate()
						.build();
		Map<Key, Entity> currentEntitiesByKey = primaryKeyMap(query(selectForUpdate));
		for (Entity entity : entities) {
			Entity current = currentEntitiesByKey.get(entity.originalPrimaryKey());
			if (current == null) {
				Entity original = entity.copy().mutable();
				original.revert();

				throw new EntityModifiedException(entity, null, emptyList(), MESSAGES.getString(RECORD_MODIFIED)
								+ ", " + original + " " + MESSAGES.getString("has_been_deleted"));
			}
			Collection<Column<?>> modifiedColumns = modifiedColumns(entity, current);
			if (!modifiedColumns.isEmpty()) {
				throw new EntityModifiedException(entity, current, modifiedColumns, createModifiedExceptionMessage(entity, current, modifiedColumns));
			}
		}
	}

	private String createUpdateQuery(Update update, List<ColumnDefinition<?>> statementColumns,
																	 List<@Nullable Object> statementValues) throws UpdateEntityException {
		EntityDefinition entityDefinition = definition(update.where().entityType());
		for (Map.Entry<Column<?>, Object> columnValue : update.values().entrySet()) {
			ColumnDefinition<Object> columnDefinition = entityDefinition.columns().definition((Column<Object>) columnValue.getKey());
			if (!columnDefinition.updatable()) {
				throw new UpdateEntityException("Column is not updatable: " + columnDefinition.attribute());
			}
			statementColumns.add(columnDefinition);
			statementValues.add(columnDefinition.attribute().type().validateType(columnValue.getValue()));
		}
		String updateQuery = updateQuery(entityDefinition.table(), statementColumns, update.where().string(entityDefinition));
		statementColumns.addAll(definitions(update.where().columns()));
		statementValues.addAll(update.where().values());

		return updateQuery;
	}

	private List<Entity> query(Select select) throws SQLException {
		List<Entity> result = cachedResult(select);
		if (result != null) {
			LOG.debug("Returning cached result for select {}", select);
			return result;
		}

		return cacheResult(select, query(select, 0));
	}

	private List<Entity> query(Select select, int referenceDepth) throws SQLException {
		List<Entity> result;
		try (EntityResultIterator iterator = resultIterator(select)) {
			result = packResult(iterator);
		}
		if (!result.isEmpty()) {
			populateForeignKeys(result, select, referenceDepth);
		}

		return result;
	}

	/**
	 * Selects the entities referenced by the given entities via foreign keys and sets those
	 * as their respective foreign key values. This is done recursively for the entities referenced
	 * by the foreign keys as well, until we reach the select reference depth limit.
	 * @param entities the entities for which to set the foreign key entity values
	 * @param select the select
	 * @param referenceDepth the current foreign key reference depth
	 * @throws SQLException in case of a database exception
	 * @see #limitReferenceDepth(boolean)
	 * @see Select.Builder#referenceDepth(int)
	 */
	void populateForeignKeys(List<Entity> entities, Select select, int referenceDepth) throws SQLException {
		Collection<ForeignKeyDefinition> foreignKeysToSet =
						foreignKeysToPopulate(select, definition(entities.get(0).type()));
		for (ForeignKeyDefinition foreignKeyDefinition : foreignKeysToSet) {
			ForeignKey foreignKey = foreignKeyDefinition.attribute();
			int referenceDepthLimit = select.referenceDepth(foreignKey)
							.orElse(foreignKeyDefinition.referenceDepth());
			if (withinReferenceDepthLimit(referenceDepth, referenceDepthLimit)
							&& containsReferencedColumns(entities.get(0), foreignKey.references())) {
				try {
					tracer.enter("populateForeignKeys", foreignKeyDefinition);
					Collection<Key> referencedKeys = Entity.keys(foreignKey, entities);
					if (referencedKeys.isEmpty()) {
						entities.forEach(entity -> entity.set(foreignKey, null));
					}
					else {
						Map<Key, Entity> referencedEntitiesMappedByKey = queryReferencedEntities(foreignKeyDefinition,
										new ArrayList<>(referencedKeys), referenceDepth, referenceDepthLimit);
						entities.forEach(entity -> entity.set(foreignKey,
										entity(entity.key(foreignKey), referencedEntitiesMappedByKey)));
					}
				}
				finally {
					tracer.exit("populateForeignKeys");
				}
			}
		}
	}

	private boolean noForeignKeysToPopulate(Select select) {
		EntityDefinition entityDefinition = domain.entities().definition(select.where().entityType());
		Set<Attribute<?>> attributes = new HashSet<>(SelectQueries.attributes(select, entityDefinition));
		Set<ForeignKeyDefinition> foreignKeys = new HashSet<>(foreignKeysToPopulate(select, entityDefinition));
		foreignKeys.removeIf(foreignKeyDefinition ->
						referenceDepthZero(foreignKeyDefinition, select) ||
										missingReferenceColumn(foreignKeyDefinition, attributes));

		return foreignKeys.isEmpty();
	}

	private boolean withinReferenceDepthLimit(int currentReferenceDepth, int referenceDepthLimit) {
		return !limitReferenceDepth || referenceDepthLimit == -1 || currentReferenceDepth < referenceDepthLimit;
	}

	private Map<Key, Entity> queryReferencedEntities(ForeignKeyDefinition foreignKeyDefinition, List<Key> referencedKeys,
																									 int referenceDepth, int referenceDepthLimit) throws SQLException {
		Key referencedKey = referencedKeys.get(0);
		Collection<Column<?>> keyColumns = referencedKey.columns();
		List<Entity> referencedEntities = new ArrayList<>(referencedKeys.size());
		int keysPerStatement = keysPerStatement(referencedKeys.get(0));
		for (int i = 0; i < referencedKeys.size(); i += keysPerStatement) {
			List<Key> keys = referencedKeys.subList(i, Math.min(i + keysPerStatement, referencedKeys.size()));
			Select referencedEntitiesCondition = where(keys(keys))
							.referenceDepth(referenceDepthLimit)
							.attributes(attributesToSelect(foreignKeyDefinition, keyColumns))
							.build();
			referencedEntities.addAll(query(referencedEntitiesCondition, referenceDepth + 1).stream()
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
		if (database.maximumParameters() == Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}

		return database.maximumParameters() / key.columns().size();
	}

	private Key createKey(Entity entity, Collection<Column<?>> keyColumns) {
		Key.Builder keyBuilder = entities().key(entity.type());
		keyColumns.forEach(column -> keyBuilder.with((Column<Object>) column, entity.get(column)));

		return keyBuilder.build();
	}

	private EntityResultIterator resultIterator(Select select) throws SQLException {
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
			statement = prepareStatement(selectQuery, false, select.timeout());
			resultSet = executeQuery(statement, selectQuery, statementColumns, statementValues);

			return new DefaultEntityResultIterator(statement, resultSet,
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
		int affectedRows = 0;
		tracer.enter(EXECUTE_UPDATE, statementValues);
		try {
			affectedRows = setParameterValues(statement, statementColumns, statementValues).executeUpdate();

			return affectedRows;
		}
		catch (SQLException e) {
			exception = e;
			throw e;
		}
		finally {
			tracer.exit(EXECUTE_UPDATE, exception, "affected rows: " + affectedRows);
			countQuery(operation);
			if (LOG.isDebugEnabled()) {
				LOG.debug(createLogMessage(query, statementValues, statementColumns, exception));
			}
		}
	}

	private ResultSet executeQuery(PreparedStatement statement, String query,
																 List<ColumnDefinition<?>> statementColumns, List<?> statementValues) throws SQLException {
		SQLException exception = null;
		tracer.enter(EXECUTE_QUERY, statementValues);
		try {
			return setParameterValues(statement, statementColumns, statementValues).executeQuery();
		}
		catch (SQLException e) {
			exception = e;
			throw e;
		}
		finally {
			tracer.exit(EXECUTE_QUERY, exception);
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

	private PreparedStatement prepareStatement(String query, boolean generatedKeys) throws SQLException {
		return prepareStatement(query, generatedKeys, queryTimeout);
	}

	private PreparedStatement prepareStatement(String query, boolean generatedKeys, int queryTimeout) throws SQLException {
		tracer.enter("prepareStatement", query);
		try {
			PreparedStatement statement = generatedKeys ?
							verifyOpenConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS) :
							verifyOpenConnection().prepareStatement(query);
			statement.setQueryTimeout(queryTimeout);

			return statement;
		}
		finally {
			tracer.exit("prepareStatement");
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

	private List<Entity> packResult(EntityResultIterator iterator) {
		Exception packingException = null;
		List<Entity> result = new ArrayList<>();
		tracer.enter(PACK_RESULT);
		try {
			while (iterator.hasNext()) {
				result.add(iterator.next());
			}

			return result;
		}
		catch (Exception e) {
			packingException = e;
			throw e;
		}
		finally {
			tracer.exit(PACK_RESULT, packingException, "row count: " + result.size());
		}
	}

	private <T> List<T> packResult(ColumnDefinition<T> columnDefinition, ResultSet resultSet) throws SQLException {
		SQLException packingException = null;
		List<@Nullable T> result = new ArrayList<>();
		tracer.enter(PACK_RESULT);
		try {
			while (resultSet.next()) {
				result.add(columnDefinition.get(resultSet, 1));
			}

			return result;
		}
		catch (SQLException e) {
			packingException = e;
			throw e;
		}
		finally {
			tracer.exit(PACK_RESULT, packingException, "row count: " + result.size());
		}
	}

	private List<ColumnDefinition<?>> generatedColumns(EntityDefinition entityDefinition) {
		return generatedColumnsCache.computeIfAbsent(entityDefinition.type(), k ->
						entityDefinition.columns().definitions().stream()
										.filter(ColumnDefinition::generated)
										.collect(toList()));
	}

	private List<ColumnDefinition<?>> insertableColumns(EntityDefinition entityDefinition) {
		return insertableColumnsCache.computeIfAbsent(entityDefinition.type(), k ->
						writableColumnDefinitions(entityDefinition, true, true));
	}

	private List<ColumnDefinition<?>> updatableColumns(EntityDefinition entityDefinition) {
		return updatableColumnsCache.computeIfAbsent(entityDefinition.type(), k ->
						writableColumnDefinitions(entityDefinition, true, false));
	}

	private List<Attribute<?>> primaryKeyAndWritableSelectedColumns(EntityType entityType) {
		return primaryKeyAndWritableSelectedColumnsCache.computeIfAbsent(entityType, k ->
						collectPrimaryKeyAndWritableSelectedColumns(entityType));
	}

	private List<Attribute<?>> collectPrimaryKeyAndWritableSelectedColumns(EntityType entityType) {
		EntityDefinition entityDefinition = definition(entityType);
		List<ColumnDefinition<?>> writableAndPrimaryKeyColumns =
						new ArrayList<>(writableColumnDefinitions(entityDefinition, true, true));
		entityDefinition.primaryKey().definitions().forEach(primaryKeyColumn -> {
			if (!writableAndPrimaryKeyColumns.contains(primaryKeyColumn)) {
				writableAndPrimaryKeyColumns.add(primaryKeyColumn);
			}
		});

		return writableAndPrimaryKeyColumns.stream()
						.filter(ColumnDefinition::selected)
						.map(ColumnDefinition::attribute)
						.collect(toList());
	}

	private void rollbackQuietly() {
		tracer.enter("rollback");
		SQLException exception = null;
		try {
			connection.rollback();
		}
		catch (SQLException e) {
			exception = e;
			LOG.warn("Exception while performing a quiet rollback", e);
		}
		finally {
			tracer.exit("rollback", exception);
		}
	}

	private void commitIfTransactionIsNotOpen() throws SQLException {
		if (!transactionOpen()) {
			tracer.enter("commit");
			SQLException exception = null;
			try {
				connection.commit();
			}
			catch (SQLException e) {
				exception = e;
				throw e;
			}
			finally {
				tracer.exit("commit", exception);
			}
		}
	}

	private void rollbackQuietlyIfTransactionIsNotOpen() {
		if (connection != null && !transactionOpen()) {
			rollbackQuietly();
		}
	}

	private String createLogMessage(@Nullable String sqlStatement, List<?> values, List<ColumnDefinition<?>> columnDefinitions, @Nullable Exception exception) {
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

	private Connection verifyOpenConnection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			throw new SQLException("Connection is closed");
		}

		return connection;
	}

	private boolean connectionValid() {
		return connection != null && database.connectionValid(connection);
	}

	private void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.rollback();
			}
		}
		catch (SQLException ex) {
			LOG.warn("Failed to rollback during connection close, connection may be invalid: {}", ex.getMessage());
		}
		try {
			if (connection != null) {
				connection.close();
				connection = null;
			}
		}
		catch (Exception ignored) {/*ignored*/}
		transactionOpen = false;
	}

	/**
	 * Returns a User with the username from the meta-data retrieved from the given connection
	 * @param connection the connection
	 * @return a user based on the information gleamed from the given connection
	 * @throws DatabaseException in case of an exception while retrieving the username from the connection meta-data
	 * @see java.sql.DatabaseMetaData#getUserName()
	 */
	private static User user(Connection connection) {
		validate(connection);
		try {
			return META_DATA_USERS.computeIfAbsent(connection.getMetaData().getUserName(), User::user);
		}
		catch (SQLException e) {
			throw new DatabaseException(e, "Failed to retrieve database username from connection metadata. Connection may be invalid or database may not support getUserName()");
		}
	}

	private static void validate(Connection connection) {
		try {
			if (connection.isClosed()) {
				throw new DatabaseException("Connection is closed");
			}
		}
		catch (SQLException exception) {
			throw new DatabaseException(exception, "Connection invalid");
		}
	}

	/**
	 * Disables auto-commit on the given connection and returns it.
	 * @param connection the connection
	 * @return the connection with auto-commit disabled
	 * @throws DatabaseException in case disabling auto-commit fails
	 */
	private static Connection disableAutoCommit(Connection connection) {
		requireNonNull(connection);
		try {
			connection.setAutoCommit(false);

			return connection;
		}
		catch (SQLException e) {
			LOG.error("Unable to disable auto commit on connection, assuming invalid state", e);
			throw new DatabaseException(e, "Failed to configure database connection: Unable to disable auto-commit");
		}
	}

	private Set<Column<?>> lazyColumns(Entity entity) {
		return lazyColumns(singletonList(entity), entity.definition());
	}

	private Set<Column<?>> lazyColumns(List<Entity> entities, EntityDefinition entityDefinition) {
		List<Column<?>> lazyColumns = lazyColumnsCache.computeIfAbsent(entityDefinition.type(), k ->
						entityDefinition.columns().definitions().stream()
										.filter(definition -> !definition.selected())
										.map(ColumnDefinition::attribute)
										.collect(toList()));
		if (lazyColumns.isEmpty()) {
			return emptySet();
		}
		Set<Column<?>> included = new HashSet<>();
		lazyColumns = new ArrayList<>(lazyColumns);
		for (Entity entity : entities) {
			if (lazyColumns.isEmpty()) {
				break;
			}
			for (Column<?> column : lazyColumns) {
				if (entity.contains(column)) {
					included.add(column);
					lazyColumns.remove(column);
					break;
				}
			}
		}

		return included;
	}

	private void throwIfReadOnly(Collection<Entity> entities) {
		for (Entity entity : entities) {
			throwIfReadOnly(entity.type());
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

	private EntityDefinition definition(EntityType entityType) {
		return domain.entities().definition(entityType);
	}

	private @Nullable List<Entity> cachedResult(Select select) {
		if (cacheQueries && !select.forUpdate()) {
			return queryCache.get(select);
		}

		return null;
	}

	private List<Entity> cacheResult(Select select, List<Entity> result) {
		if (cacheQueries && !select.forUpdate()) {
			LOG.debug("Caching result for select {}, size {}", select, result.size());
			queryCache.put(select, result);
		}

		return result;
	}

	private static @Nullable Entity entity(@Nullable Key key, Map<Key, Entity> entityKeyMap) {
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

	private static <T> void setParameterValue(PreparedStatement statement, ColumnDefinition<T> columnDefinition,
																						T value, int parameterIndex) throws SQLException {
		try {
			columnDefinition.set(statement, parameterIndex, value);
		}
		catch (SQLException e) {
			LOG.error("Unable to set parameter: {}, value: {}, value class: {}", columnDefinition, value, value == null ? "null" : value.getClass(), e);
			throw e;
		}
	}

	/**
	 * Returns all updatable {@link Column}s which original value differs from the one in the comparison entity,
	 * returns an empty Collection if all of {@code entity}s original values match the values found in {@code comparison}.
	 * Note that for non-selected columns, only values populated in both entities are included in this comparison.
	 * @param entity the entity instance to check
	 * @param comparison the entity instance to compare with
	 * @return the updatable columns which values differ from the ones in the comparison entity
	 * @see ColumnDefinition#selected()
	 */
	static Collection<Column<?>> modifiedColumns(Entity entity, Entity comparison) {
		return entity.entrySet().stream()
						.map(entry -> entity.definition().attributes().definition(entry.getKey()))
						.filter(ColumnDefinition.class::isInstance)
						.map(attributeDefinition -> (ColumnDefinition<?>) attributeDefinition)
						.filter(columnDefinition -> columnDefinition.updatable()
										&& (columnDefinition.selected() || (entity.contains(columnDefinition.attribute()) && comparison.contains(columnDefinition.attribute())))
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
																							 List<@Nullable Object> statementValues,
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
		StringBuilder builder = new StringBuilder(MESSAGES.getString(RECORD_MODIFIED)).append(": ").append(entity.type());
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

	private static boolean referenceDepthZero(ForeignKeyDefinition foreignKeyDefinition, Select select) {
		return select.referenceDepth(foreignKeyDefinition.attribute()).orElse(foreignKeyDefinition.referenceDepth()) == 0;
	}

	private static boolean missingReferenceColumn(ForeignKeyDefinition foreignKeyDefinition, Set<Attribute<?>> attributes) {
		return !foreignKeyDefinition.attribute().references().stream()
						.map(Reference::column)
						.allMatch(attributes::contains);
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

	private static Collection<ForeignKeyDefinition> foreignKeysToPopulate(Select select, EntityDefinition entityDefinition) {
		Collection<ForeignKeyDefinition> foreignKeyDefinitions = entityDefinition.foreignKeys().definitions();
		if (select.attributes().isEmpty()) {
			return foreignKeyDefinitions;
		}

		Set<Attribute<?>> selectAttributes = new HashSet<>(select.attributes());

		return foreignKeyDefinitions.stream()
						.filter(foreignKeyDefinition -> selectAttributes.contains(foreignKeyDefinition.attribute()))
						.collect(toList());
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
				if (value != null || converter.handlesNull()) {
					columnValue = converter.fromColumn(value);
				}
				else {
					columnValue = null;
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

	private static void closeSilently(@Nullable AutoCloseable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		}
		catch (Exception ignored) {/*ignored*/}
	}

	static Database configureDatabase(Database database, Domain domain) {
		new DatabaseConfiguration(domain, database).configure();

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