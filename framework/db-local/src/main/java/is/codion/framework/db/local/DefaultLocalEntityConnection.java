/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;

import static is.codion.common.db.connection.DatabaseConnection.databaseConnection;
import static is.codion.common.db.database.Database.closeSilently;
import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.db.condition.Conditions.where;
import static is.codion.framework.db.local.Queries.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * A default LocalEntityConnection implementation
 */
final class DefaultLocalEntityConnection implements LocalEntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalEntityConnection.class.getName());
  private static final String RECORD_MODIFIED = "record_modified";

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLocalEntityConnection.class);
  private static final String CONDITION_PARAM_NAME = "condition";
  private static final String ENTITIES_PARAM_NAME = "entities";
  private static final String EXECUTE_STATEMENT = "executeStatement";

  private static final ResultPacker<byte[]> BLOB_RESULT_PACKER = resultSet -> resultSet.getBytes(1);
  private static final ResultPacker<Integer> INTEGER_RESULT_PACKER = resultSet -> resultSet.getInt(1);

  private final Domain domain;
  private final Entities domainEntities;
  private final DatabaseConnection connection;
  private final SelectQueries selectQueries;
  private final Map<EntityType, List<ColumnProperty<?>>> insertablePropertiesCache = new HashMap<>();
  private final Map<EntityType, List<ColumnProperty<?>>> updatablePropertiesCache = new HashMap<>();
  private final Map<EntityType, List<ForeignKeyProperty>> foreignKeyReferenceCache = new HashMap<>();
  private final Map<EntityType, Attribute<?>[]> primaryKeyAndWritableColumnPropertiesCache = new HashMap<>();
  private final Map<SelectCondition, List<Entity>> queryCache = new HashMap<>();

  private boolean optimisticLockingEnabled = LocalEntityConnection.USE_OPTIMISTIC_LOCKING.get();
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
    this.domain = requireNonNull(domain, "domain");
    this.domainEntities = domain.getEntities();
    this.connection = databaseConnection(database, user);
    this.selectQueries = new SelectQueries(database);
    this.domain.configureConnection(this.connection);
  }

  /**
   * Constructs a new LocalEntityConnection instance
   * @param database the Database instance
   * @param domain the domain model
   * @param connection the Connection object to base this EntityConnection on, it is assumed to be in a valid state
   */
  DefaultLocalEntityConnection(Database database, Domain domain, Connection connection) throws DatabaseException {
    this.domain = requireNonNull(domain, "domain");
    this.domainEntities = domain.getEntities();
    this.connection = databaseConnection(database, connection);
    this.selectQueries = new SelectQueries(database);
    this.domain.configureConnection(this.connection);
  }

  @Override
  public LocalEntityConnection setMethodLogger(MethodLogger methodLogger) {
    synchronized (connection) {
      connection.setMethodLogger(methodLogger);
    }

    return this;
  }

  @Override
  public MethodLogger getMethodLogger() {
    synchronized (connection) {
      return connection.getMethodLogger();
    }
  }

  @Override
  public Entities getEntities() {
    return domainEntities;
  }

  @Override
  public User getUser() {
    return connection.getUser();
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
  public Key insert(Entity entity) throws DatabaseException {
    return insert(singletonList(requireNonNull(entity, "entity"))).get(0);
  }

  @Override
  public List<Key> insert(List<? extends Entity> entities) throws DatabaseException {
    if (requireNonNull(entities, ENTITIES_PARAM_NAME).isEmpty()) {
      return emptyList();
    }
    checkIfReadOnly(entities);

    List<Key> insertedKeys = new ArrayList<>(entities.size());
    List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String insertQuery = null;
    synchronized (connection) {
      try {
        List<ColumnProperty<?>> statementProperties = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
          Entity entity = entities.get(i);
          EntityDefinition entityDefinition = domainEntities.getDefinition(entity.getEntityType());
          List<ColumnProperty<?>> primaryKeyProperties = entityDefinition.getPrimaryKeyProperties();
          KeyGenerator keyGenerator = entityDefinition.getKeyGenerator();
          keyGenerator.beforeInsert(entity, primaryKeyProperties, connection);

          populatePropertiesAndValues(entity, getInsertableProperties(entityDefinition, keyGenerator.isInserted()),
                  statementProperties, statementValues, columnProperty -> entity.contains(columnProperty.getAttribute()));
          if (statementProperties.isEmpty()) {
            throw new SQLException("Unable to insert entity " + entity.getEntityType() + ", no properties to insert");
          }

          insertQuery = insertQuery(entityDefinition.getTableName(), statementProperties);
          statement = prepareStatement(insertQuery, keyGenerator.returnGeneratedKeys());
          executeStatement(statement, insertQuery, statementProperties, statementValues);
          keyGenerator.afterInsert(entity, primaryKeyProperties, connection, statement);

          insertedKeys.add(entity.getPrimaryKey());

          statement.close();
          statementProperties.clear();
          statementValues.clear();
        }
        commitIfTransactionIsNotOpen();

        return insertedKeys;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(insertQuery, statementValues, e), e);
        throw translateSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public Entity update(Entity entity) throws DatabaseException {
    return update(singletonList(requireNonNull(entity, "entity"))).get(0);
  }

  @Override
  public List<Entity> update(List<? extends Entity> entities) throws DatabaseException {
    if (requireNonNull(entities, ENTITIES_PARAM_NAME).isEmpty()) {
      return emptyList();
    }
    Map<EntityType, List<Entity>> entitiesByEntityType = Entity.mapToType(entities);
    checkIfReadOnly(entitiesByEntityType.keySet());

    List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String updateQuery = null;
    synchronized (connection) {
      try {
        if (optimisticLockingEnabled) {
          performOptimisticLocking(entitiesByEntityType);
        }

        List<ColumnProperty<?>> statementProperties = new ArrayList<>();
        List<Entity> updatedEntities = new ArrayList<>(entities.size());
        for (Map.Entry<EntityType, List<Entity>> entityTypeEntities : entitiesByEntityType.entrySet()) {
          EntityDefinition entityDefinition = domainEntities.getDefinition(entityTypeEntities.getKey());
          List<ColumnProperty<?>> updatableProperties = getUpdatableProperties(entityDefinition);

          List<Entity> entitiesToUpdate = entityTypeEntities.getValue();
          for (Entity entity : entitiesToUpdate) {
            populatePropertiesAndValues(entity, updatableProperties, statementProperties, statementValues,
                    columnProperty -> entity.isModified(columnProperty.getAttribute()));
            if (statementProperties.isEmpty()) {
              throw new SQLException("Unable to update entity " + entity.getEntityType() + ", no modified values found");
            }

            Condition condition = condition(entity.getOriginalPrimaryKey());
            updateQuery = updateQuery(entityDefinition.getTableName(), statementProperties, condition.getConditionString(entityDefinition));
            statement = prepareStatement(updateQuery);
            statementProperties.addAll(entityDefinition.getColumnProperties(condition.getAttributes()));
            statementValues.addAll(condition.getValues());
            int updatedRows = executeStatement(statement, updateQuery, statementProperties, statementValues);
            if (updatedRows == 0) {
              throw new UpdateException("Update did not affect any rows, entityType: " + entityTypeEntities.getKey());
            }

            statement.close();
            statementProperties.clear();
            statementValues.clear();
          }
          List<Entity> selected = doSelect(condition(Entity.getPrimaryKeys(entitiesToUpdate)).selectBuilder().build(), 0);//bypass caching
          if (selected.size() != entitiesToUpdate.size()) {
            throw new UpdateException(entitiesToUpdate.size() + " updated rows expected, query returned " +
                    selected.size() + ", entityType: " + entityTypeEntities.getKey());
          }
          updatedEntities.addAll(selected);
        }
        commitIfTransactionIsNotOpen();

        return updatedEntities;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, e), e);
        throw translateSQLException(e);
      }
      catch (RecordModifiedException e) {
        rollbackQuietlyIfTransactionIsNotOpen();//releasing the select for update lock
        LOG.debug(e.getMessage(), e);
        throw e;
      }
      catch (UpdateException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, e), e);
        throw e;
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public int update(UpdateCondition condition) throws DatabaseException {
    if (requireNonNull(condition, CONDITION_PARAM_NAME).getAttributeValues().isEmpty()) {
      throw new IllegalArgumentException("No attribute values provided for update");
    }
    checkIfReadOnly(condition.getEntityType());

    List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String updateQuery = null;
    synchronized (connection) {
      try {
        List<ColumnProperty<?>> statementProperties = new ArrayList<>();
        EntityDefinition entityDefinition = domainEntities.getDefinition(condition.getEntityType());
        for (Map.Entry<Attribute<?>, Object> propertyValue : condition.getAttributeValues().entrySet()) {
          ColumnProperty<Object> columnProperty = entityDefinition.getColumnProperty((Attribute<Object>) propertyValue.getKey());
          if (!columnProperty.isUpdatable()) {
            throw new IllegalArgumentException("Property is not updatable: " + columnProperty.getAttribute());
          }
          statementProperties.add(columnProperty);
          statementValues.add(columnProperty.getAttribute().validateType(propertyValue.getValue()));
        }
        updateQuery = updateQuery(entityDefinition.getTableName(), statementProperties, condition.getConditionString(entityDefinition));
        statement = prepareStatement(updateQuery);
        statementProperties.addAll(entityDefinition.getColumnProperties(condition.getAttributes()));
        statementValues.addAll(condition.getValues());
        int updatedRows = executeStatement(statement, updateQuery, statementProperties, statementValues);
        commitIfTransactionIsNotOpen();

        return updatedRows;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, e), e);
        throw translateSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public int delete(Condition condition) throws DatabaseException {
    checkIfReadOnly(requireNonNull(condition, CONDITION_PARAM_NAME).getEntityType());

    EntityDefinition entityDefinition = domainEntities.getDefinition(condition.getEntityType());
    PreparedStatement statement = null;
    String deleteQuery = null;
    synchronized (connection) {
      try {
        deleteQuery = deleteQuery(entityDefinition.getTableName(), condition.getConditionString(entityDefinition));
        statement = prepareStatement(deleteQuery);
        int deleteCount = executeStatement(statement, deleteQuery,
                entityDefinition.getColumnProperties(condition.getAttributes()), condition.getValues());
        commitIfTransactionIsNotOpen();

        return deleteCount;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(deleteQuery, condition.getValues(), e), e);
        throw translateSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public void delete(Key key) throws DatabaseException {
    delete(singletonList(requireNonNull(key, "key")));
  }

  @Override
  public void delete(Collection<Key> keys) throws DatabaseException {
    if (requireNonNull(keys, "keys").isEmpty()) {
      return;
    }
    Map<EntityType, List<Key>> keysByEntityType = Entity.mapKeysToType(keys);
    checkIfReadOnly(keysByEntityType.keySet());

    PreparedStatement statement = null;
    Condition condition = null;
    String deleteQuery = null;
    synchronized (connection) {
      try {
        int deleteCount = 0;
        for (Map.Entry<EntityType, List<Key>> entityTypeKeys : keysByEntityType.entrySet()) {
          EntityDefinition entityDefinition = domainEntities.getDefinition(entityTypeKeys.getKey());
          condition = condition(entityTypeKeys.getValue());
          deleteQuery = deleteQuery(entityDefinition.getTableName(), condition.getConditionString(entityDefinition));
          statement = prepareStatement(deleteQuery);
          deleteCount += executeStatement(statement, deleteQuery,
                  entityDefinition.getColumnProperties(condition.getAttributes()), condition.getValues());
          statement.close();
        }
        if (keys.size() != deleteCount) {
          throw new DeleteException(keys.size() + " deleted rows expected, deleted " + deleteCount);
        }
        commitIfTransactionIsNotOpen();
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(deleteQuery, condition == null ? emptyList() : condition.getValues(), e), e);
        throw translateSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public <T> Entity selectSingle(Attribute<T> attribute, T value) throws DatabaseException {
    if (attribute instanceof ForeignKey) {
      return selectSingle(where((ForeignKey) attribute).equalTo((Entity) value));
    }

    return selectSingle(where(attribute).equalTo(value));
  }

  @Override
  public Entity select(Key key) throws DatabaseException {
    return selectSingle(condition(key));
  }

  @Override
  public Entity selectSingle(Condition condition) throws DatabaseException {
    List<Entity> entities = select(condition);
    if (entities.isEmpty()) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (entities.size() > 1) {
      throw new MultipleRecordsFoundException(MESSAGES.getString("multiple_records_found"));
    }

    return entities.get(0);
  }

  @Override
  public List<Entity> select(Collection<Key> keys) throws DatabaseException {
    if (requireNonNull(keys, "keys").isEmpty()) {
      return emptyList();
    }

    synchronized (connection) {
      try {
        List<Entity> result = new ArrayList<>();
        for (List<Key> entityTypeKeys : Entity.mapKeysToType(keys).values()) {
          result.addAll(doSelect(condition(entityTypeKeys).selectBuilder().build()));
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
  public <T> List<Entity> select(Attribute<T> attribute, T value) throws DatabaseException {
    if (attribute instanceof ForeignKey) {
      return select(where((ForeignKey) attribute).equalTo((Entity) value));
    }

    return select(where(attribute).equalTo(value));
  }

  @Override
  public <T> List<Entity> select(Attribute<T> attribute, Collection<T> values) throws DatabaseException {
    requireNonNull(values, "values");
    if (attribute instanceof ForeignKey) {
      return select(where((ForeignKey) attribute).equalTo((Collection<Entity>) values));
    }

    return select(where(attribute).equalTo(values));
  }

  @Override
  public List<Entity> select(Condition condition) throws DatabaseException {
    SelectCondition selectCondition = requireNonNull(condition, CONDITION_PARAM_NAME).selectBuilder().build();
    synchronized (connection) {
      try {
        List<Entity> result = doSelect(selectCondition);
        if (!selectCondition.isForUpdate()) {
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
  public <T> List<T> select(Attribute<T> attribute) throws DatabaseException {
    return select(attribute, (Condition) null);
  }

  @Override
  public <T> List<T> select(Attribute<T> attribute, Condition condition) throws DatabaseException {
    EntityDefinition entityDefinition = domainEntities.getDefinition(requireNonNull(attribute, "attribute").getEntityType());
    if (entityDefinition.getSelectQuery() != null) {
      throw new UnsupportedOperationException("select is not implemented for entities with custom select queries");
    }
    Condition combinedCondition = where(attribute).isNotNull();
    if (condition != null) {
      condition.getAttributes().forEach(conditionAttribute -> validateAttribute(attribute.getEntityType(), conditionAttribute));
      combinedCondition = combinedCondition.and(condition);
    }
    ColumnProperty<T> propertyToSelect = entityDefinition.getColumnProperty(attribute);
    String columnExpression = propertyToSelect.getColumnExpression();
    String selectQuery = selectQueries.builder(entityDefinition)
            .columns("distinct " + columnExpression)
            .where(combinedCondition)
            .orderBy(columnExpression)
            .build();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    synchronized (connection) {
      try {
        statement = prepareStatement(selectQuery);
        resultSet = executeStatement(statement, selectQuery, combinedCondition, entityDefinition);
        List<T> result = propertyToSelect.getResultPacker().pack(resultSet);
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(selectQuery, asList(attribute, combinedCondition), e), e);
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
  public int rowCount(Condition condition) throws DatabaseException {
    EntityDefinition entityDefinition = domainEntities.getDefinition(requireNonNull(condition, CONDITION_PARAM_NAME).getEntityType());
    String selectQuery = selectQueries.builder(entityDefinition)
            .columns("count(*)")
            .subquery(selectQueries.builder(entityDefinition)
                    .selectCondition(condition.selectBuilder()
                            .selectAttributes(entityDefinition.getPrimaryKeyAttributes())
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
        LOG.error(createLogMessage(selectQuery, condition.getValues(), e), e);
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
  public Map<EntityType, Collection<Entity>> selectDependencies(Collection<? extends Entity> entities) throws DatabaseException {
    if (requireNonNull(entities, ENTITIES_PARAM_NAME).isEmpty()) {
      return emptyMap();
    }

    Map<EntityType, Collection<Entity>> dependencyMap = new HashMap<>();
    Collection<ForeignKeyProperty> foreignKeyReferences = getForeignKeyReferences(entities.iterator().next().getEntityType());
    for (ForeignKeyProperty foreignKeyReference : foreignKeyReferences) {
      if (!foreignKeyReference.isSoftReference()) {
        List<Entity> dependencies = select(where(foreignKeyReference.getAttribute()).equalTo(entities));
        if (!dependencies.isEmpty()) {
          dependencyMap.put(foreignKeyReference.getEntityType(), dependencies);
        }
      }
    }

    return dependencyMap;
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(FunctionType<C, T, R> functionType) throws DatabaseException {
    return executeFunction(functionType, null);
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(FunctionType<C, T, R> functionType, T argument) throws DatabaseException {
    requireNonNull(functionType, "functionType");
    DatabaseException exception = null;
    try {
      logAccess("executeFunction: " + functionType, argument);
      synchronized (connection) {
        return functionType.execute((C) this, domain.getFunction(functionType), argument);
      }
    }
    catch (DatabaseException e) {
      exception = e;
      LOG.error(createLogMessage(functionType.getName(), argument instanceof List ? (List<?>) argument : singletonList(argument), e), e);
      throw e;
    }
    finally {
      logExit("executeFunction: " + functionType, exception);
    }
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(ProcedureType<C, T> procedureType) throws DatabaseException {
    executeProcedure(procedureType, null);
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(ProcedureType<C, T> procedureType, T argument) throws DatabaseException {
    requireNonNull(procedureType, "procedureType");
    DatabaseException exception = null;
    try {
      logAccess("executeProcedure: " + procedureType, argument);
      synchronized (connection) {
        procedureType.execute((C) this, domain.getProcedure(procedureType), argument);
      }
    }
    catch (DatabaseException e) {
      exception = e;
      LOG.error(createLogMessage(procedureType.getName(), argument instanceof List ? (List<?>) argument : singletonList(argument), e), e);
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
        logAccess("fillReport: " + reportType, reportParameters);
        R result = reportType.fillReport(connection.getConnection(), domain.getReport(reportType), reportParameters);
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(null, singletonList(reportType), e), e);
        throw new ReportException(translateSQLException(e));
      }
      catch (ReportException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(null, singletonList(reportType), e), e);
        throw e;
      }
      finally {
        logExit("fillReport: " + reportType, exception);
      }
    }
  }

  @Override
  public void writeBlob(Key primaryKey, Attribute<byte[]> blobAttribute, byte[] blobData) throws DatabaseException {
    requireNonNull(blobData, "blobData");
    EntityDefinition entityDefinition = domainEntities.getDefinition(requireNonNull(primaryKey, "primaryKey").getEntityType());
    checkIfReadOnly(entityDefinition.getEntityType());
    ColumnProperty<byte[]> blobProperty = entityDefinition.getColumnProperty(blobAttribute);
    Condition condition = condition(primaryKey);
    String updateQuery = updateQuery(entityDefinition.getTableName(), singletonList(blobProperty), condition.getConditionString(entityDefinition));
    List<Object> statementValues = new ArrayList<>();
    statementValues.add(null);//the blob value, binary stream set explicitly later
    statementValues.addAll(condition.getValues());
    List<ColumnProperty<?>> statementProperties = new ArrayList<>();
    statementProperties.add(blobProperty);
    statementProperties.addAll(entityDefinition.getColumnProperties(condition.getAttributes()));
    synchronized (connection) {
      Exception exception = null;
      PreparedStatement statement = null;
      try {
        logAccess("writeBlob", updateQuery);
        statement = prepareStatement(updateQuery);
        setParameterValues(statement, statementProperties, statementValues);
        statement.setBinaryStream(1, new ByteArrayInputStream(blobData));//no need to close ByteArrayInputStream
        if (statement.executeUpdate() > 1) {
          throw new UpdateException("Blob write updated more than one row, key: " + primaryKey);
        }
        commitIfTransactionIsNotOpen();
      }
      catch (SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, exception), e);
        throw translateSQLException(e);
      }
      catch (UpdateException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, e), e);
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
  public byte[] readBlob(Key primaryKey, Attribute<byte[]> blobAttribute) throws DatabaseException {
    EntityDefinition entityDefinition = domainEntities.getDefinition(requireNonNull(primaryKey, "primaryKey").getEntityType());
    ColumnProperty<byte[]> blobProperty = entityDefinition.getColumnProperty(blobAttribute);
    PreparedStatement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    Condition condition = condition(primaryKey);
    String selectQuery = selectQueries.builder(entityDefinition)
            .columns(blobProperty.getColumnExpression())
            .where(condition)
            .build();
    synchronized (connection) {
      try {
        logAccess("readBlob", selectQuery);
        statement = prepareStatement(selectQuery);
        setParameterValues(statement, entityDefinition.getColumnProperties(condition.getAttributes()), condition.getValues());

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
        LOG.error(createLogMessage(selectQuery, condition.getValues(), exception), e);
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
  public DatabaseConnection getDatabaseConnection() {
    return connection;
  }

  @Override
  public ResultIterator<Entity> iterator(Condition condition) throws DatabaseException {
    synchronized (connection) {
      try {
        return entityIterator(condition);
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
  public LocalEntityConnection setOptimisticLockingEnabled(boolean optimisticLockingEnabled) {
    this.optimisticLockingEnabled = optimisticLockingEnabled;
    return this;
  }

  @Override
  public boolean isLimitForeignKeyFetchDepth() {
    return limitForeignKeyFetchDepth;
  }

  @Override
  public LocalEntityConnection setLimitFetchDepth(boolean limitFetchDepth) {
    this.limitForeignKeyFetchDepth = limitFetchDepth;
    return this;
  }

  @Override
  public int getDefaultQueryTimeout() {
    return defaultQueryTimeout;
  }

  @Override
  public LocalEntityConnection setDefaultQueryTimeout(int defaultQueryTimeout) {
    this.defaultQueryTimeout = defaultQueryTimeout;
    return this;
  }

  @Override
  public Domain getDomain() {
    return domain;
  }

  /**
   * Selects the given entities for update (if that is supported by the underlying dbms)
   * and checks if they have been modified by comparing the property values to the current values in the database.
   * Note that this does not include BLOB properties or properties that are readOnly.
   * The calling method is responsible for releasing the select for update lock.
   * @param entitiesByEntityType the entities to check, mapped to entityType
   * @throws SQLException in case of exception
   * @throws RecordModifiedException in case an entity has been modified, if an entity has been deleted,
   * the {@code modifiedRow} provided by the exception is null
   */
  private void performOptimisticLocking(Map<EntityType, List<Entity>> entitiesByEntityType) throws SQLException, RecordModifiedException {
    for (Map.Entry<EntityType, List<Entity>> entitiesByEntityTypeEntry : entitiesByEntityType.entrySet()) {
      Collection<Key> originalKeys = Entity.getOriginalPrimaryKeys(entitiesByEntityTypeEntry.getValue());
      SelectCondition selectForUpdateCondition = condition(originalKeys).selectBuilder()
              .selectAttributes(getPrimaryKeyAndWritableColumnAttributes(entitiesByEntityTypeEntry.getKey()))
              .forUpdate()
              .build();
      List<Entity> currentEntities = doSelect(selectForUpdateCondition);
      EntityDefinition definition = domainEntities.getDefinition(entitiesByEntityTypeEntry.getKey());
      Map<Key, Entity> currentEntitiesByKey = Entity.mapToPrimaryKey(currentEntities);
      for (Entity entity : entitiesByEntityTypeEntry.getValue()) {
        Entity current = currentEntitiesByKey.get(entity.getOriginalPrimaryKey());
        if (current == null) {
          Entity original = entity.copy();
          original.revertAll();

          throw new RecordModifiedException(entity, null, MESSAGES.getString(RECORD_MODIFIED)
                  + ", " + original + " " + MESSAGES.getString("has_been_deleted"));
        }
        Collection<Attribute<?>> modified = Entity.getModifiedColumnAttributes(definition, entity, current);
        if (!modified.isEmpty()) {
          throw new RecordModifiedException(entity, current, createModifiedExceptionMessage(entity, current, modified));
        }
      }
    }
  }

  private List<Entity> doSelect(SelectCondition condition) throws SQLException {
    List<Entity> result = getCachedResult(condition);
    if (result != null) {
      LOG.debug("Returning cached result: " + condition.getEntityType());
      return result;
    }

    return cacheResult(condition, doSelect(condition, 0));
  }

  private List<Entity> doSelect(SelectCondition condition, int currentForeignKeyFetchDepth) throws SQLException {
    List<Entity> result;
    try (ResultIterator<Entity> iterator = entityIterator(condition)) {
      result = packResult(iterator);
    }
    if (!condition.isForUpdate() && !result.isEmpty()) {
      setForeignKeys(result, condition, currentForeignKeyFetchDepth);
    }

    return result;
  }

  /**
   * Selects the entities referenced by the given entities via foreign keys and sets those
   * as their respective foreign key values. This is done recursively for the entities referenced
   * by the foreign keys as well, until we reach the condition fetch depth limit.
   * @param entities the entities for which to set the foreign key entity values
   * @param condition the condition
   * @param currentForeignKeyFetchDepth the current foreign key fetch depth
   * @throws SQLException in case of a database exception
   * @see #setLimitFetchDepth(boolean)
   * @see SelectCondition#fetchDepth(int)
   */
  private void setForeignKeys(List<Entity> entities, SelectCondition condition,
                              int currentForeignKeyFetchDepth) throws SQLException {
    List<ForeignKeyProperty> foreignKeyProperties =
            getForeignKeyPropertiesToSet(entities.get(0).getEntityType(), condition.getSelectAttributes());
    for (int i = 0; i < foreignKeyProperties.size(); i++) {
      ForeignKeyProperty foreignKeyProperty = foreignKeyProperties.get(i);
      ForeignKey foreignKey = foreignKeyProperty.getAttribute();
      int conditionFetchDepthLimit = condition.getFetchDepth(foreignKey)
              .orElse(foreignKeyProperty.getFetchDepth());
      if (isWithinFetchDepthLimit(currentForeignKeyFetchDepth, conditionFetchDepthLimit)
              && containsReferenceAttributes(entities.get(0), foreignKey.getReferences())) {
        try {
          logAccess("setForeignKeys", foreignKeyProperty);
          List<Key> referencedKeys = new ArrayList<>(Entity.getReferencedKeys(entities, foreignKey));
          if (referencedKeys.isEmpty()) {
            for (int j = 0; j < entities.size(); j++) {
              entities.get(j).put(foreignKey, null);
            }
          }
          else {
            SelectCondition referencedEntitiesCondition = condition(referencedKeys)
                    .selectBuilder()
                    .fetchDepth(conditionFetchDepthLimit)
                    .selectAttributes(getAttributesToSelect(foreignKeyProperty, referencedKeys.get(0).getAttributes()))
                    .build();
            List<Entity> referencedEntities = doSelect(referencedEntitiesCondition,
                    currentForeignKeyFetchDepth + 1);
            Map<Key, Entity> referencedEntitiesMappedByKey = Entity.mapToPrimaryKey(referencedEntities);
            for (int j = 0; j < entities.size(); j++) {
              Entity entity = entities.get(j);
              Key referencedKey = entity.getReferencedKey(foreignKey);
              entity.put(foreignKey, getReferencedEntity(referencedKey, referencedEntitiesMappedByKey));
            }
          }
        }
        finally {
          logExit("setForeignKeys");
        }
      }
    }
  }

  private List<ForeignKeyProperty> getForeignKeyPropertiesToSet(EntityType entityType,
                                                                Collection<Attribute<?>> conditionSelectAttributes) {
    if (conditionSelectAttributes.isEmpty()) {
      return domainEntities.getDefinition(entityType).getForeignKeyProperties();
    }

    Set<Attribute<?>> selectAttributes = new HashSet<>(conditionSelectAttributes);

    return domainEntities.getDefinition(entityType).getForeignKeyProperties().stream()
            .filter(foreignKeyProperty -> selectAttributes.contains(foreignKeyProperty.getAttribute()))
            .collect(toList());
  }

  private boolean isWithinFetchDepthLimit(int currentForeignKeyFetchDepth, int conditionFetchDepthLimit) {
    return !limitForeignKeyFetchDepth || conditionFetchDepthLimit == -1 || currentForeignKeyFetchDepth < conditionFetchDepthLimit;
  }

  private ResultIterator<Entity> entityIterator(Condition condition) throws SQLException {
    SelectCondition selectCondition = requireNonNull(condition, CONDITION_PARAM_NAME).selectBuilder().build();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectQuery = null;
    EntityDefinition entityDefinition = domainEntities.getDefinition(selectCondition.getEntityType());
    SelectQueries.Builder selectQueryBuilder = selectQueries.builder(entityDefinition)
            .selectCondition(selectCondition);
    try {
      selectQuery = selectQueryBuilder.build();
      statement = prepareStatement(selectQuery, false, selectCondition.getQueryTimeout());
      resultSet = executeStatement(statement, selectQuery, selectCondition, entityDefinition);

      return new EntityResultIterator(statement, resultSet,
              new EntityResultPacker(entityDefinition, selectQueryBuilder.getSelectedProperties()));
    }
    catch (SQLException e) {
      closeSilently(resultSet);
      closeSilently(statement);
      LOG.error(createLogMessage(selectQuery, selectCondition.getValues(), e), e);
      throw e;
    }
  }

  private int executeStatement(PreparedStatement statement, String query,
                               List<ColumnProperty<?>> statementProperties,
                               List<?> statementValues) throws SQLException {
    SQLException exception = null;
    try {
      logAccess(EXECUTE_STATEMENT, statementValues);
      setParameterValues(statement, statementProperties, statementValues);

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
        LOG.debug(createLogMessage(query, statementValues, exception));
      }
    }
  }

  private ResultSet executeStatement(PreparedStatement statement, String query,
                                     Condition condition, EntityDefinition entityDefinition) throws SQLException {
    SQLException exception = null;
    List<?> statementValues = condition.getValues();
    try {
      logAccess(EXECUTE_STATEMENT, statementValues);
      setParameterValues(statement, entityDefinition.getColumnProperties(condition.getAttributes()), statementValues);

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
        LOG.debug(createLogMessage(query, condition.getValues(), exception));
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
      logAccess("prepareStatement", query);
      PreparedStatement statement;
      if (returnGeneratedKeys) {
        statement = connection.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      }
      else {
        statement = connection.getConnection().prepareStatement(query);
      }
      statement.setQueryTimeout(queryTimeout);

      return statement;
    }
    finally {
      logExit("prepareStatement");
    }
  }

  /**
   * @param entityType the entityType
   * @return all foreign keys in the domain referencing entities of type {@code entityType}
   */
  private Collection<ForeignKeyProperty> getForeignKeyReferences(EntityType entityType) {
    return foreignKeyReferenceCache.computeIfAbsent(entityType, this::initializeForeignKeyReferences);
  }

  private List<ForeignKeyProperty> initializeForeignKeyReferences(EntityType entityType) {
    return domainEntities.getDefinitions().stream()
            .flatMap(entityDefinition -> entityDefinition.getForeignKeyProperties().stream())
            .filter(foreignKeyProperty -> foreignKeyProperty.getReferencedEntityType().equals(entityType))
            .collect(toList());
  }

  private List<Entity> packResult(ResultIterator<Entity> iterator) throws SQLException {
    SQLException packingException = null;
    List<Entity> result = new ArrayList<>();
    try {
      logAccess("packResult");
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

  private List<ColumnProperty<?>> getInsertableProperties(EntityDefinition entityDefinition,
                                                          boolean includePrimaryKeyProperties) {
    return insertablePropertiesCache.computeIfAbsent(entityDefinition.getEntityType(), entityType ->
            entityDefinition.getWritableColumnProperties(includePrimaryKeyProperties, true));
  }

  private List<ColumnProperty<?>> getUpdatableProperties(EntityDefinition entityDefinition) {
    return updatablePropertiesCache.computeIfAbsent(entityDefinition.getEntityType(), entityType ->
            entityDefinition.getWritableColumnProperties(true, false));
  }

  private Attribute<?>[] getPrimaryKeyAndWritableColumnAttributes(EntityType entityType) {
    return primaryKeyAndWritableColumnPropertiesCache.computeIfAbsent(entityType, e -> {
      EntityDefinition entityDefinition = domainEntities.getDefinition(entityType);
      List<ColumnProperty<?>> writableAndPrimaryKeyProperties =
              new ArrayList<>(entityDefinition.getWritableColumnProperties(true, true));
      entityDefinition.getPrimaryKeyProperties().forEach(primaryKeyProperty -> {
        if (!writableAndPrimaryKeyProperties.contains(primaryKeyProperty)) {
          writableAndPrimaryKeyProperties.add(primaryKeyProperty);
        }
      });

      return writableAndPrimaryKeyProperties.stream()
              .map(ColumnProperty::getAttribute).toArray(Attribute<?>[]::new);
    });
  }

  private DatabaseException translateSQLException(SQLException exception) {
    Database database = connection.getDatabase();
    if (database.isUniqueConstraintException(exception)) {
      return new UniqueConstraintException(exception, database.getErrorMessage(exception));
    }
    else if (database.isReferentialIntegrityException(exception)) {
      return new ReferentialIntegrityException(exception, database.getErrorMessage(exception));
    }
    else if (database.isTimeoutException(exception)) {
      return new QueryTimeoutException(exception, database.getErrorMessage(exception));
    }

    return new DatabaseException(exception, database.getErrorMessage(exception));
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
      methodLogger.logExit(method, exception, exitMessage);
    }
  }

  private void logAccess(String method) {
    MethodLogger methodLogger = connection.getMethodLogger();
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method);
    }
  }

  private void logAccess(String method, Object argument) {
    MethodLogger methodLogger = connection.getMethodLogger();
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method, argument);
    }
  }

  private String createLogMessage(String sqlStatement, List<?> values, Exception exception) {
    StringBuilder logMessage = new StringBuilder(getUser().toString()).append("\n");
    logMessage.append(sqlStatement == null ? "no sql statement" : sqlStatement).append(", ").append(values);
    if (exception != null) {
      logMessage.append("\n").append(" [Exception: ").append(exception.getMessage()).append("]");
    }

    return logMessage.toString();
  }

  private void countQuery(String query) {
    connection.getDatabase().countQuery(query);
  }

  private void checkIfReadOnly(List<? extends Entity> entities) throws DatabaseException {
    for (int i = 0; i < entities.size(); i++) {
      checkIfReadOnly(entities.get(i).getEntityType());
    }
  }

  private void checkIfReadOnly(Set<EntityType> entityTypes) throws DatabaseException {
    for (EntityType entityType : entityTypes) {
      checkIfReadOnly(entityType);
    }
  }

  private void checkIfReadOnly(EntityType entityType) throws DatabaseException {
    if (domainEntities.getDefinition(entityType).isReadOnly()) {
      throw new DatabaseException("Entities of type: " + entityType + " are read only");
    }
  }

  private List<Entity> getCachedResult(SelectCondition condition) {
    if (queryCacheEnabled && !condition.isForUpdate()) {
      return queryCache.get(condition);
    }

    return null;
  }

  private List<Entity> cacheResult(SelectCondition condition, List<Entity> result) {
    if (queryCacheEnabled && !condition.isForUpdate()) {
      LOG.debug("Caching result: " + condition.getEntityType());
      queryCache.put(condition, result);
    }

    return result;
  }

  private static Entity getReferencedEntity(Key referencedKey, Map<Key, Entity> entitiesMappedByKey) {
    if (referencedKey == null) {
      return null;
    }
    Entity referencedEntity = entitiesMappedByKey.get(referencedKey);
    if (referencedEntity == null) {
      //if the referenced entity is not found (it's been deleted or has been filtered out of an underlying view for example),
      //we create an empty entity wrapping the key since that's the best we can do under the circumstances
      referencedEntity = Entity.entity(referencedKey);
    }

    return referencedEntity;
  }

  private static void setParameterValues(PreparedStatement statement, List<ColumnProperty<?>> statementProperties,
                                         List<?> statementValues) throws SQLException {
    if (statementValues.isEmpty()) {
      return;
    }
    if (statementProperties.size() != statementValues.size()) {
      throw new SQLException("Parameter property value count mismatch: " +
              "expected: " + statementValues.size() + ", got: " + statementProperties.size());
    }

    for (int i = 0; i < statementProperties.size(); i++) {
      setParameterValue(statement, (ColumnProperty<Object>) statementProperties.get(i), statementValues.get(i), i + 1);
    }
  }

  private static void setParameterValue(PreparedStatement statement, ColumnProperty<Object> property,
                                        Object value, int parameterIndex) throws SQLException {
    Object columnValue = property.toColumnValue(value, statement);
    try {
      if (columnValue == null) {
        statement.setNull(parameterIndex, property.getColumnType());
      }
      else {
        statement.setObject(parameterIndex, columnValue, property.getColumnType());
      }
    }
    catch (SQLException e) {
      LOG.error("Unable to set parameter: " + property + ", value: " + value + ", value class: " + (value == null ? "null" : value.getClass()), e);
      throw e;
    }
  }

  /**
   * Populates the given lists with applicable properties and values.
   * @param entity the Entity instance
   * @param entityProperties the column properties the entity type is based on
   * @param statementProperties the list to populate with the properties to use in the statement
   * @param statementValues the list to populate with the values to be used in the statement
   * @param includeIf the Predicate to apply when checking to see if the property should be included
   */
  private static void populatePropertiesAndValues(Entity entity,
                                                  List<ColumnProperty<?>> entityProperties,
                                                  List<ColumnProperty<?>> statementProperties,
                                                  List<Object> statementValues,
                                                  Predicate<ColumnProperty<?>> includeIf) {
    for (int i = 0; i < entityProperties.size(); i++) {
      ColumnProperty<?> property = entityProperties.get(i);
      if (includeIf.test(property)) {
        statementProperties.add(property);
        statementValues.add(entity.get(property.getAttribute()));
      }
    }
  }

  private static String createModifiedExceptionMessage(Entity entity, Entity modified,
                                                       Collection<Attribute<?>> modifiedAttributes) {
    return modifiedAttributes.stream()
            .map(attribute -> " \n" + attribute + ": " + entity.getOriginal(attribute) + " -> " + modified.get(attribute))
            .collect(joining("", MESSAGES.getString(RECORD_MODIFIED) + ", " + entity.getEntityType(), ""));
  }

  private static boolean containsReferenceAttributes(Entity entity, List<ForeignKey.Reference<?>> references) {
    for (int i = 0; i < references.size(); i++) {
      if (!entity.contains(references.get(i).getAttribute())) {
        return false;
      }
    }

    return true;
  }

  private static Collection<Attribute<?>> getAttributesToSelect(ForeignKeyProperty foreignKeyProperty,
                                                                List<Attribute<?>> referencedAttributes) {
    if (foreignKeyProperty.getSelectAttributes().isEmpty()) {
      return emptyList();
    }

    Set<Attribute<?>> selectAttributes = new HashSet<>(foreignKeyProperty.getSelectAttributes());
    selectAttributes.addAll(referencedAttributes);

    return selectAttributes;
  }

  private static void validateAttribute(EntityType entityType, Attribute<?> conditionAttribute) {
    if (!conditionAttribute.getEntityType().equals(entityType)) {
      throw new IllegalArgumentException("Condition attribute entity type " + entityType + " required, got " + conditionAttribute.getEntityType());
    }
  }
}