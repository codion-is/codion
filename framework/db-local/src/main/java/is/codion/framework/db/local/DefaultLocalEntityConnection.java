/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.DeleteException;
import is.codion.common.db.exception.MultipleRecordsFoundException;
import is.codion.common.db.exception.QueryTimeoutException;
import is.codion.common.db.exception.RecordModifiedException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.exception.UniqueConstraintException;
import is.codion.common.db.exception.UpdateException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.db.result.ResultPacker;
import is.codion.common.logging.MethodLogger;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
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
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.query.SelectQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static is.codion.common.db.database.Database.closeSilently;
import static is.codion.framework.db.condition.Condition.*;
import static is.codion.framework.db.local.Queries.*;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

/**
 * A default LocalEntityConnection implementation
 */
final class DefaultLocalEntityConnection implements LocalEntityConnection {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLocalEntityConnection.class);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalEntityConnection.class.getName());
  private static final String EXECUTE_STATEMENT = "executeStatement";
  private static final String RECORD_MODIFIED = "record_modified";
  private static final String CONDITION = "condition";
  private static final String ENTITIES = "entities";

  private static final ResultPacker<byte[]> BLOB_RESULT_PACKER = resultSet -> resultSet.getBytes(1);
  private static final ResultPacker<Integer> INTEGER_RESULT_PACKER = resultSet -> resultSet.getInt(1);

  private final Domain domain;
  private final Entities domainEntities;
  private final DatabaseConnection connection;
  private final SelectQueries selectQueries;
  private final Map<EntityType, List<ColumnDefinition<?>>> insertableColumnsCache = new HashMap<>();
  private final Map<EntityType, List<ColumnDefinition<?>>> updatableColumnsCache = new HashMap<>();
  private final Map<EntityType, List<ForeignKeyDefinition>> nonSoftForeignKeyReferenceCache = new HashMap<>();
  private final Map<EntityType, List<Attribute<?>>> primaryKeyAndWritableColumnsCache = new HashMap<>();
  private final Map<Select, List<Entity>> queryCache = new HashMap<>();

  private boolean optimisticLockingEnabled = LocalEntityConnection.OPTIMISTIC_LOCKING_ENABLED.get();
  private boolean limitForeignKeyFetchDepth = LocalEntityConnection.LIMIT_FOREIGN_KEY_FETCH_DEPTH.get();
  private int defaultQueryTimeout = LocalEntityConnection.QUERY_TIMEOUT_SECONDS.get();
  private boolean queryCacheEnabled = false;

  /**
   * Constructs a new LocalEntityConnection instance
   * @param database the Database instance
   * @param domain the domain model
   * @param user the user used for connecting to the database
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
   */
  DefaultLocalEntityConnection(Database database, Domain domain, User user) throws DatabaseException {
    this(domain, DatabaseConnection.databaseConnection(configureDatabase(database, domain), user));
  }

  /**
   * Constructs a new LocalEntityConnection instance
   * @param database the Database instance
   * @param domain the domain model
   * @param connection the Connection object to base this EntityConnection on, it is assumed to be in a valid state
   */
  DefaultLocalEntityConnection(Database database, Domain domain, Connection connection) throws DatabaseException {
    this(domain, DatabaseConnection.databaseConnection(configureDatabase(database, domain), connection));
  }

  private DefaultLocalEntityConnection(Domain domain, DatabaseConnection connection) throws DatabaseException {
    this.domain = domain;
    this.connection = connection;
    this.domain.configureConnection(connection);
    this.domainEntities = domain.entities();
    this.selectQueries = new SelectQueries(connection.database());
  }

  @Override
  public Entities entities() {
    return domainEntities;
  }

  @Override
  public User user() {
    return connection.user();
  }

  @Override
  public boolean isConnected() {
    synchronized (connection) {
      return connection.isConnected();
    }
  }

  @Override
  public void close() {
    synchronized (connection) {
      connection.close();
    }
  }

  @Override
  public void beginTransaction() {
    synchronized (connection) {
      connection.beginTransaction();
    }
  }

  @Override
  public boolean isTransactionOpen() {
    synchronized (connection) {
      return connection.isTransactionOpen();
    }
  }

  @Override
  public void rollbackTransaction() {
    synchronized (connection) {
      connection.rollbackTransaction();
    }
  }

  @Override
  public void commitTransaction() {
    synchronized (connection) {
      connection.commitTransaction();
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
  public Entity.Key insert(Entity entity) throws DatabaseException {
    return insert(singletonList(requireNonNull(entity, "entity"))).iterator().next();
  }

  @Override
  public Collection<Entity.Key> insert(Collection<? extends Entity> entities) throws DatabaseException {
    if (requireNonNull(entities, ENTITIES).isEmpty()) {
      return emptyList();
    }
    checkIfReadOnly(entities);

    List<Entity.Key> insertedKeys = new ArrayList<>(entities.size());
    List<Object> statementValues = new ArrayList<>();
    List<ColumnDefinition<?>> statementColumns = new ArrayList<>();
    PreparedStatement statement = null;
    String insertQuery = null;
    synchronized (connection) {
      try {
        for (Entity entity : entities) {
          EntityDefinition entityDefinition = domainEntities.definition(entity.entityType());
          KeyGenerator keyGenerator = entityDefinition.keyGenerator();
          keyGenerator.beforeInsert(entity, connection);

          populateColumnsAndValues(entity, insertableColumns(entityDefinition, keyGenerator.isInserted()),
                  statementColumns, statementValues, columnDefinition -> entity.contains(columnDefinition.attribute()));
          if (keyGenerator.isInserted() && statementColumns.isEmpty()) {
            throw new SQLException("Unable to insert entity " + entity.entityType() + ", no values to insert");
          }

          insertQuery = insertQuery(entityDefinition.tableName(), statementColumns);
          statement = prepareStatement(insertQuery, keyGenerator.returnGeneratedKeys());
          executeStatement(statement, insertQuery, statementColumns, statementValues);
          keyGenerator.afterInsert(entity, connection, statement);

          insertedKeys.add(entity.primaryKey());

          statement.close();
          statementColumns.clear();
          statementValues.clear();
        }
        commitIfTransactionIsNotOpen();

        return insertedKeys;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(insertQuery, statementValues, statementColumns, e), e);
        throw translateSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public void update(Entity entity) throws DatabaseException {
    update(singletonList(requireNonNull(entity, "entity")));
  }

  @Override
  public Entity updateSelect(Entity entity) throws DatabaseException {
    return updateSelect(singletonList(requireNonNull(entity, "entity"))).iterator().next();
  }

  @Override
  public void update(Collection<? extends Entity> entities) throws DatabaseException {
    update(entities, null);
  }

  @Override
  public Collection<Entity> updateSelect(Collection<? extends Entity> entities) throws DatabaseException {
    if (requireNonNull(entities, ENTITIES).isEmpty()) {
      return emptyList();
    }
    List<Entity> updatedEntities = new ArrayList<>(entities.size());
    update(entities, updatedEntities);

    return updatedEntities;
  }

  @Override
  public int update(Update update) throws DatabaseException {
    if (requireNonNull(update, CONDITION).columnValues().isEmpty()) {
      throw new IllegalArgumentException("No attribute values provided for update");
    }
    checkIfReadOnly(update.where().entityType());

    List<Object> statementValues = new ArrayList<>();
    List<ColumnDefinition<?>> statementColumns = new ArrayList<>();
    PreparedStatement statement = null;
    String updateQuery = null;
    synchronized (connection) {
      try {
        EntityDefinition entityDefinition = domainEntities.definition(update.where().entityType());
        for (Map.Entry<Column<?>, Object> columnValue : update.columnValues().entrySet()) {
          ColumnDefinition<Object> columnDefinition = entityDefinition.columnDefinition((Column<Object>) columnValue.getKey());
          if (!columnDefinition.isUpdatable()) {
            throw new UpdateException("Column is not updatable: " + columnDefinition.attribute());
          }
          statementColumns.add(columnDefinition);
          statementValues.add(columnDefinition.attribute().validateType(columnValue.getValue()));
        }
        updateQuery = updateQuery(entityDefinition.tableName(), statementColumns, update.where().toString(entityDefinition));
        statement = prepareStatement(updateQuery);
        statementColumns.addAll(columnDefinitions(entityDefinition, update.where().columns()));
        statementValues.addAll(update.where().values());
        int updatedRows = executeStatement(statement, updateQuery, statementColumns, statementValues);
        commitIfTransactionIsNotOpen();

        return updatedRows;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, statementColumns, e), e);
        throw translateSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public int delete(Condition condition) throws DatabaseException {
    checkIfReadOnly(requireNonNull(condition, CONDITION).entityType());

    EntityDefinition entityDefinition = domainEntities.definition(condition.entityType());
    List<?> statementValues = condition.values();
    List<ColumnDefinition<?>> statementColumns = columnDefinitions(entityDefinition, condition.columns());
    PreparedStatement statement = null;
    String deleteQuery = null;
    synchronized (connection) {
      try {
        deleteQuery = deleteQuery(entityDefinition.tableName(), condition.toString(entityDefinition));
        statement = prepareStatement(deleteQuery);
        int deleteCount = executeStatement(statement, deleteQuery, statementColumns, statementValues);
        commitIfTransactionIsNotOpen();

        return deleteCount;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(deleteQuery, statementValues, statementColumns, e), e);
        throw translateSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public void delete(Entity.Key key) throws DatabaseException {
    delete(singletonList(requireNonNull(key, "key")));
  }

  @Override
  public void delete(Collection<Entity.Key> keys) throws DatabaseException {
    if (requireNonNull(keys, "keys").isEmpty()) {
      return;
    }
    Map<EntityType, List<Entity.Key>> keysByEntityType = Entity.mapKeysToType(keys);
    checkIfReadOnly(keysByEntityType.keySet());

    List<?> statementValues = null;
    List<ColumnDefinition<?>> statementColumns = null;
    PreparedStatement statement = null;
    Condition condition = null;
    String deleteQuery = null;
    synchronized (connection) {
      try {
        int deleteCount = 0;
        for (Map.Entry<EntityType, List<Entity.Key>> entityTypeKeys : keysByEntityType.entrySet()) {
          EntityDefinition entityDefinition = domainEntities.definition(entityTypeKeys.getKey());
          condition = keys(entityTypeKeys.getValue());
          statementValues = condition.values();
          statementColumns = columnDefinitions(entityDefinition, condition.columns());
          deleteQuery = deleteQuery(entityDefinition.tableName(), condition.toString(entityDefinition));
          statement = prepareStatement(deleteQuery);
          deleteCount += executeStatement(statement, deleteQuery, statementColumns, statementValues);
          statement.close();
        }
        if (keys.size() != deleteCount) {
          throw new DeleteException(deleteCount + " rows deleted, expected " + keys.size());
        }
        commitIfTransactionIsNotOpen();
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(deleteQuery, condition == null ? emptyList() : statementValues, statementColumns, e), e);
        throw translateSQLException(e);
      }
      catch (DeleteException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(deleteQuery, statementValues, statementColumns, e), e);
        throw e;
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public Entity select(Entity.Key key) throws DatabaseException {
    return selectSingle(key(key));
  }

  @Override
  public Entity selectSingle(Condition condition) throws DatabaseException {
    return selectSingle(Select.where(condition).build());
  }

  @Override
  public Entity selectSingle(Select select) throws DatabaseException {
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
  public Collection<Entity> select(Collection<Entity.Key> keys) throws DatabaseException {
    if (requireNonNull(keys, "keys").isEmpty()) {
      return emptyList();
    }

    synchronized (connection) {
      try {
        List<Entity> result = new ArrayList<>();
        for (List<Entity.Key> entityTypeKeys : Entity.mapKeysToType(keys).values()) {
          result.addAll(doSelect(Select.where(keys(entityTypeKeys)).build()));
        }
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        throw translateSQLException(e);
      }
    }
  }

  @Override
  public List<Entity> select(Condition condition) throws DatabaseException {
    return select(Select.where(condition).build());
  }

  @Override
  public List<Entity> select(Select select) throws DatabaseException {
    requireNonNull(select, "select");
    synchronized (connection) {
      try {
        List<Entity> result = doSelect(select);
        if (!select.forUpdate()) {
          commitIfTransactionIsNotOpen();
        }

        return result;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        throw translateSQLException(e);
      }
    }
  }

  @Override
  public <T> List<T> select(Column<T> column) throws DatabaseException {
    return select(requireNonNull(column), Select.all(column.entityType())
            .orderBy(ascending(column))
            .build());
  }

  @Override
  public <T> List<T> select(Column<T> column, Condition condition) throws DatabaseException {
    return select(column, Select.where(condition)
            .orderBy(ascending(column))
            .build());
  }

  @Override
  public <T> List<T> select(Column<T> column, Select select) throws DatabaseException {
    EntityDefinition entityDefinition = domainEntities.definition(requireNonNull(column, "column").entityType());
    if (!requireNonNull(select, "select").where().entityType().equals(column.entityType())) {
      throw new IllegalArgumentException("Condition entity type " + column.entityType() + " required, got " + select.where().entityType());
    }
    ColumnDefinition<T> columnDefinition = entityDefinition.columnDefinition(column);
    verifyColumnIsSelectable(columnDefinition, entityDefinition);
    Condition combinedCondition = and(select.where(), column(column).isNotNull());
    String selectQuery = selectQueries.builder(entityDefinition)
            .select(select, false)
            .columns(columnDefinition.columnExpression())
            .where(combinedCondition)
            .groupBy(columnDefinition.columnExpression())
            .build();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    synchronized (connection) {
      try {
        statement = prepareStatement(selectQuery);
        resultSet = executeStatement(statement, selectQuery, combinedCondition, entityDefinition);
        List<T> result = columnDefinition.resultPacker().pack(resultSet);
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(selectQuery, asList(column, select),
                columnDefinitions(entityDefinition, combinedCondition.columns()), e), e);
        throw translateSQLException(e);
      }
      finally {
        closeSilently(resultSet);
        closeSilently(statement);
        countQuery(selectQuery);
      }
    }
  }

  @Override
  public int count(Condition condition) throws DatabaseException {
    EntityDefinition entityDefinition = domainEntities.definition(requireNonNull(condition, CONDITION).entityType());
    String selectQuery = selectQueries.builder(entityDefinition)
            .columns("count(*)")
            .subquery(selectQueries.builder(entityDefinition)
                    .select(Select.where(condition)
                            .attributes(entityDefinition.primaryKeyColumns())
                            .build())
                    .build())
            .build();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    synchronized (connection) {
      try {
        statement = prepareStatement(selectQuery);
        resultSet = executeStatement(statement, selectQuery, condition, entityDefinition);
        List<Integer> result = INTEGER_RESULT_PACKER.pack(resultSet);
        commitIfTransactionIsNotOpen();
        if (result.isEmpty()) {
          throw new SQLException("Row count query returned no value");
        }

        return result.get(0);
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(selectQuery, condition.values(),
                columnDefinitions(entityDefinition, condition.columns()), e), e);
        throw translateSQLException(e);
      }
      finally {
        closeSilently(resultSet);
        closeSilently(statement);
        countQuery(selectQuery);
      }
    }
  }

  @Override
  public Map<EntityType, Collection<Entity>> dependencies(Collection<? extends Entity> entities) throws DatabaseException {
    if (requireNonNull(entities, ENTITIES).isEmpty()) {
      return emptyMap();
    }

    Map<EntityType, Collection<Entity>> dependencyMap = new HashMap<>();
    for (ForeignKeyDefinition foreignKeyReference : nonSoftForeignKeyReferences(entities.iterator().next().entityType())) {
      List<Entity> dependencies = select(Select.where(foreignKey(foreignKeyReference.attribute()).in(entities))
              .fetchDepth(1)
              .build())
              .stream()
              .map(Entity::immutable)
              .collect(toList());
      if (!dependencies.isEmpty()) {
        dependencyMap.computeIfAbsent(foreignKeyReference.entityType(), k -> new HashSet<>()).addAll(dependencies);
      }
    }

    return dependencyMap;
  }

  @Override
  public <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType) throws DatabaseException {
    return execute(functionType, null);
  }

  @Override
  public <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType, T argument) throws DatabaseException {
    requireNonNull(functionType, "functionType");
    DatabaseException exception = null;
    try {
      logEntry("executeFunction: " + functionType, argument);
      synchronized (connection) {
        return functionType.execute((C) this, domain.function(functionType), argument);
      }
    }
    catch (DatabaseException e) {
      exception = e;
      LOG.error(createLogMessage(functionType.name(), argument instanceof List ? (List<?>) argument : singletonList(argument), emptyList(), e), e);
      throw e;
    }
    finally {
      logExit("executeFunction: " + functionType, exception);
    }
  }

  @Override
  public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType) throws DatabaseException {
    execute(procedureType, null);
  }

  @Override
  public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType, T argument) throws DatabaseException {
    requireNonNull(procedureType, "procedureType");
    DatabaseException exception = null;
    try {
      logEntry("executeProcedure: " + procedureType, argument);
      synchronized (connection) {
        procedureType.execute((C) this, domain.procedure(procedureType), argument);
      }
    }
    catch (DatabaseException e) {
      exception = e;
      LOG.error(createLogMessage(procedureType.name(), argument instanceof List ? (List<?>) argument : singletonList(argument), emptyList(), e), e);
      throw e;
    }
    finally {
      logExit("executeProcedure: " + procedureType, exception);
    }
  }

  @Override
  public <T, R, P> R fillReport(ReportType<T, R, P> reportType, P reportParameters) throws ReportException {
    requireNonNull(reportType, "report");
    Exception exception = null;
    synchronized (connection) {
      try {
        logEntry("fillReport: " + reportType, reportParameters);
        R result = reportType.fillReport(domain.report(reportType), connection.getConnection(), reportParameters);
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(null, singletonList(reportType), emptyList(), e), e);
        throw new ReportException(translateSQLException(e));
      }
      catch (ReportException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(null, singletonList(reportType), emptyList(), e), e);
        throw e;
      }
      finally {
        logExit("fillReport: " + reportType, exception);
      }
    }
  }

  @Override
  public void writeBlob(Entity.Key primaryKey, Column<byte[]> blobColumn, byte[] blobData) throws DatabaseException {
    requireNonNull(blobData, "blobData");
    EntityDefinition entityDefinition = domainEntities.definition(requireNonNull(primaryKey, "primaryKey").entityType());
    checkIfReadOnly(entityDefinition.entityType());
    ColumnDefinition<byte[]> columnDefinition = entityDefinition.columnDefinition(blobColumn);
    Condition condition = key(primaryKey);
    String updateQuery = updateQuery(entityDefinition.tableName(), singletonList(columnDefinition), condition.toString(entityDefinition));
    List<Object> statementValues = new ArrayList<>();
    statementValues.add(null);//the blob value, binary stream set explicitly later
    statementValues.addAll(condition.values());
    List<ColumnDefinition<?>> statementColumns = new ArrayList<>();
    statementColumns.add(columnDefinition);
    statementColumns.addAll(columnDefinitions(entityDefinition, condition.columns()));
    synchronized (connection) {
      Exception exception = null;
      PreparedStatement statement = null;
      try {
        logEntry("writeBlob", updateQuery);
        statement = prepareStatement(updateQuery);
        setParameterValues(statement, statementColumns, statementValues);
        statement.setBinaryStream(1, new ByteArrayInputStream(blobData));//no need to close ByteArrayInputStream
        if (statement.executeUpdate() > 1) {
          throw new UpdateException("Blob write updated more than one row, key: " + primaryKey);
        }
        commitIfTransactionIsNotOpen();
      }
      catch (SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, statementColumns, exception), e);
        throw translateSQLException(e);
      }
      catch (UpdateException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, statementColumns, e), e);
        throw e;
      }
      finally {
        closeSilently(statement);
        logExit("writeBlob", exception);
        countQuery(updateQuery);
      }
    }
  }

  @Override
  public byte[] readBlob(Entity.Key primaryKey, Column<byte[]> blobColumn) throws DatabaseException {
    EntityDefinition entityDefinition = domainEntities.definition(requireNonNull(primaryKey, "primaryKey").entityType());
    ColumnDefinition<byte[]> columnDefinition = entityDefinition.columnDefinition(blobColumn);
    PreparedStatement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    Condition condition = key(primaryKey);
    List<ColumnDefinition<?>> statementColumns = columnDefinitions(entityDefinition, condition.columns());
    String selectQuery = selectQueries.builder(entityDefinition)
            .columns(columnDefinition.columnExpression())
            .where(condition)
            .build();
    synchronized (connection) {
      try {
        logEntry("readBlob", selectQuery);
        statement = prepareStatement(selectQuery);
        setParameterValues(statement, statementColumns, condition.values());

        resultSet = statement.executeQuery();
        List<byte[]> result = BLOB_RESULT_PACKER.pack(resultSet, 1);
        if (result.isEmpty()) {
          return null;
        }
        byte[] byteResult = result.get(0);
        commitIfTransactionIsNotOpen();

        return byteResult;
      }
      catch (SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(selectQuery, condition.values(), statementColumns, exception), e);
        throw translateSQLException(e);
      }
      finally {
        closeSilently(statement);
        closeSilently(resultSet);
        logExit("readBlob", exception);
        countQuery(selectQuery);
      }
    }
  }

  @Override
  public DatabaseConnection databaseConnection() {
    return connection;
  }

  @Override
  public ResultIterator<Entity> iterator(Condition condition) throws DatabaseException {
    return iterator(Select.where(condition).build());
  }

  @Override
  public ResultIterator<Entity> iterator(Select select) throws DatabaseException {
    synchronized (connection) {
      try {
        return entityIterator(select);
      }
      catch (SQLException e) {
        throw translateSQLException(e);
      }
    }
  }

  @Override
  public boolean isOptimisticLockingEnabled() {
    return optimisticLockingEnabled;
  }

  @Override
  public void setOptimisticLockingEnabled(boolean optimisticLockingEnabled) {
    this.optimisticLockingEnabled = optimisticLockingEnabled;
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

  @Override
  public Domain domain() {
    return domain;
  }

  private void update(Collection<? extends Entity> entities, List<Entity> updatedEntities) throws DatabaseException {
    Map<EntityType, List<Entity>> entitiesByEntityType = Entity.mapToType(entities);
    checkIfReadOnly(entitiesByEntityType.keySet());

    List<Object> statementValues = new ArrayList<>();
    List<ColumnDefinition<?>> statementColumns = new ArrayList<>();
    PreparedStatement statement = null;
    String updateQuery = null;
    synchronized (connection) {
      try {
        if (optimisticLockingEnabled) {
          performOptimisticLocking(entitiesByEntityType);
        }

        for (Map.Entry<EntityType, List<Entity>> entityTypeEntities : entitiesByEntityType.entrySet()) {
          EntityDefinition entityDefinition = domainEntities.definition(entityTypeEntities.getKey());
          List<ColumnDefinition<?>> updatableColumns = updatableColumns(entityDefinition);

          List<Entity> entitiesToUpdate = entityTypeEntities.getValue();
          for (Entity entity : entitiesToUpdate) {
            populateColumnsAndValues(entity, updatableColumns, statementColumns, statementValues,
                    columnDefinition -> entity.isModified(columnDefinition.attribute()));
            if (statementColumns.isEmpty()) {
              throw new SQLException("Unable to update entity " + entity.entityType() + ", no modified values found");
            }

            Condition condition = key(entity.originalPrimaryKey());
            updateQuery = updateQuery(entityDefinition.tableName(), statementColumns, condition.toString(entityDefinition));
            statement = prepareStatement(updateQuery);
            statementColumns.addAll(columnDefinitions(entityDefinition, condition.columns()));
            statementValues.addAll(condition.values());
            int updatedRows = executeStatement(statement, updateQuery, statementColumns, statementValues);
            if (updatedRows == 0) {
              throw new UpdateException("Update did not affect any rows, entityType: " + entityTypeEntities.getKey());
            }

            statement.close();
            statementColumns.clear();
            statementValues.clear();
          }
          if (updatedEntities != null) {
            List<Entity> selected = doSelect(Select.where(keys(Entity.primaryKeys(entitiesToUpdate))).build(), 0);//bypass caching
            if (selected.size() != entitiesToUpdate.size()) {
              throw new UpdateException(entitiesToUpdate.size() + " updated rows expected, query returned " +
                      selected.size() + ", entityType: " + entityTypeEntities.getKey());
            }
            updatedEntities.addAll(selected);
          }
        }
        commitIfTransactionIsNotOpen();
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, statementColumns, e), e);
        throw translateSQLException(e);
      }
      catch (RecordModifiedException e) {
        rollbackQuietlyIfTransactionIsNotOpen();//releasing the select for update lock
        LOG.debug(e.getMessage(), e);
        throw e;
      }
      catch (UpdateException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, statementColumns, e), e);
        throw e;
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
      EntityDefinition definition = domainEntities.definition(entitiesByEntityTypeEntry.getKey());
      if (definition.isOptimisticLockingEnabled()) {
        checkIfMissingOrModified(entitiesByEntityTypeEntry.getKey(), entitiesByEntityTypeEntry.getValue());
      }
    }
  }

  private void checkIfMissingOrModified(EntityType entityType, List<Entity> entities) throws SQLException, RecordModifiedException {
    Collection<Entity.Key> originalKeys = Entity.originalPrimaryKeys(entities);
    Select selectForUpdate = Select.where(keys(originalKeys))
            .attributes(primaryKeyAndWritableColumnAttributes(entityType))
            .forUpdate()
            .build();
    Map<Entity.Key, Entity> currentEntitiesByKey = Entity.mapToPrimaryKey(doSelect(selectForUpdate));
    for (Entity entity : entities) {
      Entity current = currentEntitiesByKey.get(entity.originalPrimaryKey());
      if (current == null) {
        Entity original = entity.copy();
        original.revert();

        throw new RecordModifiedException(entity, null, MESSAGES.getString(RECORD_MODIFIED)
                + ", " + original + " " + MESSAGES.getString("has_been_deleted"));
      }
      Collection<Attribute<?>> modified = Entity.modifiedColumnAttributes(entity, current);
      if (!modified.isEmpty()) {
        throw new RecordModifiedException(entity, current, createModifiedExceptionMessage(entity, current, modified));
      }
    }
  }

  private List<Entity> doSelect(Select select) throws SQLException {
    List<Entity> result = cachedResult(select);
    if (result != null) {
      LOG.debug("Returning cached result: " + select.where().entityType());
      return result;
    }

    return cacheResult(select, doSelect(select, 0));
  }

  private List<Entity> doSelect(Select select, int currentForeignKeyFetchDepth) throws SQLException {
    List<Entity> result;
    try (ResultIterator<Entity> iterator = entityIterator(select)) {
      result = packResult(iterator);
    }
    if (!select.forUpdate() && !result.isEmpty()) {
      setForeignKeys(result, select, currentForeignKeyFetchDepth);
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
  private void setForeignKeys(List<Entity> entities, Select select,
                              int currentForeignKeyFetchDepth) throws SQLException {
    Collection<ForeignKeyDefinition> foreignKeysToSet =
            foreignKeysToSet(entities.get(0).entityType(), select.attributes());
    for (ForeignKeyDefinition foreignKeyDefinition : foreignKeysToSet) {
      ForeignKey foreignKey = foreignKeyDefinition.attribute();
      int conditionFetchDepthLimit = select.fetchDepth(foreignKey)
              .orElse(foreignKeyDefinition.fetchDepth());
      if (isWithinFetchDepthLimit(currentForeignKeyFetchDepth, conditionFetchDepthLimit)
              && containsReferenceAttributes(entities.get(0), foreignKey.references())) {
        try {
          logEntry("setForeignKeys", foreignKeyDefinition);
          Collection<Entity.Key> referencedKeys = Entity.referencedKeys(foreignKey, entities);
          if (referencedKeys.isEmpty()) {
            for (int j = 0; j < entities.size(); j++) {
              entities.get(j).put(foreignKey, null);
            }
          }
          else {
            Map<Entity.Key, Entity> referencedEntitiesMappedByKey = selectReferencedEntities(foreignKeyDefinition,
                    new ArrayList<>(referencedKeys), currentForeignKeyFetchDepth, conditionFetchDepthLimit);
            for (int j = 0; j < entities.size(); j++) {
              Entity entity = entities.get(j);
              Entity.Key referencedKey = entity.referencedKey(foreignKey);
              entity.put(foreignKey, referencedEntity(referencedKey, referencedEntitiesMappedByKey));
            }
          }
        }
        finally {
          logExit("setForeignKeys");
        }
      }
    }
  }

  private Collection<ForeignKeyDefinition> foreignKeysToSet(EntityType entityType,
                                                            Collection<Attribute<?>> conditionSelectAttributes) {
    Collection<ForeignKeyDefinition> foreignKeyDefinitions = domainEntities.definition(entityType).foreignKeyDefinitions();
    if (conditionSelectAttributes.isEmpty()) {
      return foreignKeyDefinitions;
    }

    Set<Attribute<?>> selectAttributes = new HashSet<>(conditionSelectAttributes);

    return foreignKeyDefinitions.stream()
            .filter(foreignKeyDefinition -> selectAttributes.contains(foreignKeyDefinition.attribute()))
            .collect(toList());
  }

  private boolean isWithinFetchDepthLimit(int currentForeignKeyFetchDepth, int conditionFetchDepthLimit) {
    return !limitForeignKeyFetchDepth || conditionFetchDepthLimit == -1 || currentForeignKeyFetchDepth < conditionFetchDepthLimit;
  }

  private Map<Entity.Key, Entity> selectReferencedEntities(ForeignKeyDefinition foreignKeyDefinition, List<Entity.Key> referencedKeys,
                                                           int currentForeignKeyFetchDepth, int conditionFetchDepthLimit) throws SQLException {
    Entity.Key referencedKey = referencedKeys.get(0);
    Collection<Column<?>> keyColumns = referencedKey.columns();
    List<Entity> referencedEntities = new ArrayList<>(referencedKeys.size());
    int maximumNumberOfParameters = connection.database().maximumNumberOfParameters();
    for (int i = 0; i < referencedKeys.size(); i += maximumNumberOfParameters) {
      List<Entity.Key> keys = referencedKeys.subList(i, Math.min(i + maximumNumberOfParameters, referencedKeys.size()));
      Select referencedEntitiesCondition = Select.where(keys(keys))
              .fetchDepth(conditionFetchDepthLimit)
              .attributes(attributesToSelect(foreignKeyDefinition, keyColumns))
              .build();
      referencedEntities.addAll(doSelect(referencedEntitiesCondition, currentForeignKeyFetchDepth + 1).stream()
              .map(Entity::immutable)
              .collect(toList()));
    }

    if (referencedKey.isPrimaryKey()) {
      return Entity.mapToPrimaryKey(referencedEntities);
    }

    return referencedEntities.stream()
            .collect(toMap(entity -> createKey(entity, keyColumns), Function.identity()));
  }

  private Entity.Key createKey(Entity entity, Collection<Column<?>> keyColumns) {
    Entity.Key.Builder keyBuilder = entities().keyBuilder(entity.entityType());
    keyColumns.forEach(column -> keyBuilder.with((Column<Object>) column, entity.get(column)));

    return keyBuilder.build();
  }

  private ResultIterator<Entity> entityIterator(Select select) throws SQLException {
    requireNonNull(select, CONDITION);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectQuery = null;
    EntityDefinition entityDefinition = domainEntities.definition(select.where().entityType());
    SelectQueries.Builder selectQueryBuilder = selectQueries.builder(entityDefinition)
            .select(select);
    Condition condition = select.where();
    try {
      selectQuery = selectQueryBuilder.build();
      statement = prepareStatement(selectQuery, false, select.queryTimeout());
      resultSet = executeStatement(statement, selectQuery, condition, entityDefinition);

      return new EntityResultIterator(statement, resultSet,
              new EntityResultPacker(entityDefinition, selectQueryBuilder.selectedColumns()));
    }
    catch (SQLException e) {
      closeSilently(resultSet);
      closeSilently(statement);
      LOG.error(createLogMessage(selectQuery, condition.values(),
              columnDefinitions(entityDefinition, condition.columns()), e), e);
      throw e;
    }
  }

  private int executeStatement(PreparedStatement statement, String query,
                               List<ColumnDefinition<?>> statementColumns,
                               List<?> statementValues) throws SQLException {
    SQLException exception = null;
    try {
      logEntry(EXECUTE_STATEMENT, statementValues);
      setParameterValues(statement, statementColumns, statementValues);

      return statement.executeUpdate();
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      logExit(EXECUTE_STATEMENT, exception);
      countQuery(query);
      if (LOG.isDebugEnabled()) {
        LOG.debug(createLogMessage(query, statementValues, statementColumns, exception));
      }
    }
  }

  private ResultSet executeStatement(PreparedStatement statement, String query,
                                     Condition condition, EntityDefinition entityDefinition) throws SQLException {
    SQLException exception = null;
    List<?> statementValues = condition.values();
    List<ColumnDefinition<?>> statementColumns = columnDefinitions(entityDefinition, condition.columns());
    try {
      logEntry(EXECUTE_STATEMENT, statementValues);
      setParameterValues(statement, statementColumns, statementValues);

      return statement.executeQuery();
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      logExit(EXECUTE_STATEMENT, exception);
      countQuery(query);
      if (LOG.isDebugEnabled()) {
        LOG.debug(createLogMessage(query, statementValues, statementColumns, exception));
      }
    }
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
   * @return all non-soft foreign keys in the domain referencing entities of type {@code entityType}
   */
  private Collection<ForeignKeyDefinition> nonSoftForeignKeyReferences(EntityType entityType) {
    return nonSoftForeignKeyReferenceCache.computeIfAbsent(entityType, this::initializeNonSoftForeignKeyReferences);
  }

  private List<ForeignKeyDefinition> initializeNonSoftForeignKeyReferences(EntityType entityType) {
    return domainEntities.definitions().stream()
            .flatMap(entityDefinition -> entityDefinition.foreignKeyDefinitions().stream())
            .filter(foreignKeyDefinition -> !foreignKeyDefinition.isSoftReference())
            .filter(foreignKeyDefinition -> foreignKeyDefinition.referencedType().equals(entityType))
            .collect(toList());
  }

  private List<Entity> packResult(ResultIterator<Entity> iterator) throws SQLException {
    SQLException packingException = null;
    List<Entity> result = new ArrayList<>();
    try {
      logEntry("packResult");
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
      logExit("packResult", packingException, "row count: " + result.size());
    }
  }

  private List<ColumnDefinition<?>> insertableColumns(EntityDefinition entityDefinition,
                                                      boolean includePrimaryKeyColumns) {
    return insertableColumnsCache.computeIfAbsent(entityDefinition.entityType(), entityType ->
            entityDefinition.writableColumnDefinitions(includePrimaryKeyColumns, true));
  }

  private List<ColumnDefinition<?>> updatableColumns(EntityDefinition entityDefinition) {
    return updatableColumnsCache.computeIfAbsent(entityDefinition.entityType(), entityType ->
            entityDefinition.writableColumnDefinitions(true, false));
  }

  private List<Attribute<?>> primaryKeyAndWritableColumnAttributes(EntityType entityType) {
    return primaryKeyAndWritableColumnsCache.computeIfAbsent(entityType, e ->
            collectPrimaryKeyAndWritableColumnAttributes(entityType));
  }

  private List<Attribute<?>> collectPrimaryKeyAndWritableColumnAttributes(EntityType entityType) {
    EntityDefinition entityDefinition = domainEntities.definition(entityType);
    List<ColumnDefinition<?>> writableAndPrimaryKeyColumns =
            new ArrayList<>(entityDefinition.writableColumnDefinitions(true, true));
    entityDefinition.primaryKeyColumnDefinitions().forEach(primaryKeyColumn -> {
      if (!writableAndPrimaryKeyColumns.contains(primaryKeyColumn)) {
        writableAndPrimaryKeyColumns.add(primaryKeyColumn);
      }
    });

    return writableAndPrimaryKeyColumns.stream()
            .map(ColumnDefinition::attribute)
            .collect(toList());
  }

  private DatabaseException translateSQLException(SQLException exception) {
    Database database = connection.database();
    if (database.isUniqueConstraintException(exception)) {
      return new UniqueConstraintException(exception, database.errorMessage(exception));
    }
    else if (database.isReferentialIntegrityException(exception)) {
      return new ReferentialIntegrityException(exception, database.errorMessage(exception));
    }
    else if (database.isTimeoutException(exception)) {
      return new QueryTimeoutException(exception, database.errorMessage(exception));
    }

    return new DatabaseException(exception, database.errorMessage(exception));
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
    if (!isTransactionOpen()) {
      connection.commit();
    }
  }

  private void rollbackQuietlyIfTransactionIsNotOpen() {
    if (!isTransactionOpen()) {
      rollbackQuietly();
    }
  }

  private void logExit(String method) {
    logExit(method, null);
  }

  private void logExit(String method, Throwable exception) {
    logExit(method, exception, null);
  }

  private void logExit(String method, Throwable exception, String exitMessage) {
    MethodLogger methodLogger = connection.getMethodLogger();
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.exit(method, exception, exitMessage);
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

  private String createLogMessage(String sqlStatement, List<?> values, List<ColumnDefinition<?>> columnDefinitions, Exception exception) {
    StringBuilder logMessage = new StringBuilder(user().toString()).append("\n");
    String valueString = "[" + createValueString(values, columnDefinitions) + "]";
    logMessage.append(sqlStatement == null ? "no sql statement" : sqlStatement).append(", ").append(valueString);
    if (exception != null) {
      logMessage.append("\n").append(" [Exception: ").append(exception.getMessage()).append("]");
    }

    return logMessage.toString();
  }

  private void countQuery(String query) {
    connection.database().countQuery(query);
  }

  private void checkIfReadOnly(Collection<? extends Entity> entities) throws DatabaseException {
    for (Entity entity : entities) {
      checkIfReadOnly(entity.entityType());
    }
  }

  private void checkIfReadOnly(Set<EntityType> entityTypes) throws DatabaseException {
    for (EntityType entityType : entityTypes) {
      checkIfReadOnly(entityType);
    }
  }

  private void checkIfReadOnly(EntityType entityType) throws DatabaseException {
    if (domainEntities.definition(entityType).isReadOnly()) {
      throw new DatabaseException("Entities of type: " + entityType + " are read only");
    }
  }

  private List<Entity> cachedResult(Select select) {
    if (queryCacheEnabled && !select.forUpdate()) {
      return queryCache.get(select);
    }

    return null;
  }

  private List<Entity> cacheResult(Select select, List<Entity> result) {
    if (queryCacheEnabled && !select.forUpdate()) {
      LOG.debug("Caching result: " + select.where().entityType());
      queryCache.put(select, result);
    }

    return result;
  }

  private static Entity referencedEntity(Entity.Key referencedKey, Map<Entity.Key, Entity> entityKeyMap) {
    if (referencedKey == null) {
      return null;
    }
    Entity referencedEntity = entityKeyMap.get(referencedKey);
    if (referencedEntity == null) {
      //if the referenced entity is not found (it's been deleted or has been filtered out of an underlying view for example),
      //we create an empty entity wrapping the key since that's the best we can do under the circumstances
      referencedEntity = Entity.entity(referencedKey).immutable();
    }

    return referencedEntity;
  }

  /**
   *
   * @param entityDefinition
   * @param columns
   * @return
   */
  private static List<ColumnDefinition<?>> columnDefinitions(EntityDefinition entityDefinition,
                                                             List<Column<?>> columns) {
    return columns.stream()
            .map(entityDefinition::columnDefinition)
            .collect(toList());
  }

  private static void setParameterValues(PreparedStatement statement, List<ColumnDefinition<?>> statementColumns,
                                         List<?> statementValues) throws SQLException {
    if (statementValues.isEmpty()) {
      return;
    }
    if (statementColumns.size() != statementValues.size()) {
      throw new SQLException("Parameter column value count mismatch: " +
              "expected: " + statementValues.size() + ", got: " + statementColumns.size());
    }

    for (int i = 0; i < statementColumns.size(); i++) {
      setParameterValue(statement, (ColumnDefinition<Object>) statementColumns.get(i), statementValues.get(i), i + 1);
    }
  }

  private static void setParameterValue(PreparedStatement statement, ColumnDefinition<Object> columnDefinition,
                                        Object value, int parameterIndex) throws SQLException {
    Object columnValue = columnDefinition.toColumnValue(value, statement);
    try {
      if (columnValue == null) {
        statement.setNull(parameterIndex, columnDefinition.columnType());
      }
      else {
        statement.setObject(parameterIndex, columnValue, columnDefinition.columnType());
      }
    }
    catch (SQLException e) {
      LOG.error("Unable to set parameter: " + columnDefinition + ", value: " + value + ", value class: " + (value == null ? "null" : value.getClass()), e);
      throw e;
    }
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
                                                       Collection<Attribute<?>> modifiedAttributes) {
    return modifiedAttributes.stream()
            .map(attribute -> " \n" + attribute + ": " + entity.original(attribute) + " -> " + modified.get(attribute))
            .collect(joining("", MESSAGES.getString(RECORD_MODIFIED) + ", " + entity.entityType(), ""));
  }

  private static boolean containsReferenceAttributes(Entity entity, List<ForeignKey.Reference<?>> references) {
    for (int i = 0; i < references.size(); i++) {
      if (!entity.contains(references.get(i).column())) {
        return false;
      }
    }

    return true;
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
    if (columnDefinitions == null || columnDefinitions.isEmpty()) {
      return "no values";
    }
    List<String> stringValues = new ArrayList<>(values.size());
    for (int i = 0; i < values.size(); i++) {
      ColumnDefinition<Object> columnDefinition = (ColumnDefinition<Object>) columnDefinitions.get(i);
      Object value = values.get(i);
      Object columnValue;
      String stringValue;
      try {
        columnValue = columnDefinition.toColumnValue(value, null);
        stringValue = String.valueOf(value);
      }
      catch (SQLException e) {
        //fallback to the original value
        columnValue = value;
        stringValue = String.valueOf(value);
      }
      stringValues.add(columnValue == null ? "null" : addSingleQuotes(columnDefinition.columnType(), stringValue));
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
    if (columnDefinition.isAggregate()) {
      throw new UnsupportedOperationException("Selecting column values is not implemented for aggregate function values");
    }
    SelectQuery selectQuery = entityDefinition.selectQuery();
    if (selectQuery != null && selectQuery.columns() != null) {
      throw new UnsupportedOperationException("Selecting column values is not implemented for entities with custom column clauses");
    }
  }

  static Database configureDatabase(Database database, Domain domain) throws DatabaseException {
    new DatabaseConfiguration(requireNonNull(domain), requireNonNull(database)).configure();

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

    private void configure() throws DatabaseException {
      synchronized (CONFIGURED_DATABASES) {
        if (!CONFIGURED_DATABASES.contains(this)) {
          domain.configureDatabase(database);
          CONFIGURED_DATABASES.add(this);
        }
      }
    }
  }
}