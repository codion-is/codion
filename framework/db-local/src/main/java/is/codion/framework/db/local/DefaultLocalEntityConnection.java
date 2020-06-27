/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.MethodLogger;
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
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportType;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.db.result.ResultPacker;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.condition.WhereCondition;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.common.db.Operator.EQUALS;
import static is.codion.common.db.connection.DatabaseConnections.createConnection;
import static is.codion.common.db.database.Database.closeSilently;
import static is.codion.framework.db.condition.Conditions.*;
import static is.codion.framework.db.condition.NullCheck.IS_NOT_NULL;
import static is.codion.framework.db.condition.NullCheck.IS_NULL;
import static is.codion.framework.db.local.Queries.*;
import static is.codion.framework.domain.entity.Entities.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A default LocalEntityConnection implementation
 */
final class DefaultLocalEntityConnection implements LocalEntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalEntityConnection.class.getName());
  private static final String RECORD_MODIFIED_EXCEPTION = "record_modified_exception";

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLocalEntityConnection.class);
  private static final String CONDITION_PARAM_NAME = "condition";
  private static final String ENTITIES_PARAM_NAME = "entities";

  /**
   * A result packer for fetching blobs from a result set containing a single blob column
   */
  private static final ResultPacker<Blob> BLOB_RESULT_PACKER = new BlobPacker();
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
    this.connection = createConnection(database, user);
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
    this.connection = createConnection(database, connection);
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
  public void disconnect() {
    synchronized (connection) {
      connection.disconnect();
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
    requireNonNull(entities, ENTITIES_PARAM_NAME);
    if (entities.isEmpty()) {
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
                  statementProperties, statementValues, columnProperty -> entity.containsKey(columnProperty.getAttribute()));
          if (statementProperties.isEmpty()) {
            throw new SQLException("Unable to insert entity " + entity.getEntityType() + ", no properties to insert");
          }

          final String[] returnColumns = keyGenerator.returnGeneratedKeys() ? getPrimaryKeyColumnNames(entityDefinition) : null;
          insertQuery = insertQuery(entityDefinition.getTableName(), statementProperties);
          statement = prepareStatement(insertQuery, returnColumns);
          executeStatement(statement, insertQuery, statementProperties, statementValues);
          keyGenerator.afterInsert(entity, primaryKeyProperties, connection, statement);

          insertedKeys.add(entity.getKey());

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
    requireNonNull(entities, ENTITIES_PARAM_NAME);
    if (entities.isEmpty()) {
      return emptyList();
    }
    final LinkedHashMap<EntityType<Entity>, List<Entity>> entitiesByEntityType = mapToType((Collection<Entity>) entities);
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
        for (final Map.Entry<EntityType<Entity>, List<Entity>> entityTypeEntities : entitiesByEntityType.entrySet()) {
          final EntityDefinition entityDefinition = domainEntities.getDefinition(entityTypeEntities.getKey());
          final List<ColumnProperty<?>> updatableProperties = getUpdatableProperties(entityDefinition);

          final List<Entity> entitiesToUpdate = entityTypeEntities.getValue();
          for (final Entity entity : entitiesToUpdate) {
            populatePropertiesAndValues(entity, updatableProperties, statementProperties, statementValues,
                    property -> entity.containsKey(property.getAttribute()) && entity.isModified(property.getAttribute()));
            if (statementProperties.isEmpty()) {
              throw new SQLException("Unable to update entity " + entity.getEntityType() + ", no modified values found");
            }

            final WhereCondition updateCondition = whereCondition(condition(entity.getOriginalKey()), entityDefinition);
            updateQuery = updateQuery(entityDefinition.getTableName(), statementProperties, updateCondition.getWhereClause());
            statement = prepareStatement(updateQuery);
            statementProperties.addAll(updateCondition.getColumnProperties());
            statementValues.addAll(updateCondition.getValues());
            final int updatedRows = executeStatement(statement, updateQuery, statementProperties, statementValues);
            if (updatedRows == 0) {
              throw new UpdateException("Update did not affect any rows, entityType: " + entityTypeEntities.getKey());
            }

            statement.close();
            statementProperties.clear();
            statementValues.clear();
          }
          final List<Entity> selected = doSelect(selectCondition(getKeys(entitiesToUpdate)));
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
  public int update(final UpdateCondition updateCondition) throws DatabaseException {
    requireNonNull(updateCondition, "updateCondition");
    if (updateCondition.getAttributeValues().isEmpty()) {
      throw new IllegalArgumentException("No attribute values provided for update");
    }
    checkIfReadOnly(updateCondition.getEntityType());

    final List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String updateQuery = null;
    synchronized (connection) {
      try {
        final List<ColumnProperty<?>> statementProperties = new ArrayList<>();
        final EntityDefinition entityDefinition = domainEntities.getDefinition(updateCondition.getEntityType());
        for (final Map.Entry<Attribute<?>, Object> propertyValue : updateCondition.getAttributeValues().entrySet()) {
          final ColumnProperty<Object> columnProperty = entityDefinition.getColumnProperty((Attribute<Object>) propertyValue.getKey());
          if (!columnProperty.isUpdatable()) {
            throw new IllegalArgumentException("Property is not updatable: " + columnProperty.getAttribute());
          }
          statementProperties.add(columnProperty);
          statementValues.add(columnProperty.getAttribute().validateType(propertyValue.getValue()));
        }
        final WhereCondition whereCondition = whereCondition(updateCondition, entityDefinition);
        updateQuery = updateQuery(entityDefinition.getTableName(), statementProperties, whereCondition.getWhereClause());
        statement = prepareStatement(updateQuery);
        statementProperties.addAll(whereCondition.getColumnProperties());
        statementValues.addAll(whereCondition.getValues());
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
  public int delete(final Condition deleteCondition) throws DatabaseException {
    requireNonNull(deleteCondition, "deleteCondition");
    checkIfReadOnly(deleteCondition.getEntityType());

    final EntityDefinition entityDefinition = domainEntities.getDefinition(deleteCondition.getEntityType());
    final WhereCondition whereCondition = whereCondition(deleteCondition, entityDefinition);
    PreparedStatement statement = null;
    String deleteQuery = null;
    synchronized (connection) {
      try {
        deleteQuery = deleteQuery(entityDefinition.getTableName(), whereCondition.getWhereClause());
        statement = prepareStatement(deleteQuery);
        final int deleteCount = executeStatement(statement, deleteQuery,
                whereCondition.getColumnProperties(), whereCondition.getValues());
        commitIfTransactionIsNotOpen();

        return deleteCount;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(deleteQuery, whereCondition.getValues(), e), e);
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
    requireNonNull(keys, "keys");
    if (keys.isEmpty()) {
      return 0;
    }
    final LinkedHashMap<EntityType<Entity>, List<Key>> keysByEntityType = mapKeysToType(keys);
    checkIfReadOnly(keysByEntityType.keySet());

    PreparedStatement statement = null;
    WhereCondition whereCondition = null;
    String deleteQuery = null;
    synchronized (connection) {
      try {
        int deleteCount = 0;
        for (final Map.Entry<EntityType<Entity>, List<Key>> entityTypeKeys : keysByEntityType.entrySet()) {
          final EntityDefinition entityDefinition = domainEntities.getDefinition(entityTypeKeys.getKey());
          whereCondition = whereCondition(condition(entityTypeKeys.getValue()), entityDefinition);
          deleteQuery = deleteQuery(entityDefinition.getTableName(), whereCondition.getWhereClause());
          statement = prepareStatement(deleteQuery);
          deleteCount += executeStatement(statement, deleteQuery,
                  whereCondition.getColumnProperties(), whereCondition.getValues());
          statement.close();
        }
        commitIfTransactionIsNotOpen();

        return deleteCount;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(deleteQuery, whereCondition == null ? emptyList() : whereCondition.getValues(), e), e);
        throw translateDeleteSQLException(e);
      }
      finally {
        closeSilently(statement);
      }
    }
  }

  @Override
  public <T> Entity selectSingle(final Attribute<T> attribute, final T value) throws DatabaseException {
    return selectSingle(selectCondition(attribute, EQUALS, value));
  }

  @Override
  public Entity selectSingle(final Key key) throws DatabaseException {
    return selectSingle(selectCondition(key));
  }

  @Override
  public Entity selectSingle(final SelectCondition condition) throws DatabaseException {
    final List<Entity> entities = select(condition);
    if (entities.isEmpty()) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (entities.size() > 1) {
      throw new MultipleRecordsFoundException(MESSAGES.getString("many_records_found"));
    }

    return entities.get(0);
  }

  @Override
  public List<Entity> select(final List<Key> keys) throws DatabaseException {
    requireNonNull(keys, "keys");
    if (keys.isEmpty()) {
      return emptyList();
    }

    synchronized (connection) {
      try {
        final List<Entity> result = new ArrayList<>();
        for (final List<Key> entityTypeKeys : mapKeysToType(keys).values()) {
          result.addAll(doSelect(selectCondition(entityTypeKeys)));
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
    return select(value == null ? selectCondition(attribute, IS_NULL) : selectCondition(attribute, EQUALS, value));
  }

  @Override
  public <T> List<Entity> select(final Attribute<T> attribute, final Collection<T> values) throws DatabaseException {
    return select(selectCondition(attribute, EQUALS, values));
  }

  @Override
  public List<Entity> select(final SelectCondition selectCondition) throws DatabaseException {
    requireNonNull(selectCondition, "selectCondition");
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
    requireNonNull(attribute, "attribute");
    final EntityDefinition entityDefinition = domainEntities.getDefinition(attribute.getEntityType());
    if (entityDefinition.getSelectQuery() != null) {
      throw new UnsupportedOperationException("selectValues is not implemented for entities with custom select queries");
    }
    Condition combination = condition(attribute, IS_NOT_NULL);
    if (condition != null) {
      condition.getAttributes().forEach(conditionAttribute -> validateAttribute(attribute.getEntityType(), conditionAttribute));
      combination = combination.and(expand(condition, entityDefinition));
    }
    final WhereCondition combinedCondition = whereCondition(combination, entityDefinition);
    final ColumnProperty<T> propertyToSelect = entityDefinition.getColumnProperty(attribute);
    final String columnName = propertyToSelect.getColumnName();
    final String selectQuery = selectQuery(entityDefinition.getSelectTableName(),
            "distinct " + columnName, combinedCondition.getWhereClause(), columnName);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    synchronized (connection) {
      try {
        statement = prepareStatement(selectQuery);
        resultSet = executeStatement(statement, selectQuery, combinedCondition);
        final List<T> result = propertyToSelect.<T>getResultPacker().pack(resultSet, -1);
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
    requireNonNull(condition, CONDITION_PARAM_NAME);
    final EntityDefinition entityDefinition = domainEntities.getDefinition(condition.getEntityType());
    final WhereCondition whereCondition = whereCondition(condition, entityDefinition);
    final Database database = connection.getDatabase();
    final String subquery = selectQuery(Queries.columnsClause(entityDefinition.getPrimaryKeyProperties()),
            condition, whereCondition, entityDefinition, database);
    final String subqueryAlias = database.subqueryRequiresAlias() ? " as row_count" : "";
    final String selectQuery = selectQuery("(" + subquery + ")" + subqueryAlias, "count(*)");
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    synchronized (connection) {
      try {
        statement = prepareStatement(selectQuery);
        resultSet = executeStatement(statement, selectQuery, whereCondition);
        final List<Integer> result = INTEGER_RESULT_PACKER.pack(resultSet, -1);
        commitIfTransactionIsNotOpen();
        if (result.isEmpty()) {
          throw new SQLException("Row count query returned no value");
        }

        return result.get(0);
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(createLogMessage(selectQuery, whereCondition.getValues(), e), e);
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
  public Map<EntityType<Entity>, Collection<Entity>> selectDependencies(final Collection<? extends Entity> entities) throws DatabaseException {
    requireNonNull(entities, ENTITIES_PARAM_NAME);
    if (entities.isEmpty()) {
      return emptyMap();
    }

    final Map<EntityType<Entity>, Collection<Entity>> dependencyMap = new HashMap<>();
    final Collection<ForeignKeyProperty> foreignKeyReferences = getForeignKeyReferences(
            entities.iterator().next().getEntityType());
    for (final ForeignKeyProperty foreignKeyReference : foreignKeyReferences) {
      if (!foreignKeyReference.isSoftReference()) {
        final List<Entity> dependencies = select(selectCondition(foreignKeyReference.getAttribute(), EQUALS, entities));
        if (!dependencies.isEmpty()) {
          dependencyMap.put((EntityType<Entity>) foreignKeyReference.getEntityType(), dependencies);
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
    DatabaseException exception = null;
    try {
      logAccess("executeFunction: " + functionType, arguments);
      synchronized (connection) {
        return functionType.execute((C) this, domain.getFunction(functionType), arguments);
      }
    }
    catch (final DatabaseException e) {
      exception = e;
      LOG.error(createLogMessage(functionType.getName(), arguments == null ? null : arguments, e), e);
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
    DatabaseException exception = null;
    try {
      logAccess("executeProcedure: " + procedureType, arguments);
      synchronized (connection) {
        procedureType.execute((C) this, domain.getProcedure(procedureType), arguments);
      }
    }
    catch (final DatabaseException e) {
      exception = e;
      LOG.error(createLogMessage(procedureType.getName(), arguments == null ? null : arguments, e), e);
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
    final WhereCondition whereCondition = whereCondition(condition(primaryKey), entityDefinition);
    final String updateQuery = "update " + entityDefinition.getTableName() + " set " + blobProperty.getColumnName() + " = ?" +
            WHERE_SPACE_PREFIX_POSTFIX + whereCondition.getWhereClause();
    final List<Object> statementValues = new ArrayList<>();
    statementValues.add(null);//the blob value, set explicitly later
    statementValues.addAll(whereCondition.getValues());
    final List<ColumnProperty<?>> statementProperties = new ArrayList<>();
    statementProperties.add(blobProperty);
    statementProperties.addAll(whereCondition.getColumnProperties());
    synchronized (connection) {
      SQLException exception = null;
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
    final WhereCondition whereCondition = whereCondition(condition(primaryKey), entityDefinition);
    final String selectQuery = "select " + blobProperty.getColumnName() + " from " +
            entityDefinition.getTableName() + WHERE_SPACE_PREFIX_POSTFIX + whereCondition.getWhereClause();
    synchronized (connection) {
      try {
        logAccess("readBlob", selectQuery);
        statement = prepareStatement(selectQuery);
        setParameterValues(statement, whereCondition.getColumnProperties(), whereCondition.getValues());

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
        LOG.error(createLogMessage(selectQuery, whereCondition.getValues(), exception), e);
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
  public ResultIterator<Entity> iterator(final SelectCondition condition) throws DatabaseException {
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
  public LocalEntityConnection setLimitForeignKeyFetchDepth(final boolean limitForeignKeyFetchDepth) {
    this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
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
  private void performOptimisticLocking(final Map<EntityType<Entity>, List<Entity>> entitiesByEntityType) throws SQLException, RecordModifiedException {
    for (final Map.Entry<EntityType<Entity>, List<Entity>> entitiesByEntityTypeEntry : entitiesByEntityType.entrySet()) {
      final List<Key> originalKeys = getOriginalKeys(entitiesByEntityTypeEntry.getValue());
      final SelectCondition selectForUpdateCondition = selectCondition(originalKeys);
      selectForUpdateCondition.setSelectAttributes(getPrimaryKeyAndWritableColumnAttributes(entitiesByEntityTypeEntry.getKey()));
      selectForUpdateCondition.setForUpdate(true);
      final List<Entity> currentEntities = doSelect(selectForUpdateCondition);
      final EntityDefinition definition = domainEntities.getDefinition(entitiesByEntityTypeEntry.getKey());
      final Map<Key, Entity> currentEntitiesByKey = mapToKey(currentEntities);
      for (final Entity entity : entitiesByEntityTypeEntry.getValue()) {
        final Entity current = currentEntitiesByKey.get(entity.getOriginalKey());
        if (current == null) {
          final Entity original = domainEntities.copyEntity(entity);
          original.revertAll();

          throw new RecordModifiedException(entity, null, MESSAGES.getString(RECORD_MODIFIED_EXCEPTION)
                  + ", " + original + " " + MESSAGES.getString("has_been_deleted"));
        }
        final List<ColumnProperty<?>> modified = getModifiedColumnProperties(definition, entity, current);
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
   * by the foreign keys as well, until the condition fetch depth limit has been reached.
   * @param entities the entities for which to set the foreign key entity values
   * @param condition the condition
   * @param currentForeignKeyFetchDepth the current foreign key fetch depth
   * @throws SQLException in case of a database exception
   * @see #setLimitForeignKeyFetchDepth(boolean)
   * @see SelectCondition#setForeignKeyFetchDepth(int)
   */
  private void setForeignKeys(final List<Entity> entities, final SelectCondition condition,
                              final int currentForeignKeyFetchDepth) throws SQLException {
    final List<ForeignKeyProperty> foreignKeyProperties =
            domainEntities.getDefinition(entities.get(0).getEntityType()).getForeignKeyProperties();
    for (int i = 0; i < foreignKeyProperties.size(); i++) {
      final ForeignKeyProperty foreignKeyProperty = foreignKeyProperties.get(i);
      final Attribute<Entity> foreignKeyAttribute = foreignKeyProperty.getAttribute();
      Integer conditionFetchDepthLimit = condition.getForeignKeyFetchDepth(foreignKeyAttribute);
      if (conditionFetchDepthLimit == null) {//use the default one
        conditionFetchDepthLimit = foreignKeyProperty.getFetchDepth();
      }
      if (!limitForeignKeyFetchDepth || conditionFetchDepthLimit == -1 || currentForeignKeyFetchDepth < conditionFetchDepthLimit) {
        try {
          logAccess("setForeignKeys", foreignKeyProperty);
          final List<Key> referencedKeys = new ArrayList<>(getReferencedKeys(entities, foreignKeyAttribute));
          if (referencedKeys.isEmpty()) {
            for (int j = 0; j < entities.size(); j++) {
              entities.get(j).put(foreignKeyAttribute, null);
            }
          }
          else {
            final SelectCondition referencedEntitiesCondition = selectCondition(referencedKeys);
            referencedEntitiesCondition.setForeignKeyFetchDepth(conditionFetchDepthLimit);
            final List<Entity> referencedEntities = doSelect(referencedEntitiesCondition,
                    currentForeignKeyFetchDepth + 1);
            final Map<Key, Entity> referencedEntitiesMappedByKey = mapToKey(referencedEntities);
            for (int j = 0; j < entities.size(); j++) {
              final Entity entity = entities.get(j);
              final Key referencedKey = entity.getReferencedKey(foreignKeyAttribute);
              entity.put(foreignKeyAttribute, getReferencedEntity(referencedKey, referencedEntitiesMappedByKey));
            }
          }
        }
        finally {
          logExit("setForeignKeys");
        }
      }
    }
  }

  private Entity getReferencedEntity(final Key referencedPrimaryKey, final Map<Key, Entity> entitiesMappedByKey) {
    if (referencedPrimaryKey == null) {
      return null;
    }
    Entity referencedEntity = entitiesMappedByKey.get(referencedPrimaryKey);
    if (referencedEntity == null) {
      //if the referenced entity is not found (it's been deleted or has been filtered out of an underlying view for example),
      //we create an empty entity wrapping the primary key since that's the best we can do under the circumstances
      referencedEntity = domainEntities.entity(referencedPrimaryKey);
    }

    return referencedEntity;
  }

  private ResultIterator<Entity> entityIterator(final SelectCondition selectCondition) throws SQLException {
    requireNonNull(selectCondition, "selectCondition");
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectQuery = null;
    final EntityDefinition entityDefinition = domainEntities.getDefinition(selectCondition.getEntityType());
    final WhereCondition whereCondition = whereCondition(selectCondition, entityDefinition);
    final List<ColumnProperty<?>> propertiesToSelect = selectCondition.getSelectAttributes().isEmpty() ?
            entityDefinition.getSelectableColumnProperties() :
            entityDefinition.getSelectableColumnProperties(selectCondition.getSelectAttributes());
    try {
      selectQuery = selectQuery(columnsClause(entityDefinition.getEntityType(),
              selectCondition.getSelectAttributes(), propertiesToSelect), selectCondition, whereCondition,
              entityDefinition, connection.getDatabase());
      statement = prepareStatement(selectQuery);
      resultSet = executeStatement(statement, selectQuery, whereCondition);

      return new EntityResultIterator(statement, resultSet, new EntityResultPacker(
              entityDefinition, propertiesToSelect), selectCondition.getFetchCount());
    }
    catch (final SQLException e) {
      closeSilently(resultSet);
      closeSilently(statement);
      LOG.error(createLogMessage(selectQuery, whereCondition.getValues(), e), e);
      throw e;
    }
  }

  private int executeStatement(final PreparedStatement statement, final String query,
                               final List<ColumnProperty<?>> statementProperties,
                               final List<?> statementValues) throws SQLException {
    SQLException exception = null;
    try {
      logAccess("executeStatement", statementValues);
      setParameterValues(statement, statementProperties, statementValues);

      return statement.executeUpdate();
    }
    catch (final SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      logExit("executeStatement", exception);
      countQuery(query);
      if (LOG.isDebugEnabled()) {
        LOG.debug(createLogMessage(query, statementValues, exception));
      }
    }
  }

  private ResultSet executeStatement(final PreparedStatement statement, final String query,
                                     final WhereCondition whereCondition) throws SQLException {
    SQLException exception = null;
    final List<?> statementValues = whereCondition.getValues();
    try {
      logAccess("executeStatement", statementValues);
      setParameterValues(statement, whereCondition.getColumnProperties(), statementValues);

      return statement.executeQuery();
    }
    catch (final SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      logExit("executeStatement", exception);
      countQuery(query);
      if (LOG.isDebugEnabled()) {
        LOG.debug(createLogMessage(query, whereCondition.getValues(), exception));
      }
    }
  }

  private PreparedStatement prepareStatement(final String query) throws SQLException {
    return prepareStatement(query, null);
  }

  private PreparedStatement prepareStatement(final String query, final String[] returnColumns) throws SQLException {
    try {
      logAccess("prepareStatement", query);
      if (returnColumns == null) {
        return connection.getConnection().prepareStatement(query);
      }

      return connection.getConnection().prepareStatement(query, returnColumns);
    }
    finally {
      logExit("prepareStatement");
    }
  }

  private static String[] getPrimaryKeyColumnNames(final EntityDefinition entityDefinition) {
    final List<ColumnProperty<?>> primaryKeyProperties = entityDefinition.getPrimaryKeyProperties();
    final String[] columnNames = new String[primaryKeyProperties.size()];
    for (int i = 0; i < primaryKeyProperties.size(); i++) {
      columnNames[i] = primaryKeyProperties.get(i).getColumnName();
    }

    return columnNames;
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
      logAccess("packResult", null);
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
      final List<Attribute<?>> attributes = new ArrayList<>();
      writableAndPrimaryKeyProperties.forEach(columnProperty -> attributes.add(columnProperty.getAttribute()));

      return attributes.toArray(new Attribute<?>[0]);
    });
  }

  private String columnsClause(final EntityType<?> entityType, final List<Attribute<?>> selectAttributes,
                               final List<ColumnProperty<?>> propertiesToSelect) {
    if (selectAttributes.isEmpty()) {
      return allColumnsClauseCache.computeIfAbsent(entityType, type -> Queries.columnsClause(propertiesToSelect));
    }

    return Queries.columnsClause(propertiesToSelect);
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

  private void checkIfReadOnly(final Set<EntityType<Entity>> entityTypes) throws DatabaseException {
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
    if (nullOrEmpty(statementValues) || statement.getParameterMetaData().getParameterCount() == 0) {
      return;
    }
    if (statementProperties == null || statementProperties.size() != statementValues.size()) {
      throw new SQLException("Parameter property value count mismatch: " + (statementProperties == null ?
              "no properties" : ("expected: " + statementValues.size() + ", got: " + statementProperties.size())));
    }

    for (int i = 0; i < statementProperties.size(); i++) {
      setParameterValue(statement, i + 1, statementValues.get(i), (ColumnProperty<Object>) statementProperties.get(i));
    }
  }

  private static void setParameterValue(final PreparedStatement statement, final int parameterIndex,
                                        final Object value, final ColumnProperty<Object> property) throws SQLException {
    final Object columnValue = property.toColumnValue(value);
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
                                                       final Collection<ColumnProperty<?>> modifiedProperties) {
    final StringBuilder builder = new StringBuilder(MESSAGES.getString(RECORD_MODIFIED_EXCEPTION))
            .append(", ").append(entity.getEntityType());
    for (final ColumnProperty<?> property : modifiedProperties) {
      builder.append(" \n").append(property).append(": ").append(entity.getOriginal(property.getAttribute()))
              .append(" -> ").append(modified.get(property.getAttribute()));
    }

    return builder.toString();
  }

  private static void validateAttribute(final EntityType<?> entityType, final Attribute<?> conditionAttribute) {
    if (!conditionAttribute.getEntityType().equals(entityType)) {
      throw new IllegalArgumentException("Condition attribute entity type " + entityType + " required, got " + conditionAttribute.getEntityType());
    }
  }

  private static final class BlobPacker implements ResultPacker<Blob> {
    @Override
    public Blob fetch(final ResultSet resultSet) throws SQLException {
      return resultSet.getBlob(1);
    }
  }
}