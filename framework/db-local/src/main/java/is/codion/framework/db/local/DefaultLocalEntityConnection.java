/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.MultipleRecordsFoundException;
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
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

  private static final ResultPacker<Blob> BLOB_RESULT_PACKER = resultSet -> resultSet.getBlob(1);
  private static final ResultPacker<Integer> INTEGER_RESULT_PACKER = resultSet -> resultSet.getInt(1);

  private final Domain domain;
  private final Entities domainEntities;
  private final DatabaseConnection connection;
  private final Map<EntityType<?>, List<ColumnProperty<?>>> insertablePropertiesCache = new HashMap<>();
  private final Map<EntityType<?>, List<ColumnProperty<?>>> updatablePropertiesCache = new HashMap<>();
  private final Map<EntityType<?>, List<ForeignKeyProperty>> foreignKeyReferenceCache = new HashMap<>();
  private final Map<EntityType<?>, Attribute<?>[]> primaryKeyAndWritableColumnPropertiesCache = new HashMap<>();
  private final Map<EntityType<?>, String> allColumnsClauseCache = new HashMap<>();

  private boolean optimisticLockingEnabled = true;
  private boolean limitForeignKeyFetchDepth = true;

  /**
   * Constructs a new LocalEntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
   */
  DefaultLocalEntityConnection(final Domain domain, final Database database, final User user) throws DatabaseException {
    this.domain = requireNonNull(domain, "domain");
    this.domainEntities = domain.getEntities();
    this.connection = databaseConnection(database, user);
    this.domain.configureConnection(this.connection);
  }

  /**
   * Constructs a new LocalEntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param connection the Connection object to base this EntityConnection on, it is assumed to be in a valid state
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required but could not be created
   * @see Database#supportsIsValid()
   */
  DefaultLocalEntityConnection(final Domain domain, final Database database, final Connection connection) throws DatabaseException {
    this.domain = requireNonNull(domain, "domain");
    this.domainEntities = domain.getEntities();
    this.connection = databaseConnection(database, connection);
    this.domain.configureConnection(this.connection);
  }

  @Override
  public LocalEntityConnection setMethodLogger(final MethodLogger methodLogger) {
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
  public Key insert(final Entity entity) throws DatabaseException {
    return insert(singletonList(requireNonNull(entity, "entity"))).get(0);
  }

  @Override
  public List<Key> insert(final List<? extends Entity> entities) throws DatabaseException {
    if (requireNonNull(entities, ENTITIES_PARAM_NAME).isEmpty()) {
      return emptyList();
    }
    checkIfReadOnly(entities);

    final List<Key> insertedKeys = new ArrayList<>(entities.size());
    final List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String insertQuery = null;
    synchronized (connection) {
      try {
        final List<ColumnProperty<?>> statementProperties = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
          final Entity entity = entities.get(i);
          final EntityDefinition entityDefinition = domainEntities.getDefinition(entity.getEntityType());
          final List<ColumnProperty<?>> primaryKeyProperties = entityDefinition.getPrimaryKeyProperties();
          final KeyGenerator keyGenerator = entityDefinition.getKeyGenerator();
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
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(insertQuery, statementValues, e), e);
        throw translateInsertUpdateSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public Entity update(final Entity entity) throws DatabaseException {
    return update(singletonList(requireNonNull(entity, "entity"))).get(0);
  }

  @Override
  public List<Entity> update(final List<? extends Entity> entities) throws DatabaseException {
    if (requireNonNull(entities, ENTITIES_PARAM_NAME).isEmpty()) {
      return emptyList();
    }
    final Map<EntityType<?>, List<Entity>> entitiesByEntityType = Entity.mapToType(entities);
    checkIfReadOnly(entitiesByEntityType.keySet());

    final List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String updateQuery = null;
    synchronized (connection) {
      try {
        if (optimisticLockingEnabled) {
          performOptimisticLocking(entitiesByEntityType);
        }

        final List<ColumnProperty<?>> statementProperties = new ArrayList<>();
        final List<Entity> updatedEntities = new ArrayList<>(entities.size());
        for (final Map.Entry<EntityType<?>, List<Entity>> entityTypeEntities : entitiesByEntityType.entrySet()) {
          final EntityDefinition entityDefinition = domainEntities.getDefinition(entityTypeEntities.getKey());
          final List<ColumnProperty<?>> updatableProperties = getUpdatableProperties(entityDefinition);

          final List<Entity> entitiesToUpdate = entityTypeEntities.getValue();
          for (final Entity entity : entitiesToUpdate) {
            populatePropertiesAndValues(entity, updatableProperties, statementProperties, statementValues,
                    columnProperty -> entity.isModified(columnProperty.getAttribute()));
            if (statementProperties.isEmpty()) {
              throw new SQLException("Unable to update entity " + entity.getEntityType() + ", no modified values found");
            }

            final Condition condition = condition(entity.getOriginalPrimaryKey());
            updateQuery = updateQuery(entityDefinition.getTableName(), statementProperties, condition.getWhereClause(entityDefinition));
            statement = prepareStatement(updateQuery);
            statementProperties.addAll(entityDefinition.getColumnProperties(condition.getAttributes()));
            statementValues.addAll(condition.getValues());
            final int updatedRows = executeStatement(statement, updateQuery, statementProperties, statementValues);
            if (updatedRows == 0) {
              throw new UpdateException("Update did not affect any rows, entityType: " + entityTypeEntities.getKey());
            }

            statement.close();
            statementProperties.clear();
            statementValues.clear();
          }
          final List<Entity> selected = doSelect(condition(Entity.getPrimaryKeys(entitiesToUpdate)).asSelectCondition());
          if (selected.size() != entitiesToUpdate.size()) {
            throw new UpdateException(entitiesToUpdate.size() + " updated rows expected, query returned " +
                    selected.size() + ", entityType: " + entityTypeEntities.getKey());
          }
          updatedEntities.addAll(selected);
        }
        commitIfTransactionIsNotOpen();

        return updatedEntities;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, e), e);
        throw translateInsertUpdateSQLException(e);
      }
      catch (final RecordModifiedException e) {
        rollbackQuietlyIfTransactionIsNotOpen();//releasing the select for update lock
        LOG.debug(e.getMessage(), e);
        throw e;
      }
      catch (final UpdateException e) {
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
  public int update(final UpdateCondition condition) throws DatabaseException {
    if (requireNonNull(condition, CONDITION_PARAM_NAME).getAttributeValues().isEmpty()) {
      throw new IllegalArgumentException("No attribute values provided for update");
    }
    checkIfReadOnly(condition.getEntityType());

    final List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String updateQuery = null;
    synchronized (connection) {
      try {
        final List<ColumnProperty<?>> statementProperties = new ArrayList<>();
        final EntityDefinition entityDefinition = domainEntities.getDefinition(condition.getEntityType());
        for (final Map.Entry<Attribute<?>, Object> propertyValue : condition.getAttributeValues().entrySet()) {
          final ColumnProperty<Object> columnProperty = entityDefinition.getColumnProperty((Attribute<Object>) propertyValue.getKey());
          if (!columnProperty.isUpdatable()) {
            throw new IllegalArgumentException("Property is not updatable: " + columnProperty.getAttribute());
          }
          statementProperties.add(columnProperty);
          statementValues.add(columnProperty.getAttribute().validateType(propertyValue.getValue()));
        }
        updateQuery = updateQuery(entityDefinition.getTableName(), statementProperties, condition.getWhereClause(entityDefinition));
        statement = prepareStatement(updateQuery);
        statementProperties.addAll(entityDefinition.getColumnProperties(condition.getAttributes()));
        statementValues.addAll(condition.getValues());
        final int updatedRows = executeStatement(statement, updateQuery, statementProperties, statementValues);
        commitIfTransactionIsNotOpen();

        return updatedRows;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, e), e);
        throw translateInsertUpdateSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public int delete(final Condition condition) throws DatabaseException {
    checkIfReadOnly(requireNonNull(condition, CONDITION_PARAM_NAME).getEntityType());

    final EntityDefinition entityDefinition = domainEntities.getDefinition(condition.getEntityType());
    PreparedStatement statement = null;
    String deleteQuery = null;
    synchronized (connection) {
      try {
        deleteQuery = deleteQuery(entityDefinition.getTableName(), condition.getWhereClause(entityDefinition));
        statement = prepareStatement(deleteQuery);
        final int deleteCount = executeStatement(statement, deleteQuery,
                entityDefinition.getColumnProperties(condition.getAttributes()), condition.getValues());
        commitIfTransactionIsNotOpen();

        return deleteCount;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(deleteQuery, condition.getValues(), e), e);
        throw translateDeleteSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public boolean delete(final Key key) throws DatabaseException {
    return delete(singletonList(requireNonNull(key, "key"))) == 1;
  }

  @Override
  public int delete(final List<Key> keys) throws DatabaseException {
    if (requireNonNull(keys, "keys").isEmpty()) {
      return 0;
    }
    final Map<EntityType<?>, List<Key>> keysByEntityType = Entity.mapKeysToType(keys);
    checkIfReadOnly(keysByEntityType.keySet());

    PreparedStatement statement = null;
    Condition condition = null;
    String deleteQuery = null;
    synchronized (connection) {
      try {
        int deleteCount = 0;
        for (final Map.Entry<EntityType<?>, List<Key>> entityTypeKeys : keysByEntityType.entrySet()) {
          final EntityDefinition entityDefinition = domainEntities.getDefinition(entityTypeKeys.getKey());
          condition = condition(entityTypeKeys.getValue());
          deleteQuery = deleteQuery(entityDefinition.getTableName(), condition.getWhereClause(entityDefinition));
          statement = prepareStatement(deleteQuery);
          deleteCount += executeStatement(statement, deleteQuery,
                  entityDefinition.getColumnProperties(condition.getAttributes()), condition.getValues());
          statement.close();
        }
        commitIfTransactionIsNotOpen();

        return deleteCount;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(deleteQuery, condition == null ? emptyList() : condition.getValues(), e), e);
        throw translateDeleteSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public <T> Entity selectSingle(final Attribute<T> attribute, final T value) throws DatabaseException {
    if (attribute instanceof ForeignKey) {
      return selectSingle(where((ForeignKey) attribute).equalTo((Entity) value));
    }

    return selectSingle(where(attribute).equalTo(value));
  }

  @Override
  public Entity selectSingle(final Key key) throws DatabaseException {
    return selectSingle(condition(key));
  }

  @Override
  public Entity selectSingle(final Condition condition) throws DatabaseException {
    final List<Entity> entities = select(condition);
    if (entities.isEmpty()) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (entities.size() > 1) {
      throw new MultipleRecordsFoundException(MESSAGES.getString("multiple_records_found"));
    }

    return entities.get(0);
  }

  @Override
  public List<Entity> select(final List<Key> keys) throws DatabaseException {
    if (requireNonNull(keys, "keys").isEmpty()) {
      return emptyList();
    }

    synchronized (connection) {
      try {
        final List<Entity> result = new ArrayList<>();
        for (final List<Key> entityTypeKeys : Entity.mapKeysToType(keys).values()) {
          result.addAll(doSelect(condition(entityTypeKeys).asSelectCondition()));
        }
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
    }
  }

  @Override
  public <T> List<Entity> select(final Attribute<T> attribute, final T value) throws DatabaseException {
    if (attribute instanceof ForeignKey) {
      return select(where((ForeignKey) attribute).equalTo((Entity) value));
    }

    return select(where(attribute).equalTo(value));
  }

  @Override
  public <T> List<Entity> select(final Attribute<T> attribute, final Collection<T> values) throws DatabaseException {
    requireNonNull(values, "values");
    if (attribute instanceof ForeignKey) {
      return select(where((ForeignKey) attribute).equalTo((Collection<Entity>) values));
    }

    return select(where(attribute).equalTo(values));
  }

  @Override
  public List<Entity> select(final Condition condition) throws DatabaseException {
    final SelectCondition selectCondition = requireNonNull(condition, CONDITION_PARAM_NAME).asSelectCondition();
    synchronized (connection) {
      try {
        final List<Entity> result = doSelect(selectCondition);
        if (!selectCondition.isForUpdate()) {
          commitIfTransactionIsNotOpen();
        }

        return result;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
    }
  }

  @Override
  public <T> List<T> select(final Attribute<T> attribute) throws DatabaseException {
    return select(attribute, (Condition) null);
  }

  @Override
  public <T> List<T> select(final Attribute<T> attribute, final Condition condition) throws DatabaseException {
    final EntityDefinition entityDefinition = domainEntities.getDefinition(requireNonNull(attribute, "attribute").getEntityType());
    if (entityDefinition.getSelectQuery() != null) {
      throw new UnsupportedOperationException("select is not implemented for entities with custom select queries");
    }
    Condition combinedCondition = where(attribute).isNotNull();
    if (condition != null) {
      condition.getAttributes().forEach(conditionAttribute -> validateAttribute(attribute.getEntityType(), conditionAttribute));
      combinedCondition = combinedCondition.and(condition);
    }
    final ColumnProperty<T> propertyToSelect = entityDefinition.getColumnProperty(attribute);
    final String columnName = propertyToSelect.getColumnName();
    final String selectQuery = selectQuery(entityDefinition.getSelectTableName(),
            "distinct " + columnName, combinedCondition.getWhereClause(entityDefinition), columnName);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    synchronized (connection) {
      try {
        statement = prepareStatement(selectQuery);
        resultSet = executeStatement(statement, selectQuery, combinedCondition, entityDefinition);
        final List<T> result = propertyToSelect.<T>getResultPacker().pack(resultSet);
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(selectQuery, asList(attribute, combinedCondition), e), e);
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        closeSilently(resultSet);
        closeSilently(statement);
        countQuery(selectQuery);
      }
    }
  }

  @Override
  public int rowCount(final Condition condition) throws DatabaseException {
    final EntityDefinition entityDefinition = domainEntities.getDefinition(requireNonNull(condition, CONDITION_PARAM_NAME).getEntityType());
    final Database database = connection.getDatabase();
    final String subquery = selectQuery(columnsClause(entityDefinition.getPrimaryKeyProperties()), condition, entityDefinition, database);
    final String subqueryAlias = database.subqueryRequiresAlias() ? " as row_count" : "";
    final String selectQuery = selectQuery("(" + subquery + ")" + subqueryAlias, "count(*)");
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    synchronized (connection) {
      try {
        statement = prepareStatement(selectQuery);
        resultSet = executeStatement(statement, selectQuery, condition, entityDefinition);
        final List<Integer> result = INTEGER_RESULT_PACKER.pack(resultSet);
        commitIfTransactionIsNotOpen();
        if (result.isEmpty()) {
          throw new SQLException("Row count query returned no value");
        }

        return result.get(0);
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(selectQuery, condition.getValues(), e), e);
        throw new DatabaseException(e, database.getErrorMessage(e));
      }
      finally {
        closeSilently(resultSet);
        closeSilently(statement);
        countQuery(selectQuery);
      }
    }
  }

  @Override
  public Map<EntityType<?>, Collection<Entity>> selectDependencies(final Collection<? extends Entity> entities) throws DatabaseException {
    if (requireNonNull(entities, ENTITIES_PARAM_NAME).isEmpty()) {
      return emptyMap();
    }

    final Map<EntityType<?>, Collection<Entity>> dependencyMap = new HashMap<>();
    final Collection<ForeignKeyProperty> foreignKeyReferences = getForeignKeyReferences(entities.iterator().next().getEntityType());
    for (final ForeignKeyProperty foreignKeyReference : foreignKeyReferences) {
      if (!foreignKeyReference.isSoftReference()) {
        final List<Entity> dependencies = select(where(foreignKeyReference.getAttribute()).equalTo(entities));
        if (!dependencies.isEmpty()) {
          dependencyMap.put(foreignKeyReference.getEntityType(), dependencies);
        }
      }
    }

    return dependencyMap;
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(final FunctionType<C, T, R> functionType) throws DatabaseException {
    return executeFunction(functionType, emptyList());
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(final FunctionType<C, T, R> functionType, final List<T> arguments) throws DatabaseException {
    requireNonNull(functionType, "functionType");
    requireNonNull(arguments, "arguments");
    DatabaseException exception = null;
    try {
      logAccess("executeFunction: " + functionType, arguments);
      synchronized (connection) {
        return functionType.execute((C) this, domain.getFunction(functionType), arguments);
      }
    }
    catch (final DatabaseException e) {
      exception = e;
      LOG.error(createLogMessage(functionType.getName(), arguments, e), e);
      throw e;
    }
    finally {
      logExit("executeFunction: " + functionType, exception);
    }
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(final ProcedureType<C, T> procedureType) throws DatabaseException {
    executeProcedure(procedureType, emptyList());
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(final ProcedureType<C, T> procedureType, final List<T> arguments) throws DatabaseException {
    requireNonNull(procedureType, "procedureType");
    requireNonNull(arguments, "arguments");
    DatabaseException exception = null;
    try {
      logAccess("executeProcedure: " + procedureType, arguments);
      synchronized (connection) {
        procedureType.execute((C) this, domain.getProcedure(procedureType), arguments);
      }
    }
    catch (final DatabaseException e) {
      exception = e;
      LOG.error(createLogMessage(procedureType.getName(), arguments, e), e);
      throw e;
    }
    finally {
      logExit("executeProcedure: " + procedureType, exception);
    }
  }

  @Override
  public <T, R, P> R fillReport(final ReportType<T, R, P> reportType, final P reportParameters) throws ReportException {
    requireNonNull(reportType, "report");
    Exception exception = null;
    synchronized (connection) {
      try {
        logAccess("fillReport: " + reportType, reportParameters);
        final R result = reportType.fillReport(connection.getConnection(), domain.getReport(reportType), reportParameters);
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (final SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(null, singletonList(reportType), e), e);
        throw new ReportException(e);
      }
      catch (final ReportException e) {
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
  public void writeBlob(final Key primaryKey, final Attribute<byte[]> blobAttribute, final byte[] blobData) throws DatabaseException {
    requireNonNull(blobData, "blobData");
    final EntityDefinition entityDefinition = domainEntities.getDefinition(requireNonNull(primaryKey, "primaryKey").getEntityType());
    checkIfReadOnly(entityDefinition.getEntityType());
    final ColumnProperty<byte[]> blobProperty = entityDefinition.getColumnProperty(blobAttribute);
    final Condition condition = condition(primaryKey);
    final String updateQuery = "update " + entityDefinition.getTableName() + " set " + blobProperty.getColumnName() + " = ?" +
            WHERE_SPACE_PREFIX_POSTFIX + condition.getWhereClause(entityDefinition);
    final List<Object> statementValues = new ArrayList<>();
    statementValues.add(null);//the blob value, binary stream set explicitly later
    statementValues.addAll(condition.getValues());
    final List<ColumnProperty<?>> statementProperties = new ArrayList<>();
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
      catch (final SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(updateQuery, statementValues, exception), e);
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      catch (final UpdateException e) {
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
  public byte[] readBlob(final Key primaryKey, final Attribute<byte[]> blobAttribute) throws DatabaseException {
    final EntityDefinition entityDefinition = domainEntities.getDefinition(requireNonNull(primaryKey, "primaryKey").getEntityType());
    final ColumnProperty<byte[]> blobProperty = entityDefinition.getColumnProperty(blobAttribute);
    PreparedStatement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    final Condition condition = condition(primaryKey);
    final String selectQuery = "select " + blobProperty.getColumnName() + " from " +
            entityDefinition.getTableName() + WHERE_SPACE_PREFIX_POSTFIX + condition.getWhereClause(entityDefinition);
    synchronized (connection) {
      try {
        logAccess("readBlob", selectQuery);
        statement = prepareStatement(selectQuery);
        setParameterValues(statement, entityDefinition.getColumnProperties(condition.getAttributes()), condition.getValues());

        resultSet = statement.executeQuery();
        final List<Blob> result = BLOB_RESULT_PACKER.pack(resultSet, 1);
        if (result.isEmpty()) {
          return null;
        }
        final Blob blob = result.get(0);
        final byte[] byteResult = blob.getBytes(1, (int) blob.length());
        commitIfTransactionIsNotOpen();

        return byteResult;
      }
      catch (final SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(selectQuery, condition.getValues(), exception), e);
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
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
  public ResultIterator<Entity> iterator(final Condition condition) throws DatabaseException {
    synchronized (connection) {
      try {
        return entityIterator(condition);
      }
      catch (final SQLException e) {
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
    }
  }

  @Override
  public boolean isOptimisticLockingEnabled() {
    return optimisticLockingEnabled;
  }

  @Override
  public LocalEntityConnection setOptimisticLockingEnabled(final boolean optimisticLockingEnabled) {
    this.optimisticLockingEnabled = optimisticLockingEnabled;
    return this;
  }

  @Override
  public boolean isLimitForeignKeyFetchDepth() {
    return limitForeignKeyFetchDepth;
  }

  @Override
  public LocalEntityConnection setLimitFetchDepth(final boolean limitFetchDepth) {
    this.limitForeignKeyFetchDepth = limitFetchDepth;
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
  private void performOptimisticLocking(final Map<EntityType<?>, List<Entity>> entitiesByEntityType) throws SQLException, RecordModifiedException {
    for (final Map.Entry<EntityType<?>, List<Entity>> entitiesByEntityTypeEntry : entitiesByEntityType.entrySet()) {
      final List<Key> originalKeys = Entity.getOriginalPrimaryKeys(entitiesByEntityTypeEntry.getValue());
      final SelectCondition selectForUpdateCondition = condition(originalKeys).asSelectCondition()
              .attributes(getPrimaryKeyAndWritableColumnAttributes(entitiesByEntityTypeEntry.getKey()))
              .forUpdate();
      final List<Entity> currentEntities = doSelect(selectForUpdateCondition);
      final EntityDefinition definition = domainEntities.getDefinition(entitiesByEntityTypeEntry.getKey());
      final Map<Key, Entity> currentEntitiesByKey = Entity.mapToPrimaryKey(currentEntities);
      for (final Entity entity : entitiesByEntityTypeEntry.getValue()) {
        final Entity current = currentEntitiesByKey.get(entity.getOriginalPrimaryKey());
        if (current == null) {
          final Entity original = entity.copy();
          original.revertAll();

          throw new RecordModifiedException(entity, null, MESSAGES.getString(RECORD_MODIFIED)
                  + ", " + original + " " + MESSAGES.getString("has_been_deleted"));
        }
        final List<Attribute<?>> modified = Entity.getModifiedColumnAttributes(definition, entity, current);
        if (!modified.isEmpty()) {
          throw new RecordModifiedException(entity, current, createModifiedExceptionMessage(entity, current, modified));
        }
      }
    }
  }

  private List<Entity> doSelect(final SelectCondition condition) throws SQLException {
    return doSelect(condition, 0);
  }

  private List<Entity> doSelect(final SelectCondition condition, final int currentForeignKeyFetchDepth) throws SQLException {
    final List<Entity> result;
    try (final ResultIterator<Entity> iterator = entityIterator(condition)) {
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
  private void setForeignKeys(final List<Entity> entities, final SelectCondition condition,
                              final int currentForeignKeyFetchDepth) throws SQLException {
    final List<ForeignKeyProperty> foreignKeyProperties =
            domainEntities.getDefinition(entities.get(0).getEntityType()).getForeignKeyProperties();
    for (int i = 0; i < foreignKeyProperties.size(); i++) {
      final ForeignKeyProperty foreignKeyProperty = foreignKeyProperties.get(i);
      final ForeignKey foreignKey = foreignKeyProperty.getAttribute();
      Integer conditionFetchDepthLimit = condition.getFetchDepth(foreignKey);
      if (conditionFetchDepthLimit == null) {//use the default one
        conditionFetchDepthLimit = foreignKeyProperty.getFetchDepth();
      }
      if (!limitForeignKeyFetchDepth || conditionFetchDepthLimit.intValue() == -1 ||
              currentForeignKeyFetchDepth < conditionFetchDepthLimit.intValue()) {
        try {
          logAccess("setForeignKeys", foreignKeyProperty);
          final List<Key> referencedKeys = new ArrayList<>(Entity.getReferencedKeys(entities, foreignKey));
          if (referencedKeys.isEmpty()) {
            for (int j = 0; j < entities.size(); j++) {
              entities.get(j).put(foreignKey, null);
            }
          }
          else {
            final SelectCondition referencedEntitiesCondition = condition(referencedKeys)
                    .asSelectCondition().fetchDepth(conditionFetchDepthLimit);
            final List<Entity> referencedEntities = doSelect(referencedEntitiesCondition,
                    currentForeignKeyFetchDepth + 1);
            final Map<Key, Entity> referencedEntitiesMappedByKey = Entity.mapToPrimaryKey(referencedEntities);
            for (int j = 0; j < entities.size(); j++) {
              final Entity entity = entities.get(j);
              final Key referencedKey = entity.getReferencedKey(foreignKey);
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

  private Entity getReferencedEntity(final Key referencedKey, final Map<Key, Entity> entitiesMappedByKey) {
    if (referencedKey == null) {
      return null;
    }
    Entity referencedEntity = entitiesMappedByKey.get(referencedKey);
    if (referencedEntity == null) {
      //if the referenced entity is not found (it's been deleted or has been filtered out of an underlying view for example),
      //we create an empty entity wrapping the key since that's the best we can do under the circumstances
      referencedEntity = domainEntities.entity(referencedKey);
    }

    return referencedEntity;
  }

  private ResultIterator<Entity> entityIterator(final Condition condition) throws SQLException {
    final SelectCondition selectCondition = requireNonNull(condition, CONDITION_PARAM_NAME).asSelectCondition();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectQuery = null;
    final EntityDefinition entityDefinition = domainEntities.getDefinition(selectCondition.getEntityType());
    final List<ColumnProperty<?>> propertiesToSelect = selectCondition.getSelectAttributes().isEmpty() ?
            entityDefinition.getSelectableColumnProperties() :
            entityDefinition.getSelectableColumnProperties(selectCondition.getSelectAttributes());
    try {
      selectQuery = selectQuery(getColumnsClause(entityDefinition.getEntityType(),
              selectCondition.getSelectAttributes(), propertiesToSelect), selectCondition,
              entityDefinition, connection.getDatabase());
      statement = prepareStatement(selectQuery);
      resultSet = executeStatement(statement, selectQuery, selectCondition, entityDefinition);

      return new EntityResultIterator(statement, resultSet,
              new EntityResultPacker(entityDefinition, propertiesToSelect), selectCondition.getFetchCount());
    }
    catch (final SQLException e) {
      closeSilently(resultSet);
      closeSilently(statement);
      LOG.error(createLogMessage(selectQuery, selectCondition.getValues(), e), e);
      throw e;
    }
  }

  private int executeStatement(final PreparedStatement statement, final String query,
                               final List<ColumnProperty<?>> statementProperties,
                               final List<?> statementValues) throws SQLException {
    SQLException exception = null;
    try {
      logAccess(EXECUTE_STATEMENT, statementValues);
      setParameterValues(statement, statementProperties, statementValues);

      return statement.executeUpdate();
    }
    catch (final SQLException e) {
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

  private ResultSet executeStatement(final PreparedStatement statement, final String query,
                                     final Condition condition, final EntityDefinition entityDefinition) throws SQLException {
    SQLException exception = null;
    final List<?> statementValues = condition.getValues();
    try {
      logAccess(EXECUTE_STATEMENT, statementValues);
      setParameterValues(statement, entityDefinition.getColumnProperties(condition.getAttributes()), statementValues);

      return statement.executeQuery();
    }
    catch (final SQLException e) {
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

  private PreparedStatement prepareStatement(final String query) throws SQLException {
    return prepareStatement(query, false);
  }

  private PreparedStatement prepareStatement(final String query, final boolean returnGeneratedKeys) throws SQLException {
    try {
      logAccess("prepareStatement", query);
      if (returnGeneratedKeys) {
        return connection.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      }

      return connection.getConnection().prepareStatement(query);
    }
    finally {
      logExit("prepareStatement");
    }
  }

  /**
   * @param entityType the entityType
   * @return all foreign keys in the domain referencing entities of type {@code entityType}
   */
  private Collection<ForeignKeyProperty> getForeignKeyReferences(final EntityType<?> entityType) {
    return foreignKeyReferenceCache.computeIfAbsent(entityType, e -> {
      final List<ForeignKeyProperty> foreignKeyReferences = new ArrayList<>();
      for (final EntityDefinition entityDefinition : domainEntities.getDefinitions()) {
        for (final ForeignKeyProperty foreignKeyProperty : entityDefinition.getForeignKeyProperties()) {
          if (foreignKeyProperty.getReferencedEntityType().equals(entityType)) {
            foreignKeyReferences.add(foreignKeyProperty);
          }
        }
      }

      return foreignKeyReferences;
    });
  }

  private List<Entity> packResult(final ResultIterator<Entity> iterator) throws SQLException {
    SQLException packingException = null;
    final List<Entity> result = new ArrayList<>();
    try {
      logAccess("packResult");
      while (iterator.hasNext()) {
        result.add(iterator.next());
      }

      return result;
    }
    catch (final SQLException e) {
      packingException = e;
      throw e;
    }
    finally {
      logExit("packResult", packingException, "row count: " + result.size());
    }
  }

  private List<ColumnProperty<?>> getInsertableProperties(final EntityDefinition entityDefinition,
                                                          final boolean includePrimaryKeyProperties) {
    return insertablePropertiesCache.computeIfAbsent(entityDefinition.getEntityType(), entityType ->
            entityDefinition.getWritableColumnProperties(includePrimaryKeyProperties, true));
  }

  private List<ColumnProperty<?>> getUpdatableProperties(final EntityDefinition entityDefinition) {
    return updatablePropertiesCache.computeIfAbsent(entityDefinition.getEntityType(), entityType ->
            entityDefinition.getWritableColumnProperties(true, false));
  }

  private Attribute<?>[] getPrimaryKeyAndWritableColumnAttributes(final EntityType<?> entityType) {
    return primaryKeyAndWritableColumnPropertiesCache.computeIfAbsent(entityType, e -> {
      final EntityDefinition entityDefinition = domainEntities.getDefinition(entityType);
      final List<ColumnProperty<?>> writableAndPrimaryKeyProperties =
              new ArrayList<>(entityDefinition.getWritableColumnProperties(true, true));
      entityDefinition.getPrimaryKeyProperties().forEach(primaryKeyProperty -> {
        if (!writableAndPrimaryKeyProperties.contains(primaryKeyProperty)) {
          writableAndPrimaryKeyProperties.add(primaryKeyProperty);
        }
      });

      return writableAndPrimaryKeyProperties.stream().map(ColumnProperty::getAttribute).toArray(Attribute<?>[]::new);
    });
  }

  private String getColumnsClause(final EntityType<?> entityType, final List<Attribute<?>> selectAttributes,
                                  final List<ColumnProperty<?>> propertiesToSelect) {
    if (selectAttributes.isEmpty()) {
      return allColumnsClauseCache.computeIfAbsent(entityType, type -> columnsClause(propertiesToSelect));
    }

    return columnsClause(propertiesToSelect);
  }

  private DatabaseException translateInsertUpdateSQLException(final SQLException exception) {
    final Database database = connection.getDatabase();
    if (database.isUniqueConstraintException(exception)) {
      return new UniqueConstraintException(exception, database.getErrorMessage(exception));
    }
    else if (database.isReferentialIntegrityException(exception)) {
      return new ReferentialIntegrityException(exception, database.getErrorMessage(exception));
    }

    return new DatabaseException(exception, database.getErrorMessage(exception));
  }

  private DatabaseException translateDeleteSQLException(final SQLException exception) {
    final Database database = connection.getDatabase();
    if (database.isReferentialIntegrityException(exception)) {
      return new ReferentialIntegrityException(exception, database.getErrorMessage(exception));
    }

    return new DatabaseException(exception, database.getErrorMessage(exception));
  }

  private void rollbackQuietly() {
    try {
      connection.rollback();
    }
    catch (final SQLException e) {
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

  private void logExit(final String method) {
    logExit(method, null);
  }

  private void logExit(final String method, final Throwable exception) {
    logExit(method, exception, null);
  }

  private void logExit(final String method, final Throwable exception, final String exitMessage) {
    final MethodLogger methodLogger = connection.getMethodLogger();
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logExit(method, exception, exitMessage);
    }
  }

  private void logAccess(final String method) {
    final MethodLogger methodLogger = connection.getMethodLogger();
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method);
    }
  }

  private void logAccess(final String method, final Object argument) {
    final MethodLogger methodLogger = connection.getMethodLogger();
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method, argument);
    }
  }

  private String createLogMessage(final String sqlStatement, final List<?> values, final Exception exception) {
    final StringBuilder logMessage = new StringBuilder(getUser().toString()).append("\n");
    logMessage.append(sqlStatement == null ? "no sql statement" : sqlStatement).append(", ").append(values);
    if (exception != null) {
      logMessage.append("\n").append(" [Exception: ").append(exception.getMessage()).append("]");
    }

    return logMessage.toString();
  }

  private void countQuery(final String query) {
    connection.getDatabase().countQuery(query);
  }

  private void checkIfReadOnly(final List<? extends Entity> entities) throws DatabaseException {
    for (int i = 0; i < entities.size(); i++) {
      checkIfReadOnly(entities.get(i).getEntityType());
    }
  }

  private void checkIfReadOnly(final Set<EntityType<?>> entityTypes) throws DatabaseException {
    for (final EntityType<?> entityType : entityTypes) {
      checkIfReadOnly(entityType);
    }
  }

  private void checkIfReadOnly(final EntityType<?> entityType) throws DatabaseException {
    if (domainEntities.getDefinition(entityType).isReadOnly()) {
      throw new DatabaseException("Entities of type: " + entityType + " are read only");
    }
  }

  private static void setParameterValues(final PreparedStatement statement, final List<ColumnProperty<?>> statementProperties,
                                         final List<?> statementValues) throws SQLException {
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

  private static void setParameterValue(final PreparedStatement statement, final ColumnProperty<Object> property,
                                        final Object value, final int parameterIndex) throws SQLException {
    final Object columnValue = property.toColumnValue(value, statement);
    try {
      if (columnValue == null) {
        statement.setNull(parameterIndex, property.getColumnType());
      }
      else {
        statement.setObject(parameterIndex, columnValue, property.getColumnType());
      }
    }
    catch (final SQLException e) {
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
  private static void populatePropertiesAndValues(final Entity entity,
                                                  final List<ColumnProperty<?>> entityProperties,
                                                  final List<ColumnProperty<?>> statementProperties,
                                                  final List<Object> statementValues,
                                                  final Predicate<ColumnProperty<?>> includeIf) {
    for (int i = 0; i < entityProperties.size(); i++) {
      final ColumnProperty<?> property = entityProperties.get(i);
      if (includeIf.test(property)) {
        statementProperties.add(property);
        statementValues.add(entity.get(property.getAttribute()));
      }
    }
  }

  private static String createModifiedExceptionMessage(final Entity entity, final Entity modified,
                                                       final Collection<Attribute<?>> modifiedAttributes) {
    final StringBuilder builder = new StringBuilder(MESSAGES.getString(RECORD_MODIFIED))
            .append(", ").append(entity.getEntityType());
    for (final Attribute<?> attribute : modifiedAttributes) {
      builder.append(" \n").append(attribute).append(": ").append(entity.getOriginal(attribute))
              .append(" -> ").append(modified.get(attribute));
    }

    return builder.toString();
  }

  private static void validateAttribute(final EntityType<?> entityType, final Attribute<?> conditionAttribute) {
    if (!conditionAttribute.getEntityType().equals(entityType)) {
      throw new IllegalArgumentException("Condition attribute entity type " + entityType + " required, got " + conditionAttribute.getEntityType());
    }
  }
}