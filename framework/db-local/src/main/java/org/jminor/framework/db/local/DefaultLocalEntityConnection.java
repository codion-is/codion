/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Conjunction;
import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.DatabaseConnections;
import org.jminor.common.db.Databases;
import org.jminor.common.db.ResultIterator;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.db.exception.ReferentialIntegrityException;
import org.jminor.common.db.exception.UniqueConstraintException;
import org.jminor.common.db.exception.UpdateException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * A default LocalEntityConnection implementation
 */
final class DefaultLocalEntityConnection implements LocalEntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalEntityConnection.class.getName(), Locale.getDefault());
  private static final String RECORD_MODIFIED_EXCEPTION = "record_modified_exception";

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLocalEntityConnection.class);
  private static final String CONDITION_PARAM_NAME = "condition";
  private static final String WHERE = "where ";
  private static final String WHERE_SPACE_PREFIX = " " + WHERE;

  /**
   * A result packer for fetching blobs from a result set containing a single blob column
   */
  private static final ResultPacker<Blob> BLOB_RESULT_PACKER = new BlobPacker();

  private final Domain domain;
  private final DatabaseConnection connection;
  private final EntityConditions entityConditions;
  private final Map<String, ResultPacker<Entity>> resultPackers = new HashMap<>();
  private final Map<String, List<Property.ColumnProperty>> insertProperties = new HashMap<>();
  private final Map<String, List<Property.ColumnProperty>> updateProperties = new HashMap<>();

  private boolean optimisticLocking;
  private boolean limitForeignKeyFetchDepth;

  private MethodLogger methodLogger;

  /**
   * Constructs a new LocalEntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @param optimisticLocking if true then optimistic locking is used during updates
   * @param limitForeignKeyFetchDepth if false then there is no limiting of foreign key fetch depth
   * @param validityCheckTimeout specifies the timeout in seconds when validating this connection
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws org.jminor.common.db.exception.AuthenticationException in case of an authentication error
   */
  DefaultLocalEntityConnection(final Domain domain, final Database database, final User user, final boolean optimisticLocking,
                               final boolean limitForeignKeyFetchDepth, final int validityCheckTimeout) throws DatabaseException {
    this.domain = Objects.requireNonNull(domain, "domain");
    this.connection = DatabaseConnections.createConnection(database, user, validityCheckTimeout);
    this.entityConditions = new EntityConditions(domain);
    this.optimisticLocking = optimisticLocking;
    this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
  }

  /**
   * Constructs a new LocalEntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param connection the Connection object to base this EntityConnection on, it is assumed to be in a valid state
   * @param optimisticLocking if true then optimistic locking is used during updates
   * @param limitForeignKeyFetchDepth if false then there is no limiting of foreign key fetch depth
   * @param validityCheckTimeout specifies the timeout in seconds when validating this connection
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required but could not be created
   * @see org.jminor.common.db.Database#supportsIsValid()
   */
  DefaultLocalEntityConnection(final Domain domain, final Database database, final Connection connection, final boolean optimisticLocking,
                               final boolean limitForeignKeyFetchDepth, final int validityCheckTimeout) throws DatabaseException {
    this.domain = Objects.requireNonNull(domain, "domain");
    this.connection = DatabaseConnections.createConnection(database, connection, validityCheckTimeout);
    this.entityConditions = new EntityConditions(domain);
    this.optimisticLocking = optimisticLocking;
    this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
  }

  /** {@inheritDoc} */
  @Override
  public void setMethodLogger(final MethodLogger methodLogger) {
    synchronized (connection) {
      this.methodLogger = methodLogger;
      connection.setMethodLogger(methodLogger);
    }
  }

  /** {@inheritDoc} */
  @Override
  public MethodLogger getMethodLogger() {
    synchronized (connection) {
      return this.methodLogger;
    }
  }

  /** {@inheritDoc} */
  @Override
  public Domain getDomain() {
    return new Domain(domain);
  }

  /** {@inheritDoc} */
  @Override
  public User getUser() {
    return connection.getUser();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConnected() {
    synchronized (connection) {
      return connection.isConnected();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    synchronized (connection) {
      connection.disconnect();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void beginTransaction() {
    synchronized (connection) {
      connection.beginTransaction();
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isTransactionOpen() {
    synchronized (connection) {
      return connection.isTransactionOpen();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void rollbackTransaction() {
    synchronized (connection) {
      connection.rollbackTransaction();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void commitTransaction() {
    synchronized (connection) {
      connection.commitTransaction();
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException {
    if (Util.nullOrEmpty(entities)) {
      return Collections.emptyList();
    }
    checkReadOnly(entities);

    final List<Entity.Key> insertedKeys = new ArrayList<>(entities.size());
    final List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String insertSQL = null;
    synchronized (connection) {
      try {
        final List<Property.ColumnProperty> statementProperties = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
          final Entity entity = entities.get(i);
          final String entityId = entity.getEntityId();
          final Entity.KeyGenerator keyGenerator = domain.getKeyGenerator(entityId);
          keyGenerator.beforeInsert(entity, connection);

          final List<Property.ColumnProperty> insertColumnProperties = insertProperties.computeIfAbsent(entityId,
                  e -> domain.getWritableColumnProperties(entityId, !keyGenerator.getType().isAutoIncrement(), true));
          populateStatementPropertiesAndValues(true, entity, insertColumnProperties, statementProperties, statementValues);

          insertSQL = createInsertSQL(domain.getTableName(entityId), statementProperties);
          statement = prepareStatement(insertSQL);
          executePreparedUpdate(statement, insertSQL, statementProperties, statementValues);
          keyGenerator.afterInsert(entity, connection, statement);

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
        LOG.error(Databases.createLogMessage(getUser(), insertSQL, statementValues, e, null));
        throw translateInsertUpdateSQLException(e);
      }
      finally {
        Databases.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> update(final List<Entity> entities) throws DatabaseException {
    if (Util.nullOrEmpty(entities)) {
      return entities;
    }
    checkReadOnly(entities);

    final List<Object> statementValues = new ArrayList<>();
    PreparedStatement statement = null;
    String updateSQL = null;
    synchronized (connection) {
      try {
        final Map<String, List<Entity>> mappedEntities = Entities.mapToEntityId(entities);
        if (optimisticLocking) {
          lockAndCheckForUpdate(mappedEntities);
        }

        final List<Property.ColumnProperty> statementProperties = new ArrayList<>();
        final List<Entity> updatedEntities = new ArrayList<>(entities.size());
        for (final Map.Entry<String, List<Entity>> mappedEntitiesMapEntry : mappedEntities.entrySet()) {
          final String entityId = mappedEntitiesMapEntry.getKey();
          final Collection<Entity> toUpdate = mappedEntitiesMapEntry.getValue();
          final String tableName = domain.getTableName(entityId);
          final List<Property.ColumnProperty> updateColumnProperties = updateProperties.computeIfAbsent(entityId,
                  e -> domain.getWritableColumnProperties(entityId, true, false));

          for (final Entity entity : toUpdate) {
            populateStatementPropertiesAndValues(false, entity, updateColumnProperties, statementProperties, statementValues);

            final EntityCondition condition = entityConditions.condition(entity.getOriginalKey());
            updateSQL = createUpdateSQL(tableName, statementProperties, condition);
            statementProperties.addAll(condition.getColumns());
            statementValues.addAll(condition.getValues());
            statement = prepareStatement(updateSQL);
            final int updated = executePreparedUpdate(statement, updateSQL, statementProperties, statementValues);
            if (updated == 0) {
              throw new UpdateException("Update did not affect any rows");
            }

            statement.close();
            statementProperties.clear();
            statementValues.clear();
          }
          final List<Entity> selected = doSelectMany(entityConditions.selectCondition(Entities.getKeys(toUpdate)), 0);
          if (selected.size() != toUpdate.size()) {
            throw new UpdateException(toUpdate.size() + " updated rows expected, query returned " + selected.size() + " entityId: " + entityId);
          }
          updatedEntities.addAll(selected);
        }

        commitIfTransactionIsNotOpen();

        return updatedEntities;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(Databases.createLogMessage(getUser(), updateSQL, statementValues, e, null));
        throw translateInsertUpdateSQLException(e);
      }
      catch (final RecordModifiedException e) {
        rollbackQuietlyIfTransactionIsNotOpen();//releasing the select for update lock
        LOG.debug(e.getMessage(), e);
        throw e;
      }
      catch (final UpdateException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(Databases.createLogMessage(getUser(), updateSQL, statementValues, e, null));
        throw e;
      }
      finally {
        Databases.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final EntityCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition, CONDITION_PARAM_NAME);
    checkReadOnly(condition.getEntityId());

    PreparedStatement statement = null;
    String deleteSQL = null;
    synchronized (connection) {
      try {
        deleteSQL = createDeleteSQL(domain.getTableName(condition.getEntityId()), condition);
        statement = prepareStatement(deleteSQL);
        executePreparedUpdate(statement, deleteSQL, condition.getColumns(), condition.getValues());
        commitIfTransactionIsNotOpen();
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(Databases.createLogMessage(getUser(), deleteSQL, condition.getValues(), e, null));
        throw translateDeleteSQLException(e);
      }
      finally {
        Databases.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final List<Entity.Key> entityKeys) throws DatabaseException {
    if (Util.nullOrEmpty(entityKeys)) {
      return;
    }

    PreparedStatement statement = null;
    String deleteSQL = null;
    synchronized (connection) {
      try {
        final Map<String, List<Entity.Key>> mappedKeys = Entities.mapKeysToEntityId(entityKeys);
        for (final String entityId : mappedKeys.keySet()) {
          checkReadOnly(entityId);
        }
        final List<Entity.Key> conditionKeys = new ArrayList<>();
        for (final Map.Entry<String, List<Entity.Key>> mappedKeysEntry : mappedKeys.entrySet()) {
          conditionKeys.addAll(mappedKeysEntry.getValue());
          final EntityCondition condition = entityConditions.condition(conditionKeys);
          deleteSQL = createDeleteSQL(domain.getTableName(condition.getEntityId()), condition);
          statement = prepareStatement(deleteSQL);
          executePreparedUpdate(statement, deleteSQL, condition.getColumns(), condition.getValues());
          statement.close();
          conditionKeys.clear();
        }
        commitIfTransactionIsNotOpen();
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(Databases.createLogMessage(getUser(), deleteSQL, entityKeys, e, null));
        throw translateDeleteSQLException(e);
      }
      finally {
        Databases.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final String entityId, final String propertyId, final Object value) throws DatabaseException {
    return selectSingle(entityConditions.selectCondition(entityId, propertyId, Condition.Type.LIKE, value));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final Entity.Key key) throws DatabaseException {
    return selectSingle(entityConditions.selectCondition(key));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final EntitySelectCondition condition) throws DatabaseException {
    final List<Entity> entities = selectMany(condition);
    if (entities.isEmpty()) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (entities.size() > 1) {
      throw new DatabaseException(MESSAGES.getString("many_records_found"));
    }

    return entities.get(0);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException {
    if (Util.nullOrEmpty(keys)) {
      return new ArrayList<>(0);
    }

    synchronized (connection) {
      try {
        final List<Entity> result = new ArrayList<>();
        for (final Map.Entry<String, List<Entity.Key>> entry : Entities.mapKeysToEntityId(keys).entrySet()) {
          result.addAll(doSelectMany(entityConditions.selectCondition(entry.getValue()), 0));
        }
        if (!isTransactionOpen()) {
          commitQuietly();
        }

        return result;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final String entityId, final String propertyId, final Object... values) throws DatabaseException {
    return selectMany(entityConditions.selectCondition(entityId, propertyId, Condition.Type.LIKE, values == null ? null : Arrays.asList(values)));
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final EntitySelectCondition condition) throws DatabaseException {
    synchronized (connection) {
      try {
        final List<Entity> result = doSelectMany(condition, 0);
        if (!isTransactionOpen() && !condition.isForUpdate()) {
          commitQuietly();
        }

        return result;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Object> selectValues(final String propertyId, final EntityCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition, CONDITION_PARAM_NAME);
    if (domain.getSelectQuery(condition.getEntityId()) != null) {
      throw new UnsupportedOperationException("selectValues is not implemented for entities with custom select queries");
    }
    final Property.ColumnProperty property = domain.getColumnProperty(condition.getEntityId(), propertyId);
    final String columnName = property.getColumnName();
    final EntityCondition entityCondition = entityConditions.condition(condition.getEntityId(),
            Conditions.conditionSet(Conjunction.AND, condition.getCondition(), EntityConditions.stringCondition(columnName + " is not null")));
    final String selectSQL = createSelectSQL(domain.getSelectTableName(condition.getEntityId()), "distinct " + columnName,
            WHERE + entityCondition.getWhereClause(), columnName);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Databases.QUERY_COUNTER.count(selectSQL);
    synchronized (connection) {
      try {
        statement = prepareStatement(selectSQL);
        resultSet = executePreparedSelect(statement, selectSQL, condition);
        final List<Object> result = property.getResultPacker().pack(resultSet, -1);
        commitIfTransactionIsNotOpen();

        return result;
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(Databases.createLogMessage(getUser(), selectSQL, Arrays.asList(propertyId, condition), e, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        Databases.closeSilently(resultSet);
        Databases.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public int selectRowCount(final EntityCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition, CONDITION_PARAM_NAME);
    final String query = getSelectSQL(condition, connection.getDatabase());
    final String selectSQL = createSelectSQL("(" + query + ") alias", "count(*)", null, null);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Databases.QUERY_COUNTER.count(selectSQL);
    synchronized (connection) {
      try {
        statement = prepareStatement(selectSQL);
        resultSet = executePreparedSelect(statement, selectSQL, condition);
        final List<Integer> result = Databases.INTEGER_RESULT_PACKER.pack(resultSet, -1);
        commitIfTransactionIsNotOpen();
        if (result.isEmpty()) {
          throw new SQLException("Row count query returned no value");
        }

        return result.get(0);
      }
      catch (final SQLException e) {
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(Databases.createLogMessage(getUser(), selectSQL, condition.getValues(), e, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        Databases.closeSilently(resultSet);
        Databases.closeSilently(statement);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException {
    final Map<String, Collection<Entity>> dependencyMap = new HashMap<>();
    if (Util.nullOrEmpty(entities)) {
      return dependencyMap;
    }

    final Collection<Property.ForeignKeyProperty> foreignKeyReferences = domain.getForeignKeyReferences(
            entities.iterator().next().getEntityId());
    for (final Property.ForeignKeyProperty foreignKeyReference : foreignKeyReferences) {
      if (!foreignKeyReference.isSoftReference()) {
        final List<Entity> dependencies = selectMany(entityConditions.selectCondition(foreignKeyReference.getEntityId(),
                foreignKeyReference.getPropertyId(), Condition.Type.LIKE, entities));
        if (!dependencies.isEmpty()) {
          dependencyMap.put(foreignKeyReference.getEntityId(), dependencies);
        }
      }
    }

    return dependencyMap;
  }

  /** {@inheritDoc} */
  @Override
  public List executeFunction(final String functionId, final Object... arguments) throws DatabaseException {
    DatabaseException exception = null;
    try {
      logAccess("executeFunction: " + functionId, arguments);
      synchronized (connection) {
        return domain.getFunction(functionId).execute(this, arguments);
      }
    }
    catch (final DatabaseException e) {
      exception = e;
      LOG.error(Databases.createLogMessage(getUser(), functionId, arguments == null ? null : Arrays.asList(arguments), e, null));
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executeFunction: " + functionId, exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(Databases.createLogMessage(getUser(), "", arguments == null ? null : Arrays.asList(arguments), exception, entry));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void executeProcedure(final String procedureId, final Object... arguments) throws DatabaseException {
    DatabaseException exception = null;
    try {
      logAccess("executeProcedure: " + procedureId, arguments);
      synchronized (connection) {
        domain.getProcedure(procedureId).execute(this, arguments);
      }
    }
    catch (final DatabaseException e) {
      exception = e;
      LOG.error(Databases.createLogMessage(getUser(), procedureId, arguments == null ? null : Arrays.asList(arguments), e, null));
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executeProcedure: " + procedureId, exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(Databases.createLogMessage(getUser(), "", arguments == null ? null : Arrays.asList(arguments), exception, entry));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws ReportException {
    ReportException exception = null;
    synchronized (connection) {
      try {
        logAccess("fillReport", new Object[] {reportWrapper.getReportName()});
        final ReportResult result = reportWrapper.fillReport(connection.getConnection());
        if (!isTransactionOpen()) {
          commitQuietly();
        }

        return result;
      }
      catch (final ReportException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(Databases.createLogMessage(getUser(), null, Collections.singletonList(reportWrapper.getReportName()), e, null));
        throw e;
      }
      finally {
        final MethodLogger.Entry logEntry = logExit("fillReport", exception, null);
        if (LOG.isDebugEnabled()) {
          LOG.debug(Databases.createLogMessage(getUser(), null, Collections.singletonList(reportWrapper.getReportName()), exception, logEntry));
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyId, final byte[] blobData) throws DatabaseException {
    final Property.ColumnProperty property = domain.getColumnProperty(primaryKey.getEntityId(), blobPropertyId);
    if (property.getColumnType() != Types.BLOB) {
      throw new IllegalArgumentException("Property " + property.getPropertyId() + " in entity " +
              primaryKey.getEntityId() + " does not have column type BLOB");
    }
    final EntityCondition condition = entityConditions.condition(primaryKey);
    final String sql = "update " + domain.getTableName(primaryKey.getEntityId()) + " set " + property.getColumnName() +
            " = ?" + WHERE_SPACE_PREFIX + condition.getWhereClause();
    final List<Object> values = new ArrayList<>();
    final List<Property.ColumnProperty> properties = new ArrayList<>();
    Databases.QUERY_COUNTER.count(sql);
    synchronized (connection) {
      SQLException exception = null;
      PreparedStatement statement = null;
      try {
        logAccess("writeBlob", new Object[] {sql});
        values.add(null);//the blob value, set explicitly later
        values.addAll(condition.getValues());
        properties.add(property);
        properties.addAll(condition.getColumns());

        statement = prepareStatement(sql);
        setParameterValues(statement, values, properties);
        statement.setBinaryStream(1, new ByteArrayInputStream(blobData));//no need to close ByteArrayInputStream
        statement.executeUpdate();
        commitIfTransactionIsNotOpen();
      }
      catch (final SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(Databases.createLogMessage(getUser(), sql, values, exception, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        Databases.closeSilently(statement);
        final MethodLogger.Entry logEntry = logExit("writeBlob", exception, null);
        if (LOG.isDebugEnabled()) {
          LOG.debug(Databases.createLogMessage(getUser(), sql, values, exception, logEntry));
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyId) throws DatabaseException {
    final Property.ColumnProperty property = domain.getColumnProperty(primaryKey.getEntityId(), blobPropertyId);
    if (property.getColumnType() != Types.BLOB) {
      throw new IllegalArgumentException("Property " + property.getPropertyId() + " in entity " +
              primaryKey.getEntityId() + " does not have column type BLOB");
    }
    PreparedStatement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    final EntityCondition condition = entityConditions.condition(primaryKey);
    final String sql = "select " + property.getColumnName() + " from " +
            domain.getTableName(primaryKey.getEntityId()) + WHERE_SPACE_PREFIX + condition.getWhereClause();
    Databases.QUERY_COUNTER.count(sql);
    synchronized (connection) {
      try {
        logAccess("readBlob", new Object[] {sql});
        statement = prepareStatement(sql);
        setParameterValues(statement, condition.getValues(), condition.getColumns());

        resultSet = statement.executeQuery();
        final List<Blob> result = BLOB_RESULT_PACKER.pack(resultSet, 1);
        final Blob blob = result.get(0);
        final byte[] byteResult = blob.getBytes(1, (int) blob.length());
        commitIfTransactionIsNotOpen();

        return byteResult;
      }
      catch (final SQLException e) {
        exception = e;
        rollbackQuietlyIfTransactionIsNotOpen();
        LOG.error(Databases.createLogMessage(getUser(), sql, condition.getValues(), exception, null));
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
      finally {
        Databases.closeSilently(statement);
        Databases.closeSilently(resultSet);
        final MethodLogger.Entry logEntry = logExit("readBlob", exception, null);
        if (LOG.isDebugEnabled()) {
          LOG.debug(Databases.createLogMessage(getUser(), sql, condition.getValues(), exception, logEntry));
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseConnection getDatabaseConnection() {
    return connection;
  }

  /** {@inheritDoc} */
  @Override
  public ResultIterator<Entity> iterator(final EntityCondition condition) throws DatabaseException {
    synchronized (connection) {
      try {
        return createIterator(condition);
      }
      catch (final SQLException e) {
        throw new DatabaseException(e, connection.getDatabase().getErrorMessage(e));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isOptimisticLocking() {
    return optimisticLocking;
  }

  /** {@inheritDoc} */
  @Override
  public void setOptimisticLocking(final boolean optimisticLocking) {
    this.optimisticLocking = optimisticLocking;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLimitForeignKeyFetchDepth() {
    return limitForeignKeyFetchDepth;
  }

  /** {@inheritDoc} */
  @Override
  public void setLimitForeignKeyFetchDepth(final boolean limitForeignKeyFetchDepth) {
    this.limitForeignKeyFetchDepth = limitForeignKeyFetchDepth;
  }

  /**
   * Selects the given entities for update and checks if they have been modified by comparing
   * the property values to the current values in the database. Note that this does not
   * include BLOB properties or properties that are readOnly.
   * The calling method is responsible for releasing the select for update lock.
   * @param entities the entities to check, mapped to entityId
   * @throws SQLException in case of exception
   * @throws RecordModifiedException in case an entity has been modified, if an entity has been deleted,
   * the {@code modifiedRow} provided by the exception is null
   */
  private void lockAndCheckForUpdate(final Map<String, List<Entity>> entities) throws SQLException, RecordModifiedException {
    for (final Map.Entry<String, List<Entity>> entry : entities.entrySet()) {
      final List<Entity.Key> originalKeys = Entities.getKeys(entry.getValue(), true);
      final EntitySelectCondition selectForUpdateCondition = entityConditions.selectCondition(originalKeys);
      selectForUpdateCondition.setForUpdate(true);
      final List<Entity> currentValues = doSelectMany(selectForUpdateCondition, 0);
      final Map<Entity.Key, Entity> mappedEntities = Entities.mapToKey(currentValues);
      for (final Entity entity : entry.getValue()) {
        final Entity current = mappedEntities.get(entity.getOriginalKey());
        if (current == null) {
          throw new RecordModifiedException(entity, null, MESSAGES.getString(RECORD_MODIFIED_EXCEPTION)
                  + ", " + entity.getOriginalCopy() + " " + MESSAGES.getString("has_been_deleted"));
        }
        final Collection<Property.ColumnProperty> modified = Entities.getModifiedColumnProperties(entity, current, false);
        if (!modified.isEmpty()) {
          throw new RecordModifiedException(entity, current, createModifiedExceptionMessage(entity, current, modified));
        }
      }
    }
  }

  private List<Entity> doSelectMany(final EntitySelectCondition condition, final int currentForeignKeyFetchDepth) throws SQLException {
    Objects.requireNonNull(condition, CONDITION_PARAM_NAME);
    final List<Entity> result;
    try (final ResultIterator<Entity> iterator = createIterator(condition)) {
      result = packResult(iterator);
    }
    if (!condition.isForUpdate()) {
      setForeignKeys(result, condition, currentForeignKeyFetchDepth);
    }

    return result;
  }

  /**
   * Selects the entities referenced by the given entities via foreign keys and sets those
   * as their respective foreign key values. This is done recursively for the entities referenced
   * by the foreign keys as well, until the condition fetch depth limit is reached.
   * @param entities the entities for which to set the foreign key entity values
   * @param condition the condition
   * @param currentForeignKeyFetchDepth the current foreign key fetch depth
   * @throws SQLException in case of a database exception
   * @see #setLimitForeignKeyFetchDepth(boolean)
   * @see EntitySelectCondition#setForeignKeyFetchDepthLimit(int)
   */
  private void setForeignKeys(final List<Entity> entities, final EntitySelectCondition condition,
                              final int currentForeignKeyFetchDepth) throws SQLException {
    if (Util.nullOrEmpty(entities)) {
      return;
    }
    final List<Property.ForeignKeyProperty> foreignKeyProperties = domain.getForeignKeyProperties(entities.get(0).getEntityId());
    for (int i = 0; i < foreignKeyProperties.size(); i++) {
      final Property.ForeignKeyProperty foreignKeyProperty = foreignKeyProperties.get(i);
      final int conditionFetchDepthLimit = condition.getForeignKeyFetchDepthLimit(foreignKeyProperty.getPropertyId());
      if (!limitForeignKeyFetchDepth || conditionFetchDepthLimit == -1 || currentForeignKeyFetchDepth < conditionFetchDepthLimit) {
        try {
          logAccess("setForeignKeys", new Object[] {foreignKeyProperty});
          final List<Entity.Key> referencedKeys = getReferencedKeys(entities, foreignKeyProperty);
          if (referencedKeys.isEmpty()) {
            for (int j = 0; j < entities.size(); j++) {
              entities.get(j).put(foreignKeyProperty, null, false);
            }
          }
          else {
            final EntitySelectCondition referencedEntitiesCondition = entityConditions.selectCondition(referencedKeys);
            referencedEntitiesCondition.setForeignKeyFetchDepthLimit(conditionFetchDepthLimit);
            final List<Entity> referencedEntities = doSelectMany(referencedEntitiesCondition,
                    currentForeignKeyFetchDepth + 1);
            final Map<Entity.Key, Entity> mappedReferencedEntities = Entities.mapToKey(referencedEntities);
            for (int j = 0; j < entities.size(); j++) {
              final Entity entity = entities.get(j);
              final Entity.Key referencedKey = entity.getReferencedKey(foreignKeyProperty);
              entity.put(foreignKeyProperty, getReferencedEntity(referencedKey, mappedReferencedEntities), false);
            }
          }
        }
        finally {
          logExit("setForeignKeys", null, null);
        }
      }
    }
  }

  private Entity getReferencedEntity(final Entity.Key referencedPrimaryKey, final Map<Entity.Key, Entity> mappedReferencedEntities) {
    if (referencedPrimaryKey == null) {
      return null;
    }
    Entity referencedEntity = mappedReferencedEntities.get(referencedPrimaryKey);
    if (referencedEntity == null) {
      //if the referenced entity is not found (it's been deleted or has been filtered out of an underlying view for example),
      //we create an empty entity wrapping the primary key since that's the best we can do under the circumstances
      referencedEntity = domain.entity(referencedPrimaryKey);
    }

    return referencedEntity;
  }

  private ResultIterator<Entity> createIterator(final EntityCondition condition) throws SQLException {
    Objects.requireNonNull(condition, CONDITION_PARAM_NAME);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String selectSQL = null;
    try {
      selectSQL = getSelectSQL(condition, connection.getDatabase());
      statement = prepareStatement(selectSQL);
      resultSet = executePreparedSelect(statement, selectSQL, condition);

      return new EntityResultIterator(statement, resultSet,
              resultPackers.computeIfAbsent(condition.getEntityId(), entityId -> new EntityResultPacker(domain, entityId)));
    }
    catch (final SQLException e) {
      Databases.closeSilently(resultSet);
      Databases.closeSilently(statement);
      LOG.error(Databases.createLogMessage(getUser(), selectSQL, condition.getValues(), e, null));
      throw e;
    }
  }

  private int executePreparedUpdate(final PreparedStatement statement, final String sqlStatement,
                                    final List<Property.ColumnProperty> properties, final List values) throws SQLException {
    SQLException exception = null;
    Databases.QUERY_COUNTER.count(sqlStatement);
    try {
      logAccess("executePreparedUpdate", new Object[] {sqlStatement, values});
      setParameterValues(statement, values, properties);
      return statement.executeUpdate();
    }
    catch (final SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executePreparedUpdate", exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(Databases.createLogMessage(getUser(), sqlStatement, values, exception, entry));
      }
    }
  }

  private ResultSet executePreparedSelect(final PreparedStatement statement, final String sqlStatement,
                                          final EntityCondition condition) throws SQLException {
    SQLException exception = null;
    Databases.QUERY_COUNTER.count(sqlStatement);
    final List values = condition.getValues();
    try {
      logAccess("executePreparedSelect", values == null ? new Object[] {sqlStatement} : new Object[] {sqlStatement, values});
      setParameterValues(statement, values, condition.getColumns());
      return statement.executeQuery();
    }
    catch (final SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      final MethodLogger.Entry entry = logExit("executePreparedSelect", exception, null);
      if (LOG.isDebugEnabled()) {
        LOG.debug(Databases.createLogMessage(getUser(), sqlStatement, values, exception, entry));
      }
    }
  }

  private PreparedStatement prepareStatement(final String sqlStatement) throws SQLException {
    try {
      logAccess("prepareStatement", new Object[] {sqlStatement});
      return connection.getConnection().prepareStatement(sqlStatement);
    }
    finally {
      logExit("prepareStatement", null, null);
    }
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

  private DatabaseException translateInsertUpdateSQLException(final SQLException exception) {
    final Database database = connection.getDatabase();
    if (database.isUniqueConstraintException(exception)) {
      return new UniqueConstraintException(exception, database.getErrorMessage(exception));
    }
    else if (database.isReferentialIntegrityException(exception)) {
      return new ReferentialIntegrityException(exception, database.getErrorMessage(exception));
    }
    else {
      return new DatabaseException(exception, database.getErrorMessage(exception));
    }
  }

  private DatabaseException translateDeleteSQLException(final SQLException exception) {
    final Database database = connection.getDatabase();
    if (database.isReferentialIntegrityException(exception)) {
      return new ReferentialIntegrityException(exception, database.getErrorMessage(exception));
    }
    else {
      return new DatabaseException(exception, database.getErrorMessage(exception));
    }
  }

  private void commitQuietly() {
    try {
      connection.commit();
    }
    catch (final SQLException e) {
      LOG.error("Exception while performing a quiet commit", e);
    }
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

  private MethodLogger.Entry logExit(final String method, final Throwable exception, final String exitMessage) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      return methodLogger.logExit(method, exception, exitMessage);
    }

    return null;
  }

  private void logAccess(final String method, final Object[] arguments) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method, arguments);
    }
  }

  private static void setParameterValues(final PreparedStatement statement, final List values,
                                         final List<Property.ColumnProperty> parameterProperties) throws SQLException {
    if (Util.nullOrEmpty(values) || statement.getParameterMetaData().getParameterCount() == 0) {
      return;
    }
    if (parameterProperties == null || parameterProperties.size() != values.size()) {
      throw new SQLException("Parameter property value count mismatch: " + (parameterProperties == null ?
              "no properties" : ("expected: " + values.size() + ", got: " + parameterProperties.size())));
    }

    for (int i = 0; i < parameterProperties.size(); i++) {
      setParameterValue(statement, i + 1, values.get(i), parameterProperties.get(i));
    }
  }

  private static void setParameterValue(final PreparedStatement statement, final int parameterIndex,
                                        final Object value, final Property.ColumnProperty property) throws SQLException {
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

  private static List<Entity.Key> getReferencedKeys(final List<Entity> entities,
                                                    final Property.ForeignKeyProperty foreignKeyProperty) {
    final Set<Entity.Key> keySet = new HashSet<>(entities.size());
    for (int i = 0; i < entities.size(); i++) {
      final Entity.Key key = entities.get(i).getReferencedKey(foreignKeyProperty);
      if (key != null) {
        keySet.add(key);
      }
    }

    return new ArrayList<>(keySet);
  }

  private String getSelectSQL(final EntityCondition condition, final Database database) {
    final String entityId = condition.getEntityId();
    boolean containsWhereClause = false;
    String selectSQL = domain.getSelectQuery(entityId);
    if (selectSQL == null) {
      selectSQL = createSelectSQL(domain.getSelectTableName(entityId), domain.getSelectColumnsString(entityId), null, null);
    }
    else {
      containsWhereClause = domain.selectQueryContainsWhereClause(entityId);
    }

    final StringBuilder queryBuilder = new StringBuilder(selectSQL);
    final String whereClause = condition.getWhereClause();
    if (whereClause.length() > 0) {
      queryBuilder.append(containsWhereClause ? " and " : WHERE_SPACE_PREFIX).append(whereClause);
    }
    if (condition instanceof EntitySelectCondition && ((EntitySelectCondition) condition).isForUpdate()) {
      addForUpdate(database, queryBuilder);
    }
    else {
      addGroupHavingOrderByAndLimitClauses(condition, queryBuilder);
    }

    return queryBuilder.toString();
  }

  private static void addForUpdate(final Database database, final StringBuilder queryBuilder) {
    if (database.supportsSelectForUpdate()) {
      queryBuilder.append(" for update");
      if (database.supportsNowait()) {
        queryBuilder.append(" nowait");
      }
    }
  }

  private void addGroupHavingOrderByAndLimitClauses(final EntityCondition condition, final StringBuilder queryBuilder) {
    final String entityId = condition.getEntityId();
    final String groupByClause = domain.getGroupByClause(entityId);
    if (groupByClause != null) {
      queryBuilder.append(" group by ").append(groupByClause);
    }
    final String havingClause = domain.getHavingClause(entityId);
    if (havingClause != null) {
      queryBuilder.append(" having ").append(havingClause);
    }
    if (condition instanceof EntitySelectCondition) {
      final EntitySelectCondition selectCondition = (EntitySelectCondition) condition;
      final String orderByClause = getOrderByClause(selectCondition);
      if (orderByClause != null) {
        queryBuilder.append(" order by ").append(orderByClause);
      }
      if (selectCondition.getLimit() > 0) {
        queryBuilder.append(" limit ").append(selectCondition.getLimit());
        if (selectCondition.getOffset() > 0) {
          queryBuilder.append(" offset ").append(selectCondition.getOffset());
        }
      }
    }
  }

  private String getOrderByClause(final EntitySelectCondition selectCondition) {
    if (selectCondition.getOrderBy() == null) {
      return null;
    }

    final List<String> orderBys = new LinkedList<>();
    selectCondition.getOrderBy().getOrderByProperties().forEach(property ->
            orderBys.add(domain.getColumnProperty(selectCondition.getEntityId(), property.getPropertyId()).getColumnName()
                    + (property.isDescending() ? " desc" : "")));

    return String.join(", ", orderBys);
  }

  /**
   * @param tableName the table name
   * @param updateProperties the properties being updated
   * @param condition the primary key condition for the given entity
   * @return a query for updating this entity instance
   */
  private static String createUpdateSQL(final String tableName, final List<Property.ColumnProperty> updateProperties,
                                        final EntityCondition condition) {
    final StringBuilder sql = new StringBuilder("update ").append(tableName).append(" set ");
    for (int i = 0; i < updateProperties.size(); i++) {
      sql.append(updateProperties.get(i).getColumnName()).append(" = ?");
      if (i < updateProperties.size() - 1) {
        sql.append(", ");
      }
    }

    return sql.append(WHERE_SPACE_PREFIX).append(condition.getWhereClause()).toString();
  }

  /**
   * @param tableName the table name
   * @param insertProperties the properties used to insert the given entity type
   * @return a query for inserting this entity instance
   */
  private static String createInsertSQL(final String tableName, final List<Property.ColumnProperty> insertProperties) {
    final StringBuilder sql = new StringBuilder("insert ").append("into ").append(tableName).append("(");
    final StringBuilder columnValues = new StringBuilder(") values(");
    for (int i = 0; i < insertProperties.size(); i++) {
      sql.append(insertProperties.get(i).getColumnName());
      columnValues.append("?");
      if (i < insertProperties.size() - 1) {
        sql.append(", ");
        columnValues.append(", ");
      }
    }

    return sql.append(columnValues).append(")").toString();
  }

  /**
   * @param tableName the table name
   * @param condition the {@link EntityCondition} instance
   * @return a query for deleting the entities specified by the given condition
   */
  private static String createDeleteSQL(final String tableName, final EntityCondition condition) {
    final String whereClause = condition.getWhereClause();
    return "delete from " + tableName + (whereClause.isEmpty() ? "" : WHERE_SPACE_PREFIX + whereClause);
  }

  /**
   * Generates a sql select query with the given parameters
   * @param tableName the name of the table from which to select
   * @param columns the columns to select, example: "col1, col2"
   * @param whereCondition the where condition
   * @param orderByClause a string specifying the columns 'ORDER BY' clause,
   * "col1, col2" as input results in the following order by clause "order by col1, col2"
   * @return the generated sql query
   */
  private static String createSelectSQL(final String tableName, final String columns, final String whereCondition,
                                        final String orderByClause) {
    final StringBuilder sql = new StringBuilder("select ").append(columns).append(" from ").append(tableName);
    if (!Util.nullOrEmpty(whereCondition)) {
      sql.append(" ").append(whereCondition);
    }
    if (!Util.nullOrEmpty(orderByClause)) {
      sql.append(" order by ").append(orderByClause);
    }

    return sql.toString();
  }

  /**
   * @param inserting if true then all properties with values available in {@code entity} are added,
   * otherwise update is assumed and only properties with modified values are added.
   * @param entity the Entity instance
   * @param entityProperties the column properties the entity type is based on
   * @param statementProperties the list to populate with the properties to use in the statement
   * @param statementValues the list to populate with the values to be used in the statement
   * @throws java.sql.SQLException if no properties to populate the values for were found
   */
  private static void populateStatementPropertiesAndValues(final boolean inserting, final Entity entity,
                                                           final List<Property.ColumnProperty> entityProperties,
                                                           final List<Property.ColumnProperty> statementProperties,
                                                           final List<Object> statementValues) throws SQLException {
    for (int i = 0; i < entityProperties.size(); i++) {
      final Property.ColumnProperty property = entityProperties.get(i);
      if (entity.containsKey(property) && (inserting || entity.isModified(property))) {
        statementProperties.add(property);
        statementValues.add(entity.get(property));
      }
    }
    if (statementProperties.isEmpty()) {
      throw inserting ?
              new SQLException("Unable to insert entity " + entity.getEntityId() + ", no properties to insert") :
              new SQLException("Unable to update entity " + entity.getEntityId() + ", no modified values found");
    }
  }

  private static String createModifiedExceptionMessage(final Entity entity, final Entity modified,
                                                       final Collection<Property.ColumnProperty> modifiedProperties) {
    final StringBuilder builder = new StringBuilder(MESSAGES.getString(RECORD_MODIFIED_EXCEPTION)).append(", ").append(entity.getEntityId());
    for (final Property.ColumnProperty property : modifiedProperties) {
      builder.append(" \n").append(property).append(": ").append(entity.getOriginal(property)).append(" -> ").append(modified.get(property));
    }

    return builder.toString();
  }

  private void checkReadOnly(final List<Entity> entities) throws DatabaseException {
    for (int i = 0; i < entities.size(); i++) {
      checkReadOnly(entities.get(i).getEntityId());
    }
  }

  private void checkReadOnly(final String entityId) throws DatabaseException {
    if (domain.isReadOnly(entityId)) {
      throw new DatabaseException("Entities of type: " + entityId + " are read only");
    }
  }

  private static final class BlobPacker implements ResultPacker<Blob> {
    @Override
    public Blob fetch(final ResultSet resultSet) throws SQLException {
      return resultSet.getBlob(1);
    }
  }

  /**
   * A {@link MethodLogger.ArgumentStringProvider} implementation tailored for EntityConnections
   */
  static final class EntityArgumentStringProvider extends MethodLogger.DefaultArgumentStringProvider {

    private final Domain domain;

    EntityArgumentStringProvider(final Domain domain) {
      this.domain = domain;
    }

    @Override
    public String toString(final Object argument) {
      if (argument == null) {
        return "";
      }

      final StringBuilder builder = new StringBuilder();
      if (argument instanceof EntityCondition) {
        builder.append(getEntityConditionString((EntityCondition) argument));
      }
      else if (argument instanceof Object[] && ((Object[]) argument).length > 0) {
        builder.append("[").append(toString((Object[]) argument)).append("]");
      }
      else if (argument instanceof Collection && !((Collection) argument).isEmpty()) {
        builder.append("[").append(toString(((Collection) argument).toArray())).append("]");
      }
      else if (argument instanceof Entity) {
        builder.append(getEntityParameterString((Entity) argument));
      }
      else if (argument instanceof Entity.Key) {
        builder.append(getEntityKeyParameterString((Entity.Key) argument));
      }
      else {
        builder.append(argument.toString());
      }

      return builder.toString();
    }

    private String getEntityConditionString(final EntityCondition condition) {
      final StringBuilder builder = new StringBuilder(condition.getEntityId());
      final String whereClause = condition.getWhereClause();
      if (!Util.nullOrEmpty(whereClause)) {
        builder.append(",").append(WHERE_SPACE_PREFIX).append(whereClause);
      }
      final List values = condition.getValues();
      if (values != null) {
        builder.append(", ").append(toString(values));
      }

      return builder.toString();
    }

    private String getEntityParameterString(final Entity entity) {
      final StringBuilder builder = new StringBuilder(entity.getEntityId()).append(" {");
      final List<Property.ColumnProperty> columnProperties = domain.getColumnProperties(entity.getEntityId());
      for (int i = 0; i < columnProperties.size(); i++) {
        final Property.ColumnProperty property = columnProperties.get(i);
        final boolean modified = entity.isModified(property);
        if (property.isPrimaryKeyProperty() || modified) {
          final StringBuilder valueString = new StringBuilder();
          if (modified) {
            valueString.append(entity.getOriginal(property)).append("->");
          }
          valueString.append(entity.get(property.getPropertyId()));
          builder.append(property.getPropertyId()).append(":").append(valueString).append(",");
        }
      }
      builder.deleteCharAt(builder.length() - 1);

      return builder.append("}").toString();
    }

    private static String getEntityKeyParameterString(final Entity.Key argument) {
      return argument.getEntityId() + ", " + argument.toString();
    }
  }

  private static final class EntityResultIterator implements ResultIterator<Entity> {

    private final Statement statement;
    private final ResultSet resultSet;
    private final ResultPacker<Entity> resultPacker;

    private EntityResultIterator(final Statement statement, final ResultSet resultSet, final ResultPacker<Entity> resultPacker) {
      this.statement = statement;
      this.resultSet = resultSet;
      this.resultPacker = resultPacker;
    }

    @Override
    public boolean hasNext() throws SQLException {
      try {
        if (resultSet.next()) {
          return true;
        }
        close();

        return false;
      }
      catch (final SQLException e) {
        close();
        throw e;
      }
    }

    @Override
    public Entity next() throws SQLException {
      try {
        return resultPacker.fetch(resultSet);
      }
      catch (final SQLException e) {
        close();
        throw e;
      }
    }

    @Override
    public void close() {
      Databases.closeSilently(resultSet);
      Databases.closeSilently(statement);
    }
  }

  /**
   * Handles packing Entity query results.
   * Loads all database property values except for foreign key properties (Property.ForeignKeyProperty).
   */
  private static final class EntityResultPacker implements ResultPacker<Entity> {

    private final Domain domain;
    private final String entityId;
    private final List<Property.ColumnProperty> columnProperties;
    private final List<Property.TransientProperty> transientProperties;
    private final boolean hasTransientProperties;
    private final int propertyCount;

    /**
     * Instantiates a new EntityResultPacker.
     * @param entityId the id of the entities this packer packs
     */
    private EntityResultPacker(final Domain domain, final String entityId) {
      this.domain = domain;
      this.entityId = entityId;
      this.columnProperties = domain.getColumnProperties(entityId);
      this.transientProperties = domain.getTransientProperties(entityId);
      this.hasTransientProperties = !Util.nullOrEmpty(this.transientProperties);
      this.propertyCount = domain.getProperties(entityId).size();
    }

    @Override
    public Entity fetch(final ResultSet resultSet) throws SQLException {
      final Map<Property, Object> values = new HashMap<>(propertyCount);
      if (hasTransientProperties) {
        for (int i = 0; i < transientProperties.size(); i++) {
          final Property.TransientProperty transientProperty = transientProperties.get(i);
          if (!(transientProperty instanceof Property.DerivedProperty)) {
            values.put(transientProperty, null);
          }
        }
      }
      for (int i = 0; i < columnProperties.size(); i++) {
        final Property.ColumnProperty property = columnProperties.get(i);
        try {
          values.put(property, property.fetchValue(resultSet));
        }
        catch (final Exception e) {
          throw new SQLException("Exception fetching: " + property + ", entity: " + entityId + " [" + e.getMessage() + "]", e);
        }
      }

      return domain.entity(entityId, values, null);
    }
  }
}